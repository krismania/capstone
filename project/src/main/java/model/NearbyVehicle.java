package model;

public class NearbyVehicle extends Vehicle {

    private final int distance;

    protected NearbyVehicle(String registration, String make, String model, int year, String colour, Position position,
	    int distance) {
	super(registration, make, model, year, colour, position);
	this.distance = distance;
    }

    public int getDistance() {
	return this.distance;
    }

}
