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
    product_id = models.BigAutoField(primary_key = True)
    name = models.CharField(max_length=255)
    description = models.TextField()
    price = models.DecimalField(max_digits=10, decimal_places=2)

    def __str__(self):
        return self.name
    

    
class Order(models.Model):
    # generate tracking id
    package_id = models.BigAutoField(primary_key = True)
    amazon_account = models.ForeignKey(AmazonUser, on_delete=models.CASCADE, null=True)
    tracking_id = models.CharField(max_length=255, default='')
    dest_x = models.IntegerField()
    dest_y = models.IntegerField()
    
class WareHouse(models.Model):
    wh_id = models.AutoField(primary_key = True)
    wh_x = models.IntegerField()
    wh_y = models.IntegerField()

    def __str__(self):
        return f'{self.wh_x} - {self.wh_y}'
    
# class Stock(models.Model):
#     product = models.ForeignKey(Product, on_delete=models.CASCADE)
#     warehouse = models.ForeignKey(WareHouse, on_delete=models.CASCADE)
#     quantity = models.IntegerField()
    
#     def __str__(self):
#         return f'{self.product.name} - {self.warehouse.x} - {self.warehouse.y} - {self.quantity}'

class PackageProduct(models.Model):
    product = models.ForeignKey(Product, on_delete=models.CASCADE, null = True)
    package = models.ForeignKey(Order, on_delete=models.CASCADE, null = True)
    quantity = models.IntegerField()
    
class PackageStatus(models.Model):
    package = models.ForeignKey(Order, on_delete=models.CASCADE, null = True)
    wh = models.ForeignKey(WareHouse, on_delete=models.CASCADE, null = True)
    status = models.CharField(max_length=255, default='')
