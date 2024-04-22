package backend.utils;

import java.util.ArrayList;
import java.util.List;

import backend.protocol.AmazonUps.Product;
import backend.protocol.WorldAmazon.AProduct;

public class ProductToAProduct {
    public static AProduct genAProduct(Product p){
        AProduct.Builder aProductB = AProduct.newBuilder();
        aProductB.setId(p.getId());
        aProductB.setDescription(p.getDescription());
        aProductB.setCount(p.getCount());

        return aProductB.build();
    }

    public static List<AProduct> genAProductList(List<Product> products){
        List<AProduct> aProducts = new ArrayList<>();
        for(Product p : products){
            aProducts.add(genAProduct(p));
        }
        return aProducts;
    }
}
