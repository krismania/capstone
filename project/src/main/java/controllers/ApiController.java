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
import controllers.Request.CreditRequest;
import controllers.Request.PositionRequest;
import model.Booking;
import model.CreditCard;
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

	// returns a list of nearby vehicles, giving their distance to the
	// client
	post("/vehicles/nearby", (req, res) -> {
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

	// create a booking
	post("/bookings", (req, res) -> {
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

	    String clientId = req.session().attribute("clientId");

	    Booking booking = db.createBooking(dateTime, br.registration, clientId, br.duration, location_start,
		    location_end);

	    db.close();

	    // logger.info("Inserted successfully!");
	    return new Gson().toJson(booking);
	});

	// returns a list of the logged in client's bookings
	get("/bookings", (req, res) -> {
	    res.type("application/json");
	    String clientId = req.session().attribute("clientId");

	    Database db = new Database();
	    List<Booking> bookings = db.getBookingsOfUser(clientId);

	    logger.info("Found " + bookings.size() + " bookings of user " + clientId);

	    db.close();
	    return new Gson().toJson(bookings);
	});

	get("/credit", (req, res) -> {
	    res.type("application/json");
	    String clientId = req.session().attribute("clientId");

	    CreditRequest cr;

	    try {
		cr = new Gson().fromJson(req.body(), CreditRequest.class);

		String cNumber = cr.cNumber;
		String bNumber = cr.bNumber;
		String expDate = cr.expDate;
		String cName = cr.cName;

		Database db = new Database();
		CreditCard creditCard = db.insertCredit(clientId, cName, cNumber, bNumber, expDate);
		db.close();

	    } catch (JsonParseException e) {
		logger.error(e.getMessage());
		return "Error parsing request";
	    }
	    return "";
	});
    }

}
