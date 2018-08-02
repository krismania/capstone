import static spark.Spark.get;
import static spark.Spark.port;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import spark.Spark;

public class Main {

    public static void main(String[] args) {

	final Logger logger = LoggerFactory.getLogger(Main.class);

	// Load properties
	Properties prop = new Properties();
	try (InputStream propFile = new FileInputStream("config.properties")) {
	    prop.load(propFile);
	    logger.info("Config file loaded");
	} catch (IOException e) {
	    // if config can't be found, kill the server and display an error
	    logger.error("Config file was not loaded, make sure it exists");
	    System.exit(1);
	}

	String mapsApiKey = prop.getProperty("mapsApiKey");

	// set port & create routes
	port(4567);

	// use static folder in resources for static content
	Spark.staticFiles.location("/static");

	get("/", (req, res) -> {
	    Map<String, Object> model = new HashMap<>();
	    model.put("mapsApiKey", mapsApiKey);
	    return Util.render(model, "index");
	});
    }

}
