package backend;

import java.util.List;

import backend.protocol.WorldAmazon.AProduct;

public class Package {
    private long packageID;
    private String trackingID;
    private int truckID;
    private Location dest;
    private List<AProduct> products;
    private WareHouse wh;
    private String status;

    public Package(long packageID, String trackingID, int truckID, Location dest, List<AProduct> products, WareHouse wh, String status) {
        this.packageID = packageID;
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

    public String getTrackingID() {
        return trackingID;
    }

    public int getTruckID() {
        return truckID;
    }

    public Location getDest() {
        return dest;
    }

    public List<AProduct> getProducts() {
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
}
