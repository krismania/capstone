// contains JS blueprints for dynamic views
var view = (function() {
	
	return {
		
		vehicleInfo: function(vehicle) {
			var container = document.createElement("div");
			var desc = document.createElement("h3");
			var colour = document.createElement("p");
			var rego = document.createElement("p");
			
			container.className = "vehicle-info";
			desc.innerText = vehicle.description;
			colour.innerText = "Colour: " + vehicle.colour;
			rego.innerText = "Registration: " + vehicle.registration;
			
			container.appendChild(desc);
			container.appendChild(colour);
			container.appendChild(rego);
			
			return container;
		},
		
		nearbyVehicle: function(vehicle, callback) {
			var container = document.createElement("div");
			
			// create vehicle info
			var vehicleInfo = this.vehicleInfo(vehicle);
			
			// create book button
			var bookButtonContainer = document.createElement("div");
			var bookButton = document.createElement("button");
			var distance = document.createElement("p");
			
			container.className = "nearby-info";
			bookButtonContainer.className = "book-container";
			bookButton.innerText = "BOOK";
			distance.innerText = vehicle.distance;
			
			bookButtonContainer.appendChild(distance);
			bookButtonContainer.appendChild(bookButton);
			
			// create listener for book button
			bookButton.addEventListener('click', callback);
			
			// append to the container & return
			container.appendChild(vehicleInfo);
			container.appendChild(bookButtonContainer)
			return container;
		},
		
		bookingForm: function(vehicle) {			
			var form = document.createElement("form");
			var submit = document.createElement("button");
			
			// create dropoff fieldset
			var dropoff = document.createElement("fieldset");
			var dropoffLegend = document.createElement("legend");
			var dropoffTime = document.createElement("select");
			var dropoffLocation = document.createElement("input");
			
			dropoffLegend.innerText = "Drop off";
			dropoffTime.name = "dropoff-time";
			dropoffLocation.name = "dropoff-location";
			dropoffLocation.placeholder = "Enter an address...";
			
			// add options to select
			options = [
				{value: 60, text: "1 Hour"},
				{value: 120, text: "2 Hours"},
				{value: 180, text: "3 Hours"},
				{value: 360, text: "6 Hours"},
				{value: 720, text: "12 Hours"},
				{value: 1440, text: "24 Hours"},
			];
			for (var i = 0; i < options.length; i++) {
				option = document.createElement("option");
				option.value = options[i].value;
				option.innerText = options[i].text;
				dropoffTime.appendChild(option);
			}
			
			// append everything to the fieldset
			dropoff.appendChild(dropoffLegend);
			dropoff.appendChild(dropoffTime);
			dropoff.appendChild(dropoffLocation);
			
			// set up submit button
			submit.className = "confirm";
			submit.type = "submit";
			submit.innerText = "CONFIRM BOOKING";
			
			// mix it all up
			form.appendChild(dropoff);
			form.appendChild(submit);
			
			return form;
		},
		
		bookingConfirmed: function() {
			var container = document.createElement("div");
			var header = document.createElement("h3");
			var message = document.createElement("p");
			
			header.innerText = "Booking Confirmed!";
			message.innerText = "Make sure that you leave your car at the drop-off spot before the end of your rental.";
			
			container.appendChild(header);
			container.appendChild(message);
			
			return container;
		},
		
		infoWindow: function(vehicle, callback) {
			var infoContents = document.createElement("div");
			var bookBtn = document.createElement("button");
			var vehicleInfo = this.vehicleInfo(vehicle);
			
			bookBtn.className = "confirm";
			// disable button if car unavailable
			if (vehicle.available) {
				bookBtn.innerText = "BOOK NOW";
				bookBtn.addEventListener("click", callback);
			} else {
				bookBtn.innerText = "NOT AVAILABLE";
				bookBtn.disabled = true;
			}
			infoContents.className = "map-info";
			infoContents.appendChild(vehicleInfo);
			infoContents.appendChild(bookBtn);
			
			return infoContents;
		}
		
	}
	
})();
