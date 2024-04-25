from django.urls import path

from . import views

urlpatterns = [
	#Leave as empty string for base url
	path('register/', views.register, name="register"),
	path('login/', views.login_view, name="login"),
	path('logout/', views.logout_view, name="logout"),
	path('', views.catalog, name="catalog"),
	path('checkout/', views.checkout, name="checkout"),
    path('place_order/', views.place_order, name="place_order"),
    path('success/<int:order_id>', views.success, name="success"),
	path('checkorderstatus/', views.check_order_status, name="checkorderstatus"),
	path('vieworderdetail/<int:order_id>', views.view_order_detail, name="vieworderdetail"),
	path('add_to_cart/<int:product_id>', views.add_to_cart, name="add_to_cart"),
	path('cart/', views.cart, name = "cart"),
	path('update_cart/', views.update_cart, name='update_cart'),
	path('remove_from_cart/<int:product_id>/', views.remove_from_cart, name="remove_from_cart")
]