var adminView = (function() {
	
	return {
		
		// creates a menu using the passed in list of buttons
		menu: function(items) {
			var menu = document.createElement("ul");
			menu.className = "menu";
			// add list elements for each item
			for (var i = 0; i < items.length; i++) {
				var li = document.createElement("li");
				li.appendChild(items[i]);
				menu.appendChild(li);
			}
			return menu;
		},
		
		console: function(addVehicleCallback, manageUserCallback) {
			var container = document.createElement("div");
			var addVehicleBtn = document.createElement("button");
			var manageUserBtn = document.createElement("button");
			var hint = document.createElement("p");
			
			hint.innerText = "Tip: Click on a vehicle to view details about it";
			hint.className = "hint";
			
			addVehicleBtn.innerText = "Add New Vehicle";
			addVehicleBtn.addEventListener('click', addVehicleCallback);
			manageUserBtn.innerText = "Manage User";
			manageUserBtn.addEventListener('click', manageUserCallback);
			
			var adminMenu = this.menu([addVehicleBtn, manageUserBtn]);
			
			container.appendChild(adminMenu);
			container.appendChild(hint);
			
			return container;
		},
		
		vehicleForm: function(createCallback) {
			var form = document.createElement("form");
			form.id = "vehicle-form";
			
			// vehicle details fieldset
			var details = document.createElement("fieldset");
			var detailsLegend = document.createElement("legend");
			detailsLegend.innerText = "Vehicle Details";
			details.appendChild(detailsLegend);
			// vehicle status fieldset
			var status = document.createElement("fieldset");
			var statusLegend = document.createElement("legend");
			statusLegend.innerText = "Current Location & Status";
			status.appendChild(statusLegend);
			
			var rego = document.createElement("input");
			rego.id = "registration";
			rego.placeholder = "Registration";
			var year = document.createElement("input");
			year.id = "year";
			year.placeholder = "Year";
			var make = document.createElement("input");
			make.id = "make";
			make.placeholder = "Make";
			var model = document.createElement("input");
			model.id = "model";
			model.placeholder = "Model";
			var colour = document.createElement("input");
			colour.id = "colour";
			colour.placeholder = "Colour";
			var lat = document.createElement("input");
			lat.id = "current-lat";
			lat.placeholder = "Latitude";
			var lng = document.createElement("input");
			lng.id = "current-lng";
			lng.placeholder = "Longitude";
			var active = document.createElement("select");
			active.id = "active";
			
			// add options to availability selector
			var activeTrue = document.createElement("option");
			activeTrue.innerText = "Active";
			activeTrue.value = "true";
			activeTrue.selected = true;
			var activeFalse = document.createElement("option");
			activeFalse.innerText = "Inactive";
			activeFalse.value = "false";
			active.appendChild(activeTrue);
			active.appendChild(activeFalse);
			
			var submit = document.createElement("button");
			submit.addEventListener("click", function(e) {
				e.preventDefault();
				createCallback();
			});
			submit.innerText = "SAVE VEHICLE";
			submit.className = "confirm";
			submit.type = "submit";
			
			details.appendChild(rego);
			details.appendChild(year);
			details.appendChild(make);
			details.appendChild(model);
			details.appendChild(colour);
			
			status.appendChild(lat);
			status.appendChild(lng);
			status.appendChild(active);
			
			form.appendChild(details);
			form.appendChild(status);
			form.appendChild(submit);
			
			return form;
		},
		
		manageUserForm: function(requestCallback) {
			var form = document.createElement("form");
			form.className = "searchbox";
						
			var email = document.createElement("input");
			email.id = "email";
			email.type = "email";
			email.placeholder = "Email Address";
			
			form.appendChild(email);
			
			var searchIcon = document.createElement("i");
			searchIcon.innerText = "search";
			searchIcon.className = "material-icons";
			
			var search = document.createElement("button");
			search.addEventListener("click", function(e) {
				e.preventDefault();
				requestCallback();
			});
			search.type = "submit";
			search.appendChild(searchIcon);
			
			form.appendChild(search);
			
			return form;
		},
		
		bookingList: function(bookings) {
			console.log(bookings);
			if (bookings.length == 0) {
				var p = document.createElement("p");
				p.className = "hint";
				p.innerText = "No bookings found for this user";
				return p;
			} else {
				var bookingBtns = new Array(bookings.length);
				for (var i = 0; i < bookings.length; i++) {
					// closure prevents incorrect listener values
					(function() {
						var booking = bookings[i]
						bookingBtns[i] = document.createElement("button");
						bookingBtns[i].addEventListener("click", function() {
							console.log(booking);
						});
						bookingBtns[i].innerText = view.jsonDateToString(booking.timestamp) + " (" + booking.vehicle.registration + ")";
					})();
				}
				var menu = adminView.menu(bookingBtns);
				var heading = document.createElement("h3");
				heading.innerText = "Past Bookings";
				return menu;
			}
		}
		
	}
	
})();

