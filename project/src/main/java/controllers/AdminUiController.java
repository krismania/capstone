package controllers;

import static spark.Spark.before;
import static spark.Spark.get;
import static spark.Spark.halt;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AdminUiController {

    public AdminUiController(String mapsApiKey, String googleClientId) {

	// create model with API keys
	Map<String, Object> model = new HashMap<>();
	model.put("mapsApiKey", mapsApiKey);
	model.put("googleClientId", googleClientId);

	final Logger logger = LoggerFactory.getLogger(AdminUiController.class);

	// authenticate admin users
	before("/*", (req, res) -> {
	    logger.info("Admin Request: " + req.uri());
	    if (req.session(false) == null) {
		logger.info("User is not logged in");
		halt(401);
	    }
	    boolean isAdmin = req.session().attribute("isAdmin");
	    if (!isAdmin) {
		logger.info("User is NOT an administrator");
		halt(403);
	    }
	    logger.info("User is an administrator");
	});

	get("/", (req, res) -> {
	    logger.info("Serving admin dashboard");
	    return util.Util.render(model, "admin");
	});
    }

}
