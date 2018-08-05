var map;
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
			var iconAvailable = {
				url: '/img/vehicle-pin-available.png',
				size: new google.maps.Size(40, 40),
				origin: new google.maps.Point(0, 0),
				anchor: new google.maps.Point(40, 40)
			};
			var iconUnavailable = {
				url: '/img/vehicle-pin-unavailable.png',
				size: new google.maps.Size(40, 40),
				origin: new google.maps.Point(0, 0),
				anchor: new google.maps.Point(20, 40)
			};
			for (var i = 0; i < json.length; i++) {
				console.log(json[i]);
				new google.maps.Marker({
					position: json[i].position,
					map: map,
					icon: json[i].available ? iconAvailable : iconUnavailable,
					title: json[i].registration
				});
			};
		});
	});
}
