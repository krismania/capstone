import static spark.Spark.get;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.appengine.api.utils.SystemProperty;

import controllers.ApiController;
import spark.Spark;
import spark.servlet.SparkApplication;
import util.Config;
import util.Util;

public class Main implements SparkApplication {

    @Override
    public void init() {

	final Logger logger = LoggerFactory.getLogger(Main.class);

	// Load configuration
	try {
	    Config.loadConfig();
	    logger.info("Config file loaded");
	} catch (IOException e) {
	    // if config can't be found, kill the server and display an error
	    logger.error("Config file was not loaded, make sure it exists");
	    System.exit(1);
	}

	logger.info("Launching Rebu server...");

	// get keys from config depending on environment
	String mapsApiKey;
	String googleClientId;
	if (SystemProperty.environment.value() == SystemProperty.Environment.Value.Production) {
	    mapsApiKey = Config.get("remoteMapsApiKey");
	    googleClientId = Config.get("remoteGoogleClientId");
	    if (mapsApiKey == null || googleClientId == null) {
		logger.error("Maps API or ClientID is null");
		System.exit(1);
	    }
	} else {
	    mapsApiKey = Config.get("localMapsApiKey");
	    googleClientId = Config.get("localGoogleClientId");
	    if (mapsApiKey == null || googleClientId == null) {
		logger.error("Maps API or ClientID is null");
		System.exit(1);
	    }
	}

	/* == ROUTES == */

	// use static folder in resources for static content
	Spark.staticFiles.location("static");

	// api controller routes
	new ApiController();

	// all other routes
	get("/", (req, res) -> {
	    Map<String, Object> model = new HashMap<>();
	    model.put("mapsApiKey", mapsApiKey);
	    model.put("googleClientId", googleClientId);
	    return Util.render(model, "index");
	});
    }

}
