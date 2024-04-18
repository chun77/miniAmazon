package backend;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

public class LocationTest {
    @Test
    public void testGetCloest() {
        Location[] locations = {
            new Location(3, 4),
            new Location(6, 8),
            new Location(1, 2)
        };
        Location targetLocation = new Location(0, 0);
        Location expectedClosest = locations[2];

        Location actualClosest = targetLocation.getCloest(locations);

        assertEquals(expectedClosest, actualClosest);
    }

    @Test
    public void testGetCloestWithEqualDistances() {
        Location[] locations = {
            new Location(3, 4),
            new Location(6, 8),
            new Location(1, 2)
        };
        Location targetLocation = new Location(5, 6);
        Location expectedClosest = locations[1];

        Location actualClosest = targetLocation.getCloest(locations);

        assertEquals(expectedClosest, actualClosest);
    }

    @Test
    public void testGetCloestWithSingleLocation() {
        Location[] locations = { new Location(3, 4) };
        Location targetLocation = new Location(0, 0);

        Location actualClosest = targetLocation.getCloest(locations);

        assertEquals(locations[0], actualClosest);
    }

    @Test
    public void testGetCloestWithEmptyLocations() {
        Location[] locations = {};
        Location targetLocation = new Location(0, 0);

        Location actualClosest = targetLocation.getCloest(locations);

        assertNull(actualClosest);
    }
}
