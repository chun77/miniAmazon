package backend;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class OrderTest {
    @Test
    public void testGettersAndSetters() {
        String AMZaccount = "AMZ123";
        String UPSaccount = "UPS456";
        String trackingID = "TRACK789";
        String packageID = "PKG123";
        String productID = "PROD456";
        int amt = 10;
        Location shipAddr = new Location(1, 2);

        Order order = new Order(AMZaccount, UPSaccount, trackingID, packageID, productID, amt, shipAddr);

        assertEquals(AMZaccount, order.getAMZaccount());
        assertEquals(UPSaccount, order.getUPSaccount());
        assertEquals(trackingID, order.getTrackingID());
        assertEquals(packageID, order.getPackageID());
        assertEquals(productID, order.getProductID());
        assertEquals(amt, order.getAmt());
        assertEquals(shipAddr, order.getShipAddr());

        String newAMZaccount = "NEWAMZ789";
        order.setAMZaccount(newAMZaccount);
        assertEquals(newAMZaccount, order.getAMZaccount());

        String newUPSaccount = "NEWUPS987";
        order.setUPSaccount(newUPSaccount);
        assertEquals(newUPSaccount, order.getUPSaccount());

        String newTrackingID = "NEWTRACK123";
        order.setTrackingID(newTrackingID);
        assertEquals(newTrackingID, order.getTrackingID());

        String newPackageID = "NEWPKG456";
        order.setPackageID(newPackageID);
        assertEquals(newPackageID, order.getPackageID());

        String newProductID = "NEWPROD789";
        order.setProductID(newProductID);
        assertEquals(newProductID, order.getProductID());

        int newAmt = 20;
        order.setAmt(newAmt);
        assertEquals(newAmt, order.getAmt());

        Location newShipAddr = new Location(3, 4);
        order.setShipAddr(newShipAddr);
        assertEquals(newShipAddr, order.getShipAddr());
    }
}
