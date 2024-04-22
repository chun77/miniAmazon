from django.contrib import admin


# Register your models here.
from .models import *
admin.site.register(Product)
admin.site.register(Order)
admin.site.register(WareHouse)
admin.site.register(AmazonUser)
admin.site.register(PackageProduct)
admin.site.register(PackageStatus)

