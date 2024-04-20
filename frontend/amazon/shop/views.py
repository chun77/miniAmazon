from django.shortcuts import render, get_object_or_404, redirect
from .models import Product, Order
import json, socket

# Create your views here.
def main(request):
    context = {}
    return render(request, 'shop/main.html', context)

def catalog(request):
    products = Product.objects.all()
    context = {'products': products}
    return render(request, 'shop/catalog.html', context)

# catalog.html onclick passes the product_id to this view
def checkout(request, product_id):
    product = get_object_or_404(Product, id=product_id)
    context = {'product': product}
    return render(request, 'shop/checkout.html', context)

# checkout.html form submission passes the product_id to this view
def place_order(request, product_id):
    product = get_object_or_404(Product, id=product_id)
    if request.method == 'POST':
        quantity = request.POST.get('quantity')
        address_x = request.POST.get('address_x')
        address_y = request.POST.get('address_y')
        
        # Check for any missing fields
        if not (quantity and address_x and address_y):
            context = {'product': product, 'error': 'Please fill all fields correctly.'}
            return render(request, 'shop/checkout.html', context)

        # total_price = product.price * int(quantity)  # Calculate the total price
        order = Order(
            product=product,
            quantity=int(quantity),
            # total_price=total_price,
            x=int(address_x),
            y=int(address_y)
        )
        order.save()

        # transfer data to json
        order_json = {
            'amazon_account': 'wzc',
            'product': product.name,
            'quantity': quantity,
            'tracking_id': '1234567890',
            'package_id': order.id,
            'x': address_x,
            'y': address_y
        }

        order_data_json = json.dumps(order_json)

        # Send the order data to the Amazon server
        with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as s:
            # connect to the server
            s.connect(('localhost', 1234))

            # send the order data
            s.sendall(order_data_json.encode('utf-8'))

            # receive the response from the server with new tracking id
            response = s.recv(1024).decode('utf-8')
            response_data = json.loads(response)
            new_tracking_id = response_data.get('tracking_id')

        # Redirect to the success page
        return redirect('success', order_id=order.id, tracking_id=new_tracking_id)
    else:
        # Return to the checkout page if not a POST request
        return render(request, 'shop/checkout.html', {'product': product})

def success(request, order_id, tracking_id):
    order = get_object_or_404(Order, id=order_id)
    context = {'order': order, 'tracking_id': tracking_id}
    return render(request, 'shop/successful.html', context)



