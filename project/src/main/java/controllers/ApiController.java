package controllers;

import static spark.Spark.before;
import static spark.Spark.get;
import static spark.Spark.post;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;

import controllers.Request.BookingRequest;
import controllers.Request.PositionRequest;
import controllers.Response.ErrorResponse;
import model.Booking;
import model.Database;
import model.NearbyVehicle;
import model.Position;
import model.Vehicle;

public class ApiController {

    public ApiController() {

	final Logger logger = LoggerFactory.getLogger(ApiController.class);

	// log every API request
	before("/*", (req, res) -> logger.info("Client API Request: " + req.uri()));

	// returns a list of available vehicles
	get("/vehicles", (req, res) -> {
	    res.type("application/json");

	    Database db = new Database();
	    List<Vehicle> vehicles = db.getAvailableVehicles();
	    db.close();

	    logger.info("Found " + vehicles.size() + " vehicles");
	    return new Gson().toJson(vehicles);
	});

	// returns a list of nearby vehicles, giving their distance to the client
	post("/vehicles/nearby", (req, res) -> {
	    res.type("application/json");
	    Position pos;
	    try {
		// use the posted position to create a Position object
		PositionRequest pr = new Gson().fromJson(req.body(), PositionRequest.class);
		pos = new Position(pr.lat, pr.lng);
	    } catch (JsonParseException e) {
		logger.error(e.getMessage());
		res.status(400);
		return new Gson().toJson(new ErrorResponse("Error parsing request"));
	    }
	    logger.info("Getting vehicles near " + pos);

	    Database db = new Database();
	    List<NearbyVehicle> nearby = db.getNearbyVehicles(pos);
	    db.close();

	    logger.info("Found " + nearby.size() + " nearby vehicles");
	    return new Gson().toJson(nearby);
	});

	// create a booking
	post("/bookings", (req, res) -> {
	    res.type("application/json");
	    BookingRequest br;
	    LocalDateTime dateTime;

	    try {
		br = new Gson().fromJson(req.body(), BookingRequest.class);

		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
		dateTime = LocalDateTime.parse(br.timestamp, formatter);
	    } catch (JsonParseException e) {
		logger.error(e.getMessage());
		res.status(400);
		return new Gson().toJson(new ErrorResponse("Error parsing request"));
	    }

	    logger.info("Inserting a booking!");
	    Database db = new Database();

	    String clientId = req.session().attribute("clientId");

	    Booking booking = db.createBooking(dateTime, br.registration, clientId, br.duration);

	    db.close();
	    if (booking != null) {
		res.type("application/json");
		return new Gson().toJson(booking);
	    } else {
		res.status(400);
		new Gson().toJson(new ErrorResponse("Bad Request"));
	    }
	    res.status(204);
	    return "";

	});

	// returns a list of the logged in client's bookings
	get("/bookings", (req, res) -> {
	    res.type("application/json");
	    String clientId = req.session().attribute("clientId");

	    // return unauthorized response if user not logged in
	    if (clientId == null) {
		res.status(401);
		return new Gson().toJson(new ErrorResponse("Please log in"));
	    }

	    Database db = new Database();
	    List<Booking> bookings = db.getBookingsOfUser(clientId);

	    logger.info("Found " + bookings.size() + " bookings of user " + clientId);

	    db.close();
	    return new Gson().toJson(bookings);
	});

	get("/bookings/now", (req, res) -> {
	    String clientId = req.session().attribute("clientId");

	    // return unauthorized response if user not logged in
	    if (clientId == null) {
		res.status(401);
		return new Gson().toJson(new ErrorResponse("Please log in"));
	    }

	    Booking br;
	    Database db = new Database();

	    br = db.getBookingNow(clientId);
	    db.close();

	    if (br != null) {
		res.type("application/json");
		return new Gson().toJson(br);
	    } else {
		// send "no-content" status
		res.status(204);
		return "";
	    }
	});

    }

}
