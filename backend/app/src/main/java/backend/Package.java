package backend;

import java.util.List;

import backend.protocol.AmazonUps.Product;

public class Package {
    private long packageID;
    private int amazonAccount;
    private int upsAccount;
    private String email;
    private String trackingID;
    private int truckID;
    private Location dest;
    private List<Product> products;
    private WareHouse wh;
    private String status;

    public Package(long packageID, int amazonAccount, String email, String trackingID, int truckID, Location dest, List<Product> products, WareHouse wh, String status) {
        this.packageID = packageID;
        this.amazonAccount = amazonAccount;
        this.email = email;
        this.trackingID = trackingID;
        this.truckID = truckID;
        this.dest = dest;
        this.products = products;
        this.wh = wh;
        this.status = status;
    }

    public long getPackageID() {
        return packageID;
    }

    public int getAmazonAccount() {
        return amazonAccount;
    }

    public int getUpsAccount() {
        return upsAccount;
    }

    public String getEmail() {
        return email;
    }

    public String getTrackingID() {
        return trackingID;
    }

    public int getTruckID() {
        return truckID;
    }

    public Location getDest() {
        return dest;
    }

    public List<Product> getProducts() {
        return products;
    }

    public WareHouse getWh() {
        return wh;
    }

    public String getStatus() {
        return status;
    }

    public void setTruckID(int truckID) {
        this.truckID = truckID;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setUpsAccount(int upsAccount) {
        this.upsAccount = upsAccount;
    }

    public void setDest(Location dest) {
        this.dest = dest;
    }
}
