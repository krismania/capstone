var map;
function initMap() {
	navigator.geolocation.getCurrentPosition(pos => {
		map = new google.maps.Map(document.getElementById('map'), {
			center: {lat: pos.coords.latitude, lng: pos.coords.longitude},
			zoom: 12
		});
		
		var request = new Request('/api/vehicles');
		fetch(request)
		.then(res => res.json())
		.then(json => {
			for (var i = 0; i < json.length; i++) {
				new google.maps.Marker({
					position: json[i].position,
					map: map,
					title: json[i].registration
				})
			}
		});
	});
}
