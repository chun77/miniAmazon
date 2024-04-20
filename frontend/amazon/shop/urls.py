from django.urls import path

from . import views

urlpatterns = [
	#Leave as empty string for base url
	path('', views.catalog, name="catalog"),
	path('checkout/<int:product_id>/', views.checkout, name="checkout"),
    path('place_order/<int:product_id>/', views.place_order, name="place_order"),
    path('success/<int:order_id>/<str:tracking_id>', views.success, name="success"),
]