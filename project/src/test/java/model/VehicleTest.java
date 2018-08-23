package model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class VehicleTest {

    private Vehicle vehicle;

    @BeforeEach
    void setUp() throws Exception {
	this.vehicle = new Vehicle("ABC123", "SomeMake", "Some Model", 2010, "Yellow");
    }

    @Test
    void testGetDescription() {
	String expected = "SomeMake Some Model (2010)";
	assertEquals(expected, this.vehicle.getDescription());
    }

}
