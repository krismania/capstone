var map;

var urlAvail = '/img/vehicle-pin-available.png';
var urlUnavail = '/img/vehicle-pin-unavailable.png';

// keep track of the currently open info window
var currentInfoWindow = null;

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
	var button = document.getElementById("geo-button");
	button.innerHTML = 'NEARBY CARS';
	button.removeEventListener('click', geolocateHandler)
	button.addEventListener('click', nearbyHandler)
}

function showGeoButton() {
	var button = document.getElementById("geo-button");
	button.innerHTML = '<i class="material-icons md-18">my_location</i>FIND ME';
	button.removeEventListener('click', nearbyHandler)
	button.addEventListener('click', geolocateHandler)
}

function geolocateHandler(e) {
	e.preventDefault();
	navigator.geolocation.getCurrentPosition(pos => {
		map.panTo(new google.maps.LatLng(pos.coords.latitude, pos.coords.longitude));
	});
}

function nearbyHandler(e) {
	console.log("Nearby Cars");
}

function initMap() {
	navigator.geolocation.getCurrentPosition(pos => {
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
		
		var request = new Request('/api/vehicles');
		fetch(request)
		.then(res => res.json())
		.then(json => {
			for (var i = 0; i < json.length; i++) {
				addMarker(json[i], map);
			};
		});
	});
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
	currentInfoWindow.close();
	currentInfoWindow = null;
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

// Display the geolocate button initially
showGeoButton();
