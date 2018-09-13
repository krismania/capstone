package controllers;

import static spark.Spark.before;
import static spark.Spark.get;
import static spark.Spark.post;
import static spark.Spark.put;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;

import controllers.Request.EditBookingRequest;
import controllers.Request.VehicleAvailabilityRequest;
import controllers.Request.VehicleRequest;
import model.Booking;
import model.Database;
import model.Position;
import model.Vehicle;

/**
 * Contains routes to be used by the administrator front-end for managing the
 * system.
 */
public class AdminApiController {

    public AdminApiController() {

	final Logger logger = LoggerFactory.getLogger(AdminApiController.class);

	// log every API request
	before("/*", (req, res) -> logger.info("Admin API Request: " + req.uri()));

	// create a new vehicle
	post("/vehicles", (req, res) -> {
	    res.type("application/json");

	    Position pos;
	    VehicleRequest vr;
	    int active;
	    try {
		vr = new Gson().fromJson(req.body(), VehicleRequest.class);
		pos = new Position(vr.position.lat, vr.position.lng);
		if (vr.active == true) {
		    active = 1;
		} else {
		    active = 0;
		}
	    } catch (JsonParseException e) {
		logger.error(e.getMessage());
		return "Error parsing request";
	    }
	    logger.info("Inserting a car with rego: " + vr.registration);

	    Database db = new Database();
	    if (!db.vehicleExists(vr.registration)) {
		Vehicle inserted_vehicle = db.insertVehicle(vr.registration, vr.make, vr.model, vr.year, vr.colour, pos,
			active);
		db.close();
		res.status(200);
		return new Gson().toJson(inserted_vehicle);
	    } else {
		res.status(400);

	    }
	    db.close();
	    return "Unknown issue please contact an admin or try again.";

	});

	// set the status of a particular vehicle
	// inactive vehicles can't be booked by clients
	post("/vehicle/status", (req, res) -> {

	    VehicleAvailabilityRequest var;
	    int active;
	    try {
		var = new Gson().fromJson(req.body(), VehicleAvailabilityRequest.class);
		if (var.active == true) {
		    active = 1;
		} else {
		    active = 0;
		}

	    } catch (JsonParseException e) {
		logger.error(e.getMessage());
		return "Error parsing request";
	    }

	    Database db = new Database();
	    Boolean dbResponse = db.changeVehicleAvailability(var.registration, active);
	    db.close();

	    if (dbResponse) {
		res.status(200);
		logger.info("Changed availability of vehicle (" + var.registration + ")!");
	    } else {
		res.status(400);
	    }

	    return "Unknown issue please contact an admin or try again.";
	});

	// returns a list of all vehicles
	get("/vehicles/all", (req, res) -> {
	    res.type("application/json");

	    Database db = new Database();
	    List<Vehicle> vehicles = db.getVehicles();
	    db.close();

	    logger.info("Found " + vehicles.size() + " vehicles");
	    return new Gson().toJson(vehicles);
	});

	// returns a list of all bookings
	get("/bookings/all", (req, res) -> {
	    res.type("application/json");

	    Database db = new Database();
	    List<Booking> bookings = db.getBookings();
	    db.close();

	    logger.info("Found " + bookings.size() + " bookings");
	    return new Gson().toJson(bookings);
	});

	// delete a booking
	get("/bookings/delete", (req, res) -> {
	    int id = Integer.parseInt(req.queryParams("id"));

	    Database db = new Database();
	    Boolean dbResponse = db.deleteBooking(id);
	    db.close();
	    if (dbResponse) {
		res.status(200);
	    } else {
		res.status(400);
	    }

	    return "Unknown issue please contact an admin or try again.";
	});

	// update a booking
	put("/bookings/:id", (req, res) -> {
	    res.type("application/json");

	    Position location_start, location_end;
	    LocalDateTime dateTime;
	    EditBookingRequest br;

	    int id = Integer.parseInt(req.params(":id"));

	    try {
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

		br = new Gson().fromJson(req.body(), EditBookingRequest.class);

		dateTime = LocalDateTime.parse(br.timestamp, formatter);
		location_start = new Position(br.startLocation.lat, br.startLocation.lng);
		location_end = new Position(br.endLocation.lat, br.endLocation.lng);

	    } catch (JsonParseException e) {
		logger.error(e.getMessage());
		return "Error parsing request";
	    }

	    Database db = new Database();

	    Boolean dbResponse = db.editBooking(id, dateTime, br.registration, br.customerId, br.duration,
		    location_start, location_end);

	    db.close();

	    if (dbResponse) {
		res.status(200);
	    } else {
		res.status(400);
	    }
	    res.status(400);
	    return "Unknown issue please contact an admin or try again.";

	});

    }

}
