package controllers;

import static spark.Spark.get;
import static spark.Spark.post;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken.Payload;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
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
		String sub = null;

		NetHttpTransport transport = new NetHttpTransport();
		JsonFactory jsonFactory = new JacksonFactory();

		if (loginRequest.id != null) {

		    GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(transport, jsonFactory)
			    .setAudience(Collections.singletonList(googleClientId)).build();

		    GoogleIdToken idToken = verifier.verify(loginRequest.id);

		    if (idToken != null) {
			Payload payload = idToken.getPayload();

			// Print user identifier
			String userId = payload.getSubject();
			logger.info("USER ID: " + userId);

			sub = payload.getSubject();

		    } else {
			logger.info("Invalid ID token.");
		    }
		}
		req.session(true);
		req.session().attribute("clientId", sub);
		logger.info("Client logged in: " + sub);
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

    }

    /**
     * Sent by the client when they log in via Google
     */
    static class LoginRequest {
	String id;
    }

}
