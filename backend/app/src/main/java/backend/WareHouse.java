package backend;

public class WareHouse {
    private final int id;
    private Location location;

    public WareHouse(int id, Location location) {
        this.id = id;
        this.location = location;
    }

    public int getId() {
        return id;
    }

    public int getX() {
        return location.getXLocation();
    }

    public int getY() {
        return location.getYLocation();
    }

    public Location getLocation() {
        return location;
    }
}
