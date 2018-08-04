package controllers;

import static spark.Spark.get;

public class ApiController {

    public ApiController() {

	get("/api/vehicles", (req, res) -> {
	    res.type("application/json");
	    return "[{\"registration\":\"ABC123\",\"make\":\"Toyota\",\"model\":\"Corolla\",\"year\":2014,\"color\":\"Blue\",\"position\":{\"lat\":-37.808401,\"lon\":144.956159},\"available\":True}]";
	});

    }

}
