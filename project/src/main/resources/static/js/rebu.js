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
		}
	
	}
	
})();
