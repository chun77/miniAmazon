package backend;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class ProductTest {
    @Test
    public void testGettersAndSetters() {
        long id = 123;
        String description = "Product description";

        Product product = new Product(id, description);

        assertEquals(id, product.getId());
        assertEquals(description, product.getDescription());

        long newId = 456;
        product.setId(newId);
        assertEquals(newId, product.getId());

        String newDescription = "New product description";
        product.setDescription(newDescription);
        assertEquals(newDescription, product.getDescription());
    }
}
