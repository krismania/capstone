import static spark.Spark.get;
import static spark.Spark.port;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import controllers.ApiController;
import spark.Spark;
import spark.resource.ClassPathResource;
import spark.resource.Resource;
import spark.servlet.SparkApplication;

public class Main implements SparkApplication {

    @Override
    public void init() {

	final Logger logger = LoggerFactory.getLogger(Main.class);

	// Load properties
	Properties prop = new Properties();

	Resource config = new ClassPathResource("config/config.properties");

	try (InputStream propFile = config.getInputStream()) {
	    prop.load(propFile);
	    logger.info("Config file loaded");
	} catch (IOException e) {
	    // if config can't be found, kill the server and display an error
	    logger.error("Config file was not loaded, make sure it exists");
	    System.exit(1);
	}

	logger.info("Launching Rebu server...");

	String mapsApiKey = prop.getProperty("mapsApiKey");

	// set port & create routes
	port(45678);

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
