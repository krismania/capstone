var map;

// list of vehicles currently being displayed on the map
// map markers are stored in vehicles[i].marker
var mapVehicles = [];
// currently booked vehicle if applicable, marker is stored as above
var bookedVehicle = null;

var urlAvail = '/img/vehicle-pin-available.png';
var urlUnavail = '/img/vehicle-pin-unavailable.png';
var urlBooked = '/img/vehicle-pin-booked.png';

// keep track of the currently open info window
var currentInfoWindow = null;
// keep track of which button is currently visible
var nearbyButton = true;
// the marker which represents the user's location
var geoMarker = null;
// signed in user
var googleUser = null;

function onLogin(user) {
	// post the client ID to the servera
	console.log("Token: ", user.getAuthResponse().id_token);
	var headers = new Headers();
	headers.append("Content-Type", "application/json");
	var request = new Request("/login", {
		method: 'post',
		headers: headers,
		body: JSON.stringify({
			id: user.getAuthResponse().id_token
		})
	});
	fetch(request)
	.then(res => {
		googleUser = user;
	    console.log('Logged in as: ' + googleUser.getBasicProfile().getName());
	    var id_token = googleUser.getAuthResponse().id_token;
	    // show logout button
	    document.getElementById("header-links").style.visibility = 'visible';
	    // fire event
	    document.dispatchEvent(new Event("login"));
	});
}

function signOut() {
	// kill the session
	fetch(new Request("/logout"))
	.then(res => {
		// sign out on client side
		gapi.auth2.getAuthInstance().signOut().then(function () {
			googleUser = null;
			console.log('User signed out.');
			// hide logout button
		    document.getElementById("header-links").style.visibility = 'hidden';
		    // fire event
		    document.dispatchEvent(new Event("logout"));
		});
	});
}

function showNearbyButton() {
	if (!nearbyButton) {
		var button = document.getElementById("geo-button");
		button.innerHTML = 'NEARBY CARS';
		button.removeEventListener('click', geolocateHandler)
		button.addEventListener('click', nearbyHandler)
		nearbyButton = true;
	}
}

function showGeoButton() {
	if (nearbyButton) {
		var button = document.getElementById("geo-button");
		button.innerHTML = '<i class="material-icons md-18">my_location</i>FIND ME';
		button.removeEventListener('click', nearbyHandler)
		button.addEventListener('click', geolocateHandler)
		nearbyButton = false;
	}
}

function geolocateHandler(e) {
	e.preventDefault();
	map.panTo(geoMarker.marker.getPosition());
	showNearbyButton();
}

function nearbyHandler(e) {
	var pos = geoMarker.marker.getPosition()
	nearbyCars(pos);
}

function initMap() {
	map = new google.maps.Map(document.getElementById('map'), {
		center: new google.maps.LatLng(-37.813985, 144.960235),
		zoom: 15,
		disableDefaultUI: true
	});
	
	map.setOptions({styles: [
		{
			featureType: 'poi.business',
			stylers: [{visibility: 'off'}]
		}
	]});
	
	// listener which resets the 'geolocate' button on pan
	map.addListener('center_changed', () => {
		showGeoButton();
	});
	
	// draw user's location
	navigator.geolocation.watchPosition(pos => {
		displayLocation(pos);
	}, console.error, {enableHighAccuracy: true});
	
	// fetch & display vehicles
	rebu.getVehicles(displayVehicles);
	
	// check if the user has a booking currently
	displayCurrentBooking();
}

function displayVehicles(vehicles) {
	// clear old markers
	for (var i = 0; i < mapVehicles.length; i++) {
		mapVehicles[i].marker.setMap(null);
	}
	// display new markers
	for (var i = 0; i < vehicles.length; i++) {
		vehicles[i].marker = createVehicleMarker(vehicles[i], map);
	};
	mapVehicles = vehicles;
}

function displayLocation(pos) {
	p = new google.maps.LatLng(pos.coords.latitude, pos.coords.longitude);
	acc = pos.coords.accuracy
	if (geoMarker) {
		geoMarker.marker.setPosition(p);
		geoMarker.circle.setCenter(p);
		geoMarker.circle.setRadius(acc);
	} else {
		geoMarker = {
			marker: new google.maps.Marker({
				position: p,
				map: map,
				icon: {
					url: "/img/geo-dot.png",
					size: new google.maps.Size(14, 14),
					anchor: new google.maps.Point(7, 7)
				}
			}),
			circle: new google.maps.Circle({
				fillColor: "#F44336",
				fillOpacity: 0.25,
				strokeWeight: 0,
				map: map,
				center: p,
				radius: acc
			})
		};
	}
}

