package controllers;

import static spark.Spark.get;
import static spark.Spark.post;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;

import model.Booking;
import model.Database;
import model.NearbyVehicle;
import model.Position;
import model.Vehicle;

public class ApiController {

    /**
     * Parses user-posted coordinates
     */
    static class PositionRequest {
	double lat;
	double lng;
    }

    static class VehicleRequest {
	String registration;
	String make;
	String model;
	int year;
	String colour;
	PositionRequest position;
	boolean available;
    }

    static class BookingRequest {
	String timestamp;
	String registration;
	String customerId;
	int duration;
	PositionRequest startLocation;
	PositionRequest endLocation;
    }

    public ApiController() {

	final Logger logger = LoggerFactory.getLogger(ApiController.class);

	get("/api/vehicles/all", (req, res) -> {
	    res.type("application/json");

	    Database db = new Database();
	    List<Vehicle> vehicles = db.getVehicles();
	    db.close();

	    logger.info("Found " + vehicles.size() + " vehicles");
	    return new Gson().toJson(vehicles);
	});

	post("/api/vehicles/nearby", (req, res) -> {
	    res.type("application/json");
	    Position pos;
	    try {
		// use the posted position to create a Position object
		PositionRequest pr = new Gson().fromJson(req.body(), PositionRequest.class);
		pos = new Position(pr.lat, pr.lng);
	    } catch (JsonParseException e) {
		logger.error(e.getMessage());
		return "Error parsing request";
	    }
	    logger.info("Getting vehicles near " + pos);

	    Database db = new Database();
	    List<NearbyVehicle> nearby = db.getNearbyVehicles(pos);
	    db.close();

	    logger.info("Found " + nearby.size() + " nearby vehicles");
	    return new Gson().toJson(nearby);
	});

	get("/api/bookings", (req, res) -> {
	    res.type("application/json");

	    Database db = new Database();
	    List<Booking> bookings = db.getBookings();
	    db.close();

	    logger.info("Found " + bookings.size() + " bookings");
	    return new Gson().toJson(bookings);
	});

	post("/api/vehicles", (req, res) -> {
	    res.type("application/json");

	    Position pos;
	    VehicleRequest vr;
	    int available;
	    try {
		vr = new Gson().fromJson(req.body(), VehicleRequest.class);
		pos = new Position(vr.position.lat, vr.position.lng);
		if (vr.available == true) {
		    available = 1;
		} else {
		    available = 0;
		}
	    } catch (JsonParseException e) {
		logger.error(e.getMessage());
		return "Error parsing request";
	    }
	    logger.info("Inserting a car with rego: " + vr.registration);

	    Database db = new Database();
	    Vehicle inserted_vehicle = db.insertVehicle(vr.registration, vr.make, vr.model, vr.year, vr.colour, pos,
		    available);
	    db.close();

	    logger.info("Inserted successfully!");
	    return new Gson().toJson(inserted_vehicle);
	});

	post("/api/bookings", (req, res) -> {
	    res.type("application/json");
	    Position location_start, location_end;
	    BookingRequest br;
	    LocalDateTime dateTime;

	    try {
		br = new Gson().fromJson(req.body(), BookingRequest.class);

		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
		dateTime = LocalDateTime.parse(br.timestamp, formatter);

		location_start = new Position(br.startLocation.lat, br.startLocation.lng);
		location_end = new Position(br.endLocation.lat, br.endLocation.lng);
	    } catch (JsonParseException e) {
		logger.error(e.getMessage());
		return "Error parsing request";
	    }

	    logger.info("Inserting a booking!");
	    Database db = new Database();

	    Booking booking = db.createBooking(dateTime, br.registration, br.customerId, br.duration, location_start,
		    location_end);

	    db.close();

	    // logger.info("Inserted successfully!");
	    return new Gson().toJson(booking);
	});

	get("/api/vehicles", (req, res) -> {
	    res.type("application/json");

	    Database db = new Database();
	    List<Vehicle> vehicles = db.getAvailableVehicles();
	    db.close();

	    logger.info("Found " + vehicles.size() + " vehicles");
	    return new Gson().toJson(vehicles);
	});

    }

}
