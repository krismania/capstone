package controllers;

class Request {

    private Request() {
    }

    /**
     * Parses user-posted coordinates
     */
    static class PositionRequest {
	double lat;
	double lng;
    }

    static class BookingRequest {
	String timestamp;
	String registration;
	int duration;
	PositionRequest startLocation;
	PositionRequest endLocation;
    }

    static class EditBookingRequest extends BookingRequest {
	String customerId;
    }

    static class VehicleAvailabilityRequest {
	String registration;
	boolean active;
    }

    static class VehicleRequest {
	String registration;
	String make;
	String model;
	int year;
	String colour;
	PositionRequest position;
	boolean active;
    }

}