function mainMenu() {
	sidepane.clear();
	sidepane.appendHeader("ADMIN CONSOLE");
	sidepane.append(adminView.console(addVehicle, manageUser))
}

function addVehicle() {
	sidepane.clear();
	sidepane.appendHeader("ADD VEHICLE", function() {
		mainMenu();
	});
	sidepane.append(adminView.vehicleForm(function() {
		var vehicle = {
			registration: document.getElementById("registration").value,
			year: parseInt(document.getElementById("year").value),
			make: document.getElementById("make").value,
			model: document.getElementById("model").value,
			colour: document.getElementById("colour").value,
			position: {
				lat: parseFloat(document.getElementById("current-lat").value),
				lng: parseFloat(document.getElementById("current-lng").value)
			},
			status: document.getElementById("active").selectedIndex == 0 ? "active" : "inactive"
		};
		console.log("Creating vehicle", vehicle);
		var headers = new Headers();
		headers.append("Content-Type", "application/json");
		var request = new Request("/admin/api/vehicles", {
			method: "POST",
			headers: headers,
			body: JSON.stringify(vehicle)
		});
		
		fetch(request)
		.then(res => {
			if (res.ok) {
				alert("Vehicle created.");
				window.location.reload();
			}
		});
	}));
}

function editVehicle(vehicle) {
	sidepane.clear();
	sidepane.appendHeader("EDIT VEHICLE", function() {
		mainMenu();
	});
	sidepane.append(adminView.vehicleForm(function() {
		var vehicle = {
			registration: document.getElementById("registration").value,
			year: parseInt(document.getElementById("year").value),
			make: document.getElementById("make").value,
			model: document.getElementById("model").value,
			colour: document.getElementById("colour").value,
			position: {
				lat: parseFloat(document.getElementById("current-lat").value),
				lng: parseFloat(document.getElementById("current-lng").value)
			},
			active: (document.getElementById("active").selectedIndex == 0)
		};
		console.log("Creating vehicle", vehicle);
		var headers = new Headers();
		headers.append("Content-Type", "application/json");
		var request = new Request("/admin/api/vehicles", {
			method: "POST",
			headers: headers,
			body: JSON.stringify(vehicle)
		});
		
		fetch(request)
		.then(res => {
			if (res.ok) {
				alert("Vehicle created.");
			}
		});
	}));
	// populate the fields
	document.getElementById("registration").value = vehicle.registration;
	document.getElementById("year").value = vehicle.year;
	document.getElementById("make").value = vehicle.make;
	document.getElementById("model").value = vehicle.model;
	document.getElementById("colour").value = vehicle.colour;
}

function manageUser() {
	sidepane.clear();
	sidepane.appendHeader("MANAGE USER", function() {
		mainMenu();
	});
	sidepane.append(adminView.manageUserForm(function() {
		var email = document.getElementById("email").value;
		var request = new Request("/admin/api/bookings/" + email) // TODO: currently takes user ID, not email
		fetch(request)
		.then(res => res.json())
		.then(bookings => sidepane.append(adminView.bookingList(bookings)));
	}));
}

// set admin status
rebu.setAdmin(true);
mainMenu();
