import static spark.Spark.get;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import controllers.ApiController;
import spark.Spark;
import spark.servlet.SparkApplication;
import util.Config;

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

	String mapsApiKey = Config.get("mapsApiKey");

	/* == ROUTES == */

	// use static folder in resources for static content
	Spark.staticFiles.location("static");

	// api controller routes
	new ApiController();

	// all other routes
	get("/", (req, res) -> {
	    Map<String, Object> model = new HashMap<>();
	    model.put("mapsApiKey", mapsApiKey);
	    return Util.render(model, "index");
	});
    }

}
