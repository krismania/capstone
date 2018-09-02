package model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

public class DatabaseTest {

    private Database database = new Database();

    @Test
    void testGetVehicleSize() {

	int expected = 10;
	List<Vehicle> vehicles = new ArrayList<Vehicle>();
	vehicles = this.database.getVehicles();

	System.out.println(vehicles.size());
	assertEquals(expected, vehicles.size());
    }
}
