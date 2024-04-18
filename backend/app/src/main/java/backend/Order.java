package backend;

public class Order {
    private String AMZaccount;
    private String UPSaccount;
    private String trackingID;
    private String packageID;
    private String productID;
    private int amt;
    private Location shipAddr;
    
    public Order(String AMZaccount, String trackingID, String packageID, String productID, int amt, Location shipAddr) {
        this(AMZaccount, null, trackingID, packageID, productID, amt, shipAddr);
    }

    public Order(String AMZaccount, String UPSaccount, String trackingID, String packageID, String productID, int amt, Location shipAddr) {
        this.AMZaccount = AMZaccount;
        this.UPSaccount = UPSaccount;
        this.trackingID = trackingID;
        this.packageID = packageID;
        this.productID = productID;
        this.amt = amt;
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

    public String getProductID() {
        return productID;
    }

    public int getAmt() {
        return amt;
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

    public void setProductID(String productID) {
        this.productID = productID;
    }

    public void setAmt(int amt) {
        this.amt = amt;
    }

    public void setShipAddr(Location shipAddr) {
        this.shipAddr = shipAddr;
    }
}
