package controllers;

import static spark.Spark.get;
import static spark.Spark.post;

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

    public ApiController() {

	final Logger logger = LoggerFactory.getLogger(ApiController.class);

	get("/api/vehicles", (req, res) -> {
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

    }

}
