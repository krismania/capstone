package controllers;

import static spark.Spark.get;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ApiController {

    public ApiController() {

	final Logger logger = LoggerFactory.getLogger(ApiController.class);

	get("/api/vehicles", (req, res) -> {
	    res.type("application/json");
	    String data = "";
	    try {
		byte[] encoded = Files.readAllBytes(Paths.get("src/main/resources/data/vehicles.json"));
		data = new String(encoded);
		logger.debug(data);
	    } catch (IOException e) {
		logger.error(e.getMessage());
	    }
	    return data;
	});

    }

}
