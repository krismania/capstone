package model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Date;

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

    @Test
    void dbVehByReg()
    {
        DatabaseDummy db = new DatabaseDummy();
        String reg = "YODUDE";

        Vehicle ve = db.getVehicleByReg(reg);
        assertEquals(reg, ve.getRegistration());
        System.out.println("Test 6: Assert 1 passed.");
        assertEquals("Black", ve.getColour());
    }

    @Test
    void dbDoubBooked()
    {
        DatabaseDummy db = new DatabaseDummy();
        boolean check;

        LocalDateTime dateTime;

        String reg = "YODUDE";
        dateTime = LocalDateTime.now();

        check = db.isCarDoubleBooked(dateTime, reg);
        assertFalse(check);
        System.out.println("Test 7: Assert 1 passed.");
    }

    @Test
    void dbUserDoubBooked()
    {
        DatabaseDummy db = new DatabaseDummy();
        boolean check;

        LocalDateTime dateTime;

        String custID = "112606983151403770748";
        dateTime = LocalDateTime.now();

        check = db.isUserDoubleBooked(dateTime, custID);
        assertFalse(check);
        System.out.println("Test 8: Assert 1 passed.");
    }

    @Test
    void changeAvail()
    {
        DatabaseDummy db = new DatabaseDummy();
        boolean check;
        String falseReg = "YODUDE";
        int avail = 1;

        check = db.changeVehicleAvailability(falseReg, avail);
        assertTrue(check);
        System.out.println("Test 9: Assert 1 passed.");
    }

    @Test
    void dbUserBookings()
    {
        DatabaseDummy db = new DatabaseDummy();
        List<Booking> book = new ArrayList<Booking>();
        String email = "112606983151403770748";
        book = db.getBookingsOfUser(email);

        int bookSize = 2;

        assertEquals(bookSize, book.size());
        System.out.println("Test 10: Assert 1 passed.");

        Vehicle ve = book.get(0).getVehicle();
        String reg = "YODUDE";
        assertEquals(reg, ve.getRegistration());
        System.out.println("Test 10: Assert 2 passed.");
    }

    @Test
    void dbDeleteBooking()
    {
        DatabaseDummy db = new DatabaseDummy();
        boolean check;
        int delete = 100;

        check = db.deleteBooking(delete);
        assertFalse(check);
        System.out.println("Test 11: Assert 1 passed.");

    }

    @Test
    void dbEditBookings()
    {
        DatabaseDummy db = new DatabaseDummy();
        boolean check;

        int id = 1;
        LocalDateTime dateTime = LocalDateTime.now();
        String reg = "YODUDE";
        String custID = "112606983151403770748";
        int duration = 120;
        Position startLocation = new Position(-37.815603, 144.969967);
        Position endLocation = new Position(-37.807232, 144.963620);

        check = db.editBooking(id, dateTime, reg, custID, duration, startLocation, endLocation);

        assertTrue(check);
        System.out.println("Test 12: Assert 1 passed.");
    }

    @Test
    void dbBookingExists()
    {
        DatabaseDummy db = new DatabaseDummy();
        boolean check;
        int id = 1;

        check = db.bookingExists(id);

        assertTrue(check);
        System.out.println("Test 13: Assert 1 passed.");
    }

    @Test
    void dbCheckVeh()
    {
        DatabaseDummy db = new DatabaseDummy();
        boolean check;
        String reg = "YODUDE";

        check = db.checkReg(reg);

        assertTrue(check);
        System.out.println("Test 14: Assert 1 passed.");
    }
}
