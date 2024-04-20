package backend;

import java.util.List;
import java.util.Map;

public class Order {
    private String AMZaccount;
    private String UPSaccount;
    private String trackingID;
    private String packageID;
    private List<Map<String, Integer>> products;
    private Location shipAddr;
    
    public Order(String AMZaccount, String trackingID, String packageID, List<Map<String, Integer>> products, Location shipAddr) {
        this(AMZaccount, null, trackingID, packageID, products, shipAddr);
    }

    public Order(String AMZaccount, String UPSaccount, String trackingID, String packageID, List<Map<String, Integer>> products, Location shipAddr) {
        this.AMZaccount = AMZaccount;
        this.UPSaccount = UPSaccount;
        this.trackingID = trackingID;
        this.packageID = packageID;
        this.products = products;
        this.shipAddr = shipAddr;
    }

    public String getAMZaccount() {
        return AMZaccount;
    }

    public String getUPSaccount() {
        return UPSaccount;
    }

    public String getTrackingID() {
        return trackingID;
    }

    public String getPackageID() {
        return packageID;
    }

    public List<Map<String, Integer>> getProducts() {
        return products;
    }

    public Location getShipAddr() {
        return shipAddr;
    }

    public void setAMZaccount(String AMZaccount) {
        this.AMZaccount = AMZaccount;
    }

    public void setUPSaccount(String UPSaccount) {
        this.UPSaccount = UPSaccount;
    }

    public void setTrackingID(String trackingID) {
        this.trackingID = trackingID;
    }

    public void setPackageID(String packageID) {
        this.packageID = packageID;
    }

    public void setProducts(List<Map<String, Integer>> products) {
        this.products = products;
    }

    public void setShipAddr(Location shipAddr) {
        this.shipAddr = shipAddr;
    }
}
