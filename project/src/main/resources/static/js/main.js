var map;

var urlAvail = '/img/vehicle-pin-available.png';
var urlUnavail = '/img/vehicle-pin-unavailable.png';

// keep track of the currently open info window
var currentInfoWindow = null;
// keep track of which button is currently visible
var nearbyButton = true;
// the marker which represents the user's location
var geoMarker = null;

function onSuccess(googleUser) {
    console.log('Logged in as: ' + googleUser.getBasicProfile().getName());
    
    var id_token = googleUser.getAuthResponse().id_token;
    console.log("ID Token: " + id_token);
    alert(id_token);
}

function signOut() {
	var auth2 = gapi.auth2.getAuthInstance();
	auth2.signOut().then(function () {
		console.log('User signed out.');
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
	pos = geoMarker.marker.getPosition()
	nearbyCars(pos.lat, pos.lng);
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
	
	var request = new Request('/api/vehicles');
	fetch(request)
	.then(res => res.json())
	.then(json => {
		for (var i = 0; i < json.length; i++) {
			addMarker(json[i], map);
		};
	});
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
				fillColor: "#687BF1",
				fillOpacity: 0.2,
				strokeWeight: 0,
				map: map,
				center: p,
				radius: acc
			})
		};
	}
}

function addMarker(vehicle, map) {
	console.log("Adding marker for " + vehicle.registration);
	
	var marker = new google.maps.Marker({
		position: vehicle.position,
		map: map,
		icon: {
			url: vehicle.available ? urlAvail : urlUnavail,
			size: new google.maps.Size(40, 40),
			origin: new google.maps.Point(0, 0),
			anchor: new google.maps.Point(20, 40)
		},
		title: vehicle.registration
	});
		
	marker.addListener('click', function() {
		console.log("Clicked on marker for", vehicle)
		// close the currently opened window
		if (currentInfoWindow) currentInfoWindow.close();
		
		// add the required fields to vehicle
		vehicle.colour = vehicle.color;
		vehicle.description = vehicle.make + " " + vehicle.model + " (" + vehicle.year + ")";
		
		// create info window & open it
		content = view.infoWindow(vehicle, function(e) {
			e.preventDefault();
			bookingForm(vehicle.registration);
		});
		info = new google.maps.InfoWindow({content: content});
		info.open(map, marker);
		
		// update the current window var
		currentInfoWindow = info;
	});
}

function bookingForm(registration) {
	console.log("Getting booking form for " + registration)
	// close the current info window
	if (currentInfoWindow) {
		currentInfoWindow.close();
		currentInfoWindow = null;
	}
	// get the vehicle info & create the form
	vehicle = {registration: "QRB990", description: "BMW 325i (2003)", colour: "Black"};
	vehicleInfo = view.vehicleInfo(vehicle);
	bookingForm = view.bookingForm(vehicle);
	bookingForm.addEventListener("submit", submitBooking);
	
	sidepane.clear();
	sidepane.appendHeader("BOOK YOUR CAR");
	sidepane.append(vehicleInfo);
	sidepane.append(bookingForm);
	sidepane.open();
}

function submitBooking(e) {
	// prevent the default form action
	e.preventDefault();
	
	// dummy vehicle info
	vehicle = {registration: "QRB990", description: "BMW 325i (2003)", colour: "Black"};
	vehicleInfo = view.vehicleInfo(vehicle);
	
	console.log("Submitting booking form")
	
	// show the confirmation screen
	sidepane.clear();
	sidepane.appendHeader("BOOK YOUR CAR");
	sidepane.append(vehicleInfo);
	sidepane.append(view.bookingConfirmed());
}

function nearbyCars(lat, lng) {
	// fetch nearby cars json
	// this is a simulated response
	nearby = [
		{registration: "QRB990", description: "BMW 325i (2003)", colour: "Black", distance: "500 m"},
		{registration: "JTD955", description: "Holden Commodore (2005)", colour: "Grey", distance: "800 m"},
		{registration: "FOK356", description: "Holden Barina (2017)", colour: "White", distance: "1.2 km"},
		{registration: "QOP299", description: "Kia Rio (2013)", colour: "Pink", distance: "1.8 km"},
		{registration: "YODUDE", description: "Nissan Skyline (2010)", colour: "Black", distance: "2.1 km"},
	];
	// show the response
	sidepane.clear();
	sidepane.appendHeader("NEARBY CARS");
	for (var i = 0; i < nearby.length; i++) {
		sidepane.append(view.nearbyVehicle(nearby[i]));
	}
	sidepane.open();
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

// Display the geolocate button initially
showGeoButton();
