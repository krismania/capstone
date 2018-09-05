package model;

import java.time.LocalDateTime;

public class Booking {

    private final int id;
    private final LocalDateTime timestamp;
    private final Vehicle vehicle;
    private final String customerId;
    private final int duration;
    private final Position startLocation;
    private final Position endLocation;

    protected Booking(int id, LocalDateTime timestamp, Vehicle vehicle, String customerId, int duration,
	    Position startLocation, Position endLocation) {
	this.id = id;
	this.timestamp = timestamp;
	this.vehicle = vehicle;
	this.customerId = customerId;
	this.duration = duration;
	this.startLocation = startLocation;
	this.endLocation = endLocation;
    }

    public int getId() {
	return this.id;
    }

    public LocalDateTime getTimestamp() {
	return this.timestamp;
    }

    public Vehicle getVehicle() {
	return this.vehicle;
    }

    public String getCustomerId() {
	return this.customerId;
    }

    public int getDuration() {
	return this.duration;
    }

    public Position getStartLocation() {
	return this.startLocation;
    }

    public Position getEndLocation() {
	return this.endLocation;
    }

}
