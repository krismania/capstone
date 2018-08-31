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
					vehicle.description = vehicle.make + " " + vehicle.model + " (" + vehicle.year + ")";
					vehicle.available = true;
					
					vehicles.push(vehicle);
				};
				console.log(vehicles);
				callback(vehicles);
			});
		},
		
		getNearby: function(pos, callback) {
			console.log("[api] getting nearby vehicles");
			var vehicles = [];
			var headers = new Headers();
			headers.append("Content-Type", "application/json");
			var request = new Request('/api/vehicles/nearby', {
				method: 'post',
				headers: headers,
				body: JSON.stringify(pos)
			});
			
			fetch(request)
			.then(res => res.json())
			.then(json => {
				for (var i = 0; i < json.length; i++) {
					var vehicle = json[i];
					
					// TODO: temporary fix for model mismatch
					vehicle.description = vehicle.make + " " + vehicle.model + " (" + vehicle.year + ")";
					vehicle.available = true;
					
					// make the distance prettier
					vehicle.distance = vehicle.distance.toFixed(2) + " km";
					
					vehicles.push(vehicle);
				}
				console.log(vehicles);
				callback(vehicles)
			});
		},
		
		requestBooking: function(bookingRequest, callback) {
			console.log("[api] requesting booking", bookingRequest);
			// TODO: link this route
			succeeded = false;
			callback(succeeded);
		}
	
	}
	
})();
