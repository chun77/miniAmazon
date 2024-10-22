from django.shortcuts import render, get_object_or_404, redirect
from .models import *
import json, socket
from hashids import Hashids
from django.contrib.auth import authenticate, login, logout
from django.contrib.auth.decorators import login_required
from django.contrib import messages
from .forms import *
from django.http import JsonResponse
from django.urls import reverse
from django.core.validators import validate_integer
from django.core.exceptions import ValidationError

def register(request):
	if request.user.is_authenticated:
		return redirect('catalog')
	else:
		form = UserRegisterForm()
		if request.method == 'POST':
			form = UserRegisterForm(request.POST)
			if form.is_valid():
				form.save()
				user = form.cleaned_data.get('username')
				messages.success(request, 'Account was created for ' + user)

				return redirect('login')
			

		context = {'form':form}
		return render(request, 'shop/register.html', context)


def login_view(request):
	if request.user.is_authenticated:
		return redirect('catalog')
	else:
		if request.method == 'POST':
			username = request.POST.get('username')
			password =request.POST.get('password')

			user = authenticate(request, username=username, password=password)

			if user is not None:
				login(request, user)
				return redirect('catalog')
			else:
				messages.info(request, 'Username OR password is incorrect')

		context = {}
		return render(request, 'shop/login.html', context)

def logout_view(request):
	logout(request)
	return redirect('login')


# Create your views here.
@login_required(login_url='login')
def main(request):
    context = {}
    return render(request, 'shop/main.html', context)

@login_required(login_url='login')
def catalog(request):
    search_query = request.GET.get('search', '')
    if search_query:
        products = Product.objects.filter(name__icontains=search_query)
    else:
        products = Product.objects.all()
    context = {'products': products}
    return render(request, 'shop/catalog.html', context)

@login_required(login_url='login')
def checkout(request):
    cart = request.session.get('cart', {})
    if not cart:
        messages.info(request, "Cannot proceed to checkout when cart is empty.")
        return redirect('cart')

    items = []
    total_price = 0
    for product_id_str, quantity in cart.items():
        product = get_object_or_404(Product, pk=int(product_id_str))
        total = product.price * quantity
        total_price += total
        items.append({
            'product': product,
            'quantity': quantity,
            'total': total,
        })
    
    context = {'items': items, 'total_price': total_price}
    return render(request, 'shop/checkout.html', context)

# checkout.html form submission passes the product_id to this view
@login_required(login_url='login')
def place_order(request):
    cart = request.session.get('cart', {})
    if request.method == 'POST':
        form = OrderForm(request.POST)  # Bind form with POST data
        if form.is_valid():
            # Form data is valid, proceed with order placement
            order = Order(
                user=request.user,
                dest_x=int(form.cleaned_data['address_x']),
                dest_y=int(form.cleaned_data['address_y']),
                ups_account=form.cleaned_data['ups_account']
            )
            order.save()

            # Generate tracking number
            hashids = Hashids(salt="zw297hg161", min_length=8)
            tracking_number = hashids.encode(order.package_id)
            order.tracking_id = tracking_number
            order.save()

            # Create PackageProduct for each item in the cart
            for product_id_str, quantity in cart.items():
                product = Product.objects.get(pk=int(product_id_str))
                PackageProduct.objects.create(
                    product=product,
                    package=order,
                    quantity=quantity
                )

            # Save package status
            package_status = PackageStatus(
                package=order,
                status="OrderPlaced"
            )
            package_status.save()

            # Send package ID to external system
            package_id_str = str(order.package_id)
            response = None
            retry_count = 3

            # Retry sending package ID in case of failure
            for attempt in range(retry_count):
                try:
                    with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as s:
                        s.connect(('backend', 8888))
                        s.settimeout(5.0)
                        s.sendall(package_id_str.encode('utf-8'))
                        response = s.recv(1024).decode('utf-8')
                        if response:
                            print("Received response:", response)
                            break
                        else:
                            print("No response, retrying...")
                except socket.timeout:
                    print("Socket timed out, retrying...")
                except Exception as e:
                    print("An error occurred:", e)

            if response is None:
                print("Failed to receive a response after {} attempts".format(retry_count))

            # Clear the cart and redirect to success page
            request.session['cart'] = {}
            return redirect('success', order_id=order.package_id)
        else:
            # Form data is invalid, render the checkout page with form and errors
            # messages.error(request, "Failed to place order. Please correct the errors below.")
            
            # Retrieve the items from the cart to be passed to the template for rendering order summary
            items = []
            for product_id_str, quantity in cart.items():
                product = Product.objects.get(pk=int(product_id_str))
                items.append({
                    'product': product,
                    'quantity': quantity
                })

            return render(request, 'shop/checkout.html', {
                'form': form,  # Pass the form back to the template with errors
                'items': items,  # Pass the cart items to maintain the order summary
            })
    else:
        form = OrderForm()  # If it's a GET request, create a new form
        items = []

        # Retrieve the items from the cart to be passed to the template for rendering order summary
        for product_id_str, quantity in cart.items():
            product = Product.objects.get(pk=int(product_id_str))
            items.append({
                'product': product,
                'quantity': quantity
            })

        return render(request, 'shop/checkout.html', {
            'form': form,
            'items': items
        })