function createVehicleMarker(vehicle, map, booked = false) {
	var marker = new google.maps.Marker({
		position: vehicle.position,
		map: map,
		icon: {
			url: booked ? urlBooked : (vehicle.available ? urlAvail : urlUnavail),
			size: new google.maps.Size(40, 40),
			origin: new google.maps.Point(0, 0),
			anchor: new google.maps.Point(20, 40)
		},
		title: vehicle.registration
	});
	
	if (!booked) {
		marker.addListener('click', function() {
			console.log("Clicked on marker for", vehicle)
			// close the currently opened window
			if (currentInfoWindow) currentInfoWindow.close();
			
			// create info window & open it
			var content = view.infoWindow(vehicle, function(e) {
				e.preventDefault();
				bookingForm(vehicle);
			});
			var info = new google.maps.InfoWindow({content: content});
			info.open(map, marker);
			
			// update the current window var
			currentInfoWindow = info;
		});
	}
	
	return marker;
}

function bookingForm(vehicle) {
	console.log("Getting booking form for", vehicle)
	// close the current info window
	if (currentInfoWindow) {
		currentInfoWindow.close();
		currentInfoWindow = null;
	}
	// create the form
	var vehicleInfo = view.vehicleInfo(vehicle);
	var bookingForm = view.bookingForm(vehicle);
	bookingForm.addEventListener("submit", function(e) {
		e.preventDefault();
		submitBooking(vehicle);
	});
	
	sidepane.clear();
	sidepane.appendHeader("BOOK YOUR CAR");
	sidepane.append(vehicleInfo);
	sidepane.append(bookingForm);
	sidepane.open();
}

function submitBooking(vehicle) {	
	// collect booking details
	if (googleUser != null){
		var form = document.getElementById("booking-form");
		var timeSelect = document.getElementById("dropoff-time");
		var duration = timeSelect.options[timeSelect.selectedIndex].value;
		var registration = document.getElementById("registration").value;
		
		var bookingRequest = {
			registration: registration,
			duration: duration,
			client: googleUser.getBasicProfile().getEmail()
		};
		
		rebu.requestBooking(bookingRequest, function(succeeded) {
			if (succeeded) {
				// show the confirmation screen
				var vehicleInfo = view.vehicleInfo(vehicle);
				sidepane.clear();
				sidepane.appendHeader("BOOK YOUR CAR");
				sidepane.append(vehicleInfo);
				sidepane.append(view.bookingConfirmed());
				// refresh the map
				rebu.getVehicles(displayVehicles);
			} else {
				alert("Booking failed");
			}
		});
	} else {
		alert("You are not logged in! Please login before making a booking.");
	}

}

function nearbyCars(pos) {
	// fetch nearby cars
	rebu.getNearby(pos, function(nearby) {
		// show the response
		sidepane.clear();
		sidepane.appendHeader("NEARBY CARS");
		for (var i = 0; i < nearby.length; i++) {
			let vehicle = nearby[i];
			console.log(vehicle);
			var nearbyVehicle = view.nearbyVehicle(vehicle, function(e) {
				e.preventDefault();
				bookingForm(vehicle);
			});
			sidepane.append(nearbyVehicle);
		}
		sidepane.open();
	});
}

// Queries for the user's current booking & displays it as a card
function displayCurrentBooking() {
	rebu.getCurrentBooking(function(booking) {
		// create vehicle marker
		bookedVehicle = booking.vehicle;
		bookedVehicle.marker = createVehicleMarker(bookedVehicle, map, true);
		// callback for "find car" button
		var findCallback = function(booking) {
			map.panTo(bookedVehicle.marker.getPosition());
			// map.setZoom(18); // this doesn't work very well
		}
		// display the card
		document.body.appendChild(view.currentBookingCard(booking, findCallback));
	});
}

// removes the current booking card
function removeCurrentBooking() {
	var currentBooking = document.getElementById("current-booking");
	if (currentBooking) {
		document.body.removeChild(currentBooking);
	}
}

// initialize sidepane
sidepane.setOpenCallback(function() {
	document.getElementById('sidepane').style.width = null;
	document.getElementById('map-wrapper').style.left = '360px';
});
sidepane.setCloseCallback(function() {
	document.getElementById('sidepane').style.width = '0';
	document.getElementById('map-wrapper').style.left = null;
});

// refresh the map automatically every 60 seconds
setInterval(function() {
	rebu.getVehicles(displayVehicles);
}, 60000);
