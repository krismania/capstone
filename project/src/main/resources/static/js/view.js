// contains JS blueprints for dynamic views
var view = (function() {
	
	return {
		
		createNearbyVehicleElement: function(vehicle) {
			var container = document.createElement("div");
			
			// create vehicle info
			var vehicleInfo = document.createElement("div");
			var desc = document.createElement("h3");
			var colour = document.createElement("p");
			var rego = rego = document.createElement("p");
			
			container.className = "nearby-info";
			vehicleInfo.className = "vehicle-info";
			
			desc.innerText = vehicle.description;
			colour.innerText = "Colour: " + vehicle.colour;
			rego.innerText = "Registration: " + vehicle.registration;
			
			vehicleInfo.appendChild(desc);
			vehicleInfo.appendChild(colour);
			vehicleInfo.appendChild(rego);
			
			// create book button
			var bookButtonContainer = document.createElement("div");
			var bookButton = document.createElement("button");
			var distance = document.createElement("p");
			
			bookButtonContainer.className = "book-container";
			bookButton.innerText = "BOOK";
			distance.innerText = vehicle.distance;
			
			bookButtonContainer.appendChild(distance);
			bookButtonContainer.appendChild(bookButton);
			
			// create listener for book button
			bookButton.addEventListener('click', () => {
				bookingForm(vehicle.registration);
			});
			
			// append to the container & return
			container.appendChild(vehicleInfo);
			container.appendChild(bookButtonContainer)
			return container;
		}
		
	}
	
})();
