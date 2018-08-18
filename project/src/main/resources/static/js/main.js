var map;

var urlAvail = '/img/vehicle-pin-available.png';
var urlUnavail = '/img/vehicle-pin-unavailable.png';

function initSearch() {
	document.getElementById("geo-button").addEventListener('click', (e) => {
		console.log(e)
		e.preventDefault();
		navigator.geolocation.getCurrentPosition(pos => {
			map.panTo(new google.maps.LatLng(pos.coords.latitude, pos.coords.longitude));
		});
	});
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
		
	var info = new google.maps.InfoWindow({
		content: vehicle.registration + '<br/>' + vehicle.color + ' ' + 
			vehicle.make + ' ' + vehicle.model + ' (' + vehicle.year + ')'
	});
		
	marker.addListener('click', () => {info.open(map, marker)});
}

function openSidepane() {
	document.getElementById('sidepane').style.width = null
}

function closeSidepane() {
	document.getElementById('sidepane').style.width = '0'
}

function bookingForm() {
	request = new Request('/html/book.html')
	fetch(request)
	.then(res => res.text())
	.then(html => {
		document.getElementById('sidepane-content').innerHTML = html
		openSidepane()
	});
}

initSearch();
