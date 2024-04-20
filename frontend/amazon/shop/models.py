from django.db import models

# Create your models here.
from django.contrib.auth.models import User
class AmazonUser(models.Model):
    user = models.OneToOneField(User, on_delete=models.CASCADE)
    ups_account = models.CharField(max_length=255)

    def __str__(self):
        return self.user.username

# initialize Product Warehouse
class Product(models.Model):
    name = models.CharField(max_length=255)
    description = models.TextField()
    # price = models.DecimalField(max_digits=10, decimal_places=2)

    def __str__(self):
        return self.name
    

    
class Order(models.Model):
    amazon_account = models.ForeignKey(AmazonUser, on_delete=models.CASCADE, null=True)
    tracking_id = models.CharField(max_length=255, default='')
    product = models.ForeignKey(Product, on_delete=models.CASCADE)
    quantity = models.IntegerField()
    # total_price = models.DecimalField(max_digits=10, decimal_places=2)
    x = models.IntegerField()
    y = models.IntegerField()

    def __str__(self):
        return f'{self.product.name} - {self.quantity}'
    
class WareHouse(models.Model):
    x = models.IntegerField()
    y = models.IntegerField()

    def __str__(self):
        return f'{self.x} - {self.y}'
    
class Stock(models.Model):
    product = models.ForeignKey(Product, on_delete=models.CASCADE)
    warehouse = models.ForeignKey(WareHouse, on_delete=models.CASCADE)
    quantity = models.IntegerField()
    
    def __str__(self):
        return f'{self.product.name} - {self.warehouse.x} - {self.warehouse.y} - {self.quantity}'
