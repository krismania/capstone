package model;

import java.time.LocalDateTime;

public class Booking {

    private final int id;
    private final LocalDateTime timestamp;
    private final Vehicle vehicle;
    private final String customerId;
    private final int duration;
    private final Position startLocation;
    private final int cost;

    protected Booking(int id, LocalDateTime timestamp, Vehicle vehicle, String customerId, int duration,
	    Position startLocation, int cost) {
	this.id = id;
	this.timestamp = timestamp;
	this.vehicle = vehicle;
	this.customerId = customerId;
	this.duration = duration;
	this.startLocation = startLocation;
	this.cost = cost;
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

    public int getCost() {
	return this.cost;
    }

    public Position getStartLocation() {
	return this.startLocation;
    }

}
