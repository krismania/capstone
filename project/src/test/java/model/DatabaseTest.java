package model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

public class DatabaseTest {

    @Test
    void connectToDatabase() {

	DatabaseDummy db = new DatabaseDummy();
	System.out.println("Test 1: Connection To Database Success");
    }

    @Test
    void dbGetVehicles() {

	DatabaseDummy db = new DatabaseDummy();
	List<Vehicle> veh = new ArrayList<Vehicle>();
	veh = db.getVehicles();

	Position pos = new Position(0, 0);
	Vehicle vehi = new Vehicle("ABC123", "Toyota", "Corolla", 2014, "Blue", pos, 0);
	assertEquals(vehi.getRegistration(), veh.get(0).getRegistration());
	System.out.println("Test 2: Assert 1 passed.");

	assertEquals(veh.size(), 10);
	System.out.println("Test 2: Assert 2 passed.");
    }

    @Test
    void dbAvail() {

	DatabaseDummy db = new DatabaseDummy();
	List<Vehicle> veh = new ArrayList<Vehicle>();
	veh = db.getAvailableVehicles();

	Position pos = new Position(0, 0);
	Vehicle vehi = new Vehicle("BLA555","Mazda", "3", 2010, "White", pos, 1);
	assertEquals(vehi.getRegistration(), veh.get(0).getRegistration());

	System.out.println("Test 3: Assert 1 passed.");
    }

    @Test
    void dbNearby()
    {
        DatabaseDummy db = new DatabaseDummy();
        Position pos = new Position(-37.808401, 144.956159);
        List<NearbyVehicle> veh = new ArrayList<NearbyVehicle>();
        veh = db.getNearbyVehicles(pos);

        int expected = 0;
        assertEquals(expected, veh.get(0).getDistance());
        System.out.println("Test 4: Assert 1 passed.");

        String name = "ABC123";
        assertEquals(name, veh.get(0).getRegistration());
        System.out.println("Test 4: Assert 2 passed.");
    }

    @Test
    void dbBooking()
    {
        DatabaseDummy db = new DatabaseDummy();
        List<Booking> book = new ArrayList<Booking>();
        book = db.getBookings();

        String custID = "112606983151403770748";
        assertEquals(custID, book.get(0).getCustomerId());
        System.out.println("Test 5: Assert 1 passed.");

        Vehicle ve = book.get(0).getVehicle();
        String reg = "YODUDE";
        assertEquals(reg, ve.getRegistration());
        System.out.println("Test 5: Assert 2 passed.");
    }

}
