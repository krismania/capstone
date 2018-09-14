// contains all API requests
rebu = (function() {
	
	function zeroPad(number) {
		if (number < 10) {
			return "0" + number;
		} else {
			return "" + number;
		}
	}
	
	function dateToString(d) {
		var year = d.getFullYear();
		var month = zeroPad(d.getMonth() + 1);
		var day = zeroPad(d.getDate());
		var hours = zeroPad(d.getHours());
		var minutes = zeroPad(d.getMinutes());
		var seconds = zeroPad(d.getSeconds());
		return year + "-" + month + "-" + day + " "
			+ hours + ":" + minutes + ":" + seconds;
	}
	
	return {
		
		getVehicles: function(callback) {
			console.log("[api] getting all available vehicles");
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
			var booking = {
			    timestamp: dateToString(new Date()),
			    registration: bookingRequest.registration,
			    customerId: bookingRequest.client,
			    duration: bookingRequest.duration
			}
			
			booking = JSON.stringify(booking);
			console.log("Booking:", booking);
						
			var headers = new Headers();
			headers.append("Content-Type", "application/json");
			var request = new Request('/api/bookings', {
				method: 'post',
				headers: headers,
				body: booking
			});
			
			fetch(request)
			.then(res => res.json())
			.then(json => {
				console.log(json);
				// if response was null, booking failed
				if (json == null) {
					callback(false);
				} else {
					callback(true);
				}
			});
		},
		
		getBookings: function(user, callback) {
			console.log("[api] getting bookings for " + user);
			var request = new Request('/api/bookings?id=' + user);
			fetch(request)
			.then(res => res.json())
			.then(json => {
				callback(json);
			});
		}
	
	}
	
})();
