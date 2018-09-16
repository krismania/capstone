package controllers;

import static spark.Spark.before;
import static spark.Spark.delete;
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
import controllers.Request.VehicleRequest;
import controllers.Request.VehicleStatusRequest;
import controllers.Response.ErrorResponse;
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
	    int status = 2;
	    try {
		vr = new Gson().fromJson(req.body(), VehicleRequest.class);
		pos = new Position(vr.position.lat, vr.position.lng);
		if (vr.status.equals("active")) {
		    status = 0;
		} else if (vr.status.equals("inactive")) {
		    status = 1;
		} else if (vr.status.equals("retired")) {
		    status = 2;
		} else {
		    res.status(400);
		    return new Gson().toJson(new ErrorResponse("Bad Request - Vehicle Creation Error"));
		}
	    } catch (JsonParseException | NullPointerException e) {
		logger.error(e.getMessage());
		res.status(400);
		return new Gson().toJson(new ErrorResponse("Error parsing request"));
	    }
	    logger.info("Inserting a car with rego: " + vr.registration);

	    Database db = new Database();
	    Vehicle inserted_vehicle = db.insertVehicle(vr.registration, vr.make, vr.model, vr.year, vr.colour, pos,
		    status);
	    db.close();

	    if (inserted_vehicle != null) {
		logger.info("Inserted successfully!");
		return new Gson().toJson(inserted_vehicle);
	    } else {
		logger.info("Clould not insert vehicle");
		res.status(400);
		return new Gson().toJson(new ErrorResponse("Vehicle was not created"));
	    }
	});

	// set the status of a particular vehicle
	// inactive vehicles can't be booked by clients
	post("/vehicle/status", (req, res) -> {
	    res.type("application/json");

	    VehicleStatusRequest var;
	    int status = 2;
	    try {
		var = new Gson().fromJson(req.body(), VehicleStatusRequest.class);
		if (var.status.equals("active")) {
		    status = 0;
		} else if (var.status.equals("inactive")) {
		    status = 1;
		} else if (var.status.equals("retired")) {
		    status = 2;
		} else {
		    res.status(400);
		    return new Gson().toJson(new ErrorResponse("Bad Request"));
		}

	    } catch (JsonParseException | NullPointerException e) {
		logger.error(e.getMessage());
		res.status(400);
		return new Gson().toJson(new ErrorResponse("Error parsing request"));
	    }

	    Database db = new Database();
	    Boolean dbResponse = db.changeVehicleStatus(var.registration, status);
	    db.close();

	    if (dbResponse) {
		res.status(200);
		logger.info("Changed availability of vehicle (" + var.registration + ")!");
		return "";
	    } else {
		res.status(400);
		return new Gson().toJson(new ErrorResponse("Bad Request - Vehicle Status"));
	    }
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
	delete("/bookings/:id", (req, res) -> {
	    int id = Integer.parseInt(req.params(":id"));

	    Database db = new Database();
	    Boolean dbResponse = db.deleteBooking(id);
	    db.close();
	    if (dbResponse) {
		res.status(200);
		return "";
	    } else {
		res.status(400);
		res.type("application/json");
		return new Gson().toJson(new ErrorResponse("Couldn't delete booking"));
	    }
	});

	// update a booking
	put("/bookings/:id", (req, res) -> {
	    res.type("application/json");

	    LocalDateTime dateTime;
	    EditBookingRequest br;

	    int id = Integer.parseInt(req.params(":id"));

	    try {
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

		br = new Gson().fromJson(req.body(), EditBookingRequest.class);
		dateTime = LocalDateTime.parse(br.timestamp, formatter);

	    } catch (JsonParseException | NullPointerException e) {
		logger.error(e.getMessage());
		res.status(400);
		return new Gson().toJson(new ErrorResponse("Error parsing request"));
	    }

	    Database db = new Database();
	    int status = db.checkVehicleStatus(br.registration);

	    if (status == 0) {
		Boolean dbResponse = db.editBooking(id, dateTime, br.registration, br.customerId, br.duration);

		db.close();

		if (dbResponse) {
		    res.status(200);
		    return "";
		} else {
		    res.status(400);
		    return new Gson().toJson(new ErrorResponse("Bad Request - Update Booking"));
		}
	    } else {
		db.close();
		res.status(400);
		return new Gson().toJson(new ErrorResponse("Bad Request - Update Booking"));
	    }

	});

    }

}
