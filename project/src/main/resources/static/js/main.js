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
	navigator.geolocation.getCurrentPosition(pos => {
		map.panTo(new google.maps.LatLng(pos.coords.latitude, pos.coords.longitude));
		// display the 'nearby cars' button
		showNearbyButton();
	});
}

function nearbyHandler(e) {
	navigator.geolocation.getCurrentPosition(pos => {
		nearbyCars(pos.coords.latitude, pos.coords.longitude);
	});
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
		
	marker.addListener('click', () => {
		console.log("Clicked on marker for " + vehicle.registration)
		// close the currently opened window
		if (currentInfoWindow) currentInfoWindow.close();
		getInfoFor(vehicle.registration, (html) => {
			// fetch the vehicle info & display
			info = new google.maps.InfoWindow({content: html});
			info.open(map, marker);
			// update the current window var
			currentInfoWindow = info;
		});
	});
}

function openSidepane() {
	document.getElementById('sidepane').style.width = null;
	document.getElementById('map-wrapper').style.left = '360px';
}

function closeSidepane() {
	document.getElementById('sidepane').style.width = '0';
	document.getElementById('map-wrapper').style.left = null;
}

function getInfoFor(registration, callback) {
	console.log("Getting info for " + registration)
	// TODO: move these static pages into a controller so they can send back
	// customized views of the requested info
	request = new Request('/html/vehicle-info.html')
	fetch(request)
	.then(res => res.text())
	.then(html => callback(html))
}

function bookingForm(registration) {
	console.log("Getting booking form for " + registration)
	// close the current info window
	if (currentInfoWindow) {
		currentInfoWindow.close();
		currentInfoWindow = null;
	}
	// TODO: see above
	request = new Request('/html/book.html')
	fetch(request)
	.then(res => res.text())
	.then(html => {
		document.getElementById('sidepane-content').innerHTML = html
		openSidepane()
	});
}

function submitBooking(e) {
	// prevent the default form action
	e.preventDefault();
	console.log("Submitting booking form")
	// get the confirmation screen
	request = new Request('/html/confirmed.html')
	fetch(request)
	.then(res => res.text())
	.then(html => {
		document.getElementById('sidepane-content').innerHTML = html
	});
}

function createNearbyVehicleElement(vehicle) {
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

function nearbyCars(lat, lng) {
	console.log("Getting nearby cars: ", lat, lng);
	// get nearby cars screen
	request = new Request('/html/nearby.html')
	fetch(request)
	.then(res => res.text())
	.then(html => {
		sidepane = document.getElementById('sidepane-content');
		sidepane.innerHTML = html;
		// fetch nearby cars json
		nearby = [
			{registration: "QRB990", description: "BMW 325i (2003)", colour: "Black", distance: "500 m"},
			{registration: "JTD955", description: "Holden Commodore (2005)", colour: "Grey", distance: "800 m"},
			{registration: "FOK356", description: "Holden Barina (2017)", colour: "White", distance: "1.2 km"},
			{registration: "QOP299", description: "Kia Rio (2013)", colour: "Pink", distance: "1.8 km"},
			{registration: "YODUDE", description: "Nissan Skyline (2010)", colour: "Black", distance: "2.1 km"},
		];
		for (var i = 0; i < nearby.length; i++) {
			sidepane.appendChild(createNearbyVehicleElement(nearby[i]));
		}
		openSidepane();
	});
}

// Display the geolocate button initially
showGeoButton();
