from django import forms
from django.contrib.auth.models import User
from django.contrib.auth.forms import UserCreationForm

class UserRegisterForm(UserCreationForm):
    email = forms.EmailField()

    class Meta:
        model = User
        fields = ['username', 'email', 'password1', 'password2']

class OrderForm(forms.Form):
    address_x = forms.CharField(label='Address X', max_length=100)
    address_y = forms.CharField(label='Address Y', max_length=100)
    ups_account = forms.IntegerField(label='UPS Account', required=False)