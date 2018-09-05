package controllers;

import static spark.Spark.get;
import static spark.Spark.post;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

import util.Util;

public class UiController {

    public UiController(String mapsApiKey, String googleClientId) {

	// create model with API keys
	Map<String, Object> model = new HashMap<>();
	model.put("mapsApiKey", mapsApiKey);
	model.put("googleClientId", googleClientId);

	final Logger logger = LoggerFactory.getLogger(UiController.class);

	post("/login", (req, res) -> {
	    LoginRequest loginRequest = new Gson().fromJson(req.body(), LoginRequest.class);
	    // set up session
	    if (req.session(false) == null || req.session().attribute("clientId") == null) {
		req.session(true);
		req.session().attribute("clientId", loginRequest.id);
		logger.info("Client logged in: " + loginRequest.id);
	    }
	    res.status(200);
	    return "";
	});

	get("/logout", (req, res) -> {
	    if (req.session(false) != null) {
		String id = req.session().attribute("clientId");
		req.session().removeAttribute("clientId");
		logger.info("Client logged out: " + id);
		res.status(200);
	    } else {
		res.status(400);
	    }
	    return "";
	});

	get("/", (req, res) -> {
	    return Util.render(model, "index");
	});

	get("/account", (req, res) -> {
	    Map<String, Object> m = new HashMap<>(model);
	    m.put("clientId", req.session().attribute("clientId"));
	    return Util.render(m, "account");
	});

    }

    /**
     * Sent by the client when they log in via Google
     */
    static class LoginRequest {
	String id;
    }

}
