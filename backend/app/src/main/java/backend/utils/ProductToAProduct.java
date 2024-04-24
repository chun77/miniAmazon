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

    public static boolean hasSameProducts(List<AProduct> aProducts, List<Product> products){
        if(aProducts.size() != products.size()){
            return false;
        }
        for(int i = 0; i < aProducts.size(); i++){
            if(aProducts.get(i).getId() != products.get(i).getId() ||
                    !aProducts.get(i).getDescription().equals(products.get(i).getDescription()) ||
                    aProducts.get(i).getCount() != products.get(i).getCount()){
                return false;
            }
        }
        return true;
    }
}
