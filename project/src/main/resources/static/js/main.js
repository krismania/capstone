var map;

var urlAvail = '/img/vehicle-pin-available.png';
var urlUnavail = '/img/vehicle-pin-unavailable.png';

function initMap() {
	navigator.geolocation.getCurrentPosition(pos => {
		map = new google.maps.Map(document.getElementById('map'), {
			center: {lat: pos.coords.latitude, lng: pos.coords.longitude},
			zoom: 12,
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
