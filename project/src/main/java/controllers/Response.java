package controllers;

import java.util.List;

import model.Position;
import model.Vehicle;

class Response {

    private Response() {
    }

    static class ErrorResponse {
	String message;

	ErrorResponse(String message) {
	    this.message = message;
	}
    }

    static class ClientIdResponse {
	String clientId;

	public ClientIdResponse(String clientId) {
	    this.clientId = clientId;
	}
    }

    static class RouteResponse {
	List<Position> route;

	public RouteResponse(List<Position> route) {
	    this.route = route;
	}
    }

    static class BookedVehiclesResponse extends Vehicle {

	final boolean booked;

	protected BookedVehiclesResponse(Vehicle vehicle, boolean booked) {
	    super(vehicle.getRegistration(), vehicle.getMake(), vehicle.getModel(), vehicle.getYear(),
		    vehicle.getColour(), vehicle.getPosition(), vehicle.getStatus(), vehicle.getType());
	    this.booked = booked;
	}
    }

}
