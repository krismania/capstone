package controllers;

import static spark.Spark.get;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import util.Util;

public class UiController {

    public UiController(String mapsApiKey, String googleClientId) {

	// create model with API keys
	Map<String, Object> model = new HashMap<>();
	model.put("mapsApiKey", mapsApiKey);
	model.put("googleClientId", googleClientId);

	@SuppressWarnings("unused")
	final Logger logger = LoggerFactory.getLogger(UiController.class);

	get("/", (req, res) -> {
	    return Util.render(model, "index");
	});

	get("/account", (req, res) -> {
	    return Util.render(model, "account");
	});

    }

}
