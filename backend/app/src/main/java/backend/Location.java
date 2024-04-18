package backend;

public class Location {
    private Integer xLocation;
    private Integer yLocation;

    public Location(Integer xLocation, Integer yLocation) {
        this.xLocation = xLocation;
        this.yLocation = yLocation;
    }

    public Location() {
        this(0, 0);
    }

    public Integer getXLocation() {
        return xLocation;
    }

    public void setXLocation(Integer xLocation) {
        this.xLocation = xLocation;
    }

    public Integer getYLocation() {
        return yLocation;
    }

    public void setYLocation(Integer yLocation) {
        this.yLocation = yLocation;
    }

    public Location getCloest(Location[] locations) {
        if (locations == null || locations.length == 0) {
            return null; 
        }
        Location cloest = locations[0];
        double minDistance = Math.sqrt(Math.pow(xLocation - cloest.getXLocation(), 2) + Math.pow(yLocation - cloest.getYLocation(), 2));
        for (Location location : locations) {
            double distance = Math.sqrt(Math.pow(xLocation - location.getXLocation(), 2) + Math.pow(yLocation - location.getYLocation(), 2));
            if (distance < minDistance) {
                minDistance = distance;
                cloest = location;
            }
        }
        return cloest;
    }

}
