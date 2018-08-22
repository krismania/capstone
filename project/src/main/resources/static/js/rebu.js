// contains all API requests
rebu = (function() {
	
	return {
		
		getVehicles: function(callback) {
			console.log("[api] getting all vehicles");
			var vehicles = [];
			var request = new Request('/api/vehicles');
			fetch(request)
			.then(res => res.json())
			.then(json => {
				for (var i = 0; i < json.length; i++) {
					var vehicle = json[i];
					
					// TODO: temporary fix for model mismatch
					vehicle.colour = vehicle.color;
					vehicle.description = vehicle.make + " " + vehicle.model + " (" + vehicle.year + ")";
					
					vehicles.push(vehicle);
				};
				console.log(vehicles);
				callback(vehicles);
			});
		},
		
		getNearby: function(pos, callback) {
			console.log("[api] getting all vehicles");
			// TODO: link this route
			var vehicles = [
				{registration: "QRB990", description: "BMW 325i (2003)", colour: "Black", distance: "500 m"},
				{registration: "JTD955", description: "Holden Commodore (2005)", colour: "Grey", distance: "800 m"},
				{registration: "FOK356", description: "Holden Barina (2017)", colour: "White", distance: "1.2 km"},
				{registration: "QOP299", description: "Kia Rio (2013)", colour: "Pink", distance: "1.8 km"},
				{registration: "YODUDE", description: "Nissan Skyline (2010)", colour: "Black", distance: "2.1 km"},
			];
			callback(vehicles);
		}
	
	}
	
})();
