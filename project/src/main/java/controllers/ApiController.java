package controllers;

import static spark.Spark.get;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import spark.resource.ClassPathResource;
import spark.resource.Resource;

public class ApiController {

    public ApiController() {

	final Logger logger = LoggerFactory.getLogger(ApiController.class);

	get("/api/vehicles", (req, res) -> {
	    res.type("application/json");
	    String data = "";
	    try {
		Resource vehiclesResource = new ClassPathResource("data/vehicles.json");
		byte[] vehicleResourceBytes = new byte[(int) vehiclesResource.contentLength()];
		vehiclesResource.getInputStream().read(vehicleResourceBytes);
		data = new String(vehicleResourceBytes);
		logger.debug(data);
	    } catch (IOException e) {
		logger.error(e.getMessage());
	    }
	    return data;
	});

    }

}