@login_required(login_url='login')
def success(request, order_id):
    order = get_object_or_404(Order, package_id=order_id)
    context = {'order': order}
    return render(request, 'shop/successful.html', context)

@login_required(login_url='login')
def check_order_status(request):
    orders = Order.objects.filter(user=request.user)
    
    order_ids = orders.values_list('package_id', flat=True)
    
    package_statuses = PackageStatus.objects.filter(package__package_id__in=order_ids)
    
    context = {'packageStatuses': package_statuses}
    return render(request, 'shop/checkorderstatus.html', context)

@login_required(login_url='login')
def view_order_detail(request, order_id):
    order = get_object_or_404(Order, package_id=order_id)
    package_products = PackageProduct.objects.filter(package=order)
    context = {
        'order': order,
        'package_products': package_products
    }
    return render(request, 'shop/vieworderdetail.html', context)

@login_required(login_url='login')
def cart(request):
    cart = request.session.get('cart', {})
    items = []
    for product_id, quantity in cart.items():
        product = get_object_or_404(Product, pk=int(product_id))
        items.append({
            'product_id': product.product_id,
            'name': product.name,
            'price': product.price,
            'quantity': quantity,
            'imageURL': product.imageURL
        })
    context = {'items': items}
    return render(request, 'shop/cart.html', context)
    
@login_required(login_url='login')
def add_to_cart(request, product_id):
    if request.method == 'POST':
        product = get_object_or_404(Product, pk=int(product_id))
        quantity = int(request.POST.get('quantity', 1))
        cart = request.session.get('cart', {})

        # Treat product_id as a string since session keys are always strings
        product_id_str = str(product_id)
        cart[product_id_str] = cart.get(product_id_str, 0) + quantity
    
        request.session['cart'] = cart
    
        # if it is an AJAX request 
        if request.headers.get('X-Requested-With') == 'XMLHttpRequest':
            return JsonResponse({'message': f'{quantity} x "{product.name}" added to cart.'})
        else:
            messages.success(request, f'{quantity} x "{product.name}" added to cart.')
            return redirect('catalog')

@login_required(login_url='login')
def update_cart(request):
    if request.method == 'POST':
        # Loop over the items in the POST request
        cart = request.session.get('cart', {})
        for key in request.POST:
            if key.startswith('quantity_'):
                product_id_str = key.split('_')[1]
                quantity = int(request.POST[key])

                if product_id_str in cart:
                    if quantity <= 0:
                        # Remove the product if the quantity is less than or equal to zero
                        del cart[product_id_str]
                        messages.success(request, "Product removed from your cart.")
                    else:
                        # Update the quantity for the product
                        cart[product_id_str] = quantity
                        messages.success(request, "Cart updated successfully.")
                else:
                    messages.error(request, "Product not found in cart.")
        request.session['cart'] = cart
        return redirect('cart')
    else:
        return redirect('catalog')



@login_required(login_url='login')
def remove_from_cart(request, product_id):
    cart = request.session.get('cart', {})

    # Ensure product_id is a string
    product_id_str = str(product_id)

    if product_id_str in cart:
        # Remove the product from the cart
        del cart[product_id_str]
        request.session['cart'] = cart
        messages.success(request, "Product removed from your cart.")

    return redirect('cart')



