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

    @Test
    void testVehicleArray() {

	Vehicle veh = new Vehicle("ABC123", "Toyota", "Corolla", 2014, "Blue", new Position(-37.808401, 144.956159));
	List<Vehicle> vehicles = new ArrayList<Vehicle>();
	vehicles = this.database.getVehicles();

	assertEquals(veh.getMake(), vehicles.get(0).getMake());
    }

    @Test
    void testNearbyArrayReturns() {

	Position pos = new Position(-37.808401, 144.956159);
	List<Vehicle> vehicles = new ArrayList<Vehicle>();
	vehicles = this.database.getVehicles();
	List<NearbyVehicle> vehiclesN = new ArrayList<NearbyVehicle>();
	vehiclesN = this.database.getNearbyVehicles(pos);

	assertEquals(vehiclesN.get(0).getDistance(), 0);
    }

    @Test
    void testNearbyFirstCar() {

	Position pos = new Position(-37.808401, 144.956159);
	List<Vehicle> vehicles = new ArrayList<Vehicle>();
	vehicles = this.database.getVehicles();
	List<NearbyVehicle> vehiclesN = new ArrayList<NearbyVehicle>();
	vehiclesN = this.database.getNearbyVehicles(pos);

	assertEquals(vehiclesN.get(0).getDescription(), vehicles.get(0).getDescription());
    }

}
