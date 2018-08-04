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
			var marker = new google.maps.Marker({
				position: {
					lat: json[0].position.lat,
					lng: json[0].position.lon
				},
				map: map,
				title: json[0].registration
			});
			console.log(marker);
		});
	});
}
