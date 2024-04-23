from django.shortcuts import render, get_object_or_404, redirect
from .models import *
import json, socket
from hashids import Hashids
from django.contrib.auth import authenticate, login, logout
from django.contrib.auth.decorators import login_required
from django.contrib import messages
from .forms import*


def register(request):
	if request.user.is_authenticated:
		return redirect('catalog')
	else:
		form = CreateUserForm()
		if request.method == 'POST':
			form = CreateUserForm(request.POST)
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

# catalog.html onclick passes the product_id to this view
@login_required(login_url='login')
def checkout(request, product_id):
    product = get_object_or_404(Product, product_id=product_id)
    context = {'product': product}
    return render(request, 'shop/checkout.html', context)

# checkout.html form submission passes the product_id to this view
@login_required(login_url='login')
def place_order(request, product_id):
    product = get_object_or_404(Product, product_id=product_id)
    if request.method == 'POST':
        quantity = request.POST.get('quantity')
        address_x = request.POST.get('address_x')
        address_y = request.POST.get('address_y')
        ups_account = request.POST.get('ups_account')
        
        # Check for any missing fields
        if not (quantity and address_x and address_y):
            context = {'product': product, 'error': 'Please fill all fields correctly.'}
            return render(request, 'shop/checkout.html', context)

        order = Order(
            # tbd: amazonAccount
            user = request.user,
            dest_x=int(address_x),
            dest_y=int(address_y),
            ups_account=ups_account or None  # deal with empty value
        )
        order.save()

        # produce tracking_number according to package_id
        hashids = Hashids(salt = "zw297hg161", min_length = 8);
        tracking_number = hashids.encode(order.package_id)
        order.tracking_id = tracking_number
        order.save()

        package_product = PackageProduct(
            product = product,
            package = order,
            quantity = quantity
        )
        package_product.save()

        package_status = PackageStatus(
            package = order,
            status = "OrderPlaced"
        )
        package_status.save()

        # only send package_id
        package_id_str = str(order.package_id)

        # Send the order data to the Amazon server
        # with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as s:
        #     # connect to the server
        #     s.connect(('vcm-37900.vm.duke.edu', 8888))

        #     # send the order data
        #     s.sendall(package_id_str.encode('utf-8'))

        #     # receive the response from the server with new tracking id
        #     response = s.recv(1024).decode('utf-8')
        #     print(response)

        # Redirect to the success page
        return redirect('success', order_id=order.package_id)
    else:
        # Return to the checkout page if not a POST request
        return render(request, 'shop/checkout.html', {'product': product})

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

def view_order_detail(request, order_id):
    order = get_object_or_404(Order, package_id=order_id)
    package_products = PackageProduct.objects.filter(package=order)
    context = {
        'order': order,
        'package_products': package_products
    }
    return render(request, 'shop/vieworderdetail.html', context)




