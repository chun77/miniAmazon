package backend;

import java.util.List;

public class Package {
    private String packageID;
    private String truckID;
    private Location dest;
    private List<Long> productIDs;
    private WareHouse wh;

    public Package(String packageID, String truckID, Location dest, List<Long> productIDs, WareHouse wh) {
        this.packageID = packageID;
        this.truckID = truckID;
        this.dest = dest;
        this.productIDs = productIDs;
        this.wh = wh;
    }

    public String getPackageID() {
        return packageID;
    }

    public String getTruckID() {
        return truckID;
    }

    public Location getDest() {
        return dest;
    }

    public List<Long> getProductIDs() {
        return productIDs;
    }

    public WareHouse getWh() {
        return wh;
    }
}
