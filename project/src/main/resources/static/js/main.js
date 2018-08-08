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

function userLocation()
{
        if (navigator.geolocation)
        {
            navigator.geolocation.getCurrentPosition(addUserMarker, geoErrors);
        }
        else
        {
            alert("Geolocation is not supported by this browser.");
        }
}

function addUserMarker(position)
{
        var userLat = position.coords.latitude;
        var userLong = position.coords.longitude;
        var userLocation = {lat: userLat, lng: userLong};
        var marker = new google.maps.Marker({position: userLocation, map: map});
        findNearestCar(userLocation);
}

/*function findNearestCar(userLocation)
{
		var map = new google.maps.Map(document.getElementById('map'));
        var objects = map.getObjects();
        var objectAmt = map.getObjects().length;
        var i;
        var distance;
        var minDist;
        var nearestCar;
        
        for (i = 0 ; i < objectAmt ; i += 1)
        {
            distance = objects[i].getPosition().distance(userLocation);
            if (i == 0)
            {
                  minDist = distance;
            }
            else
            {
                  if(minDist > distance)
                  {
                       minDist = distance;
                       nearestCar = objects[i].getData();
                  }
            }
        }
        alert('The nearest marker is: ' + nearestCar); 
}
*/
function geoErrors(error) {
    switch(error.code) {
        case error.PERMISSION_DENIED:
            alert("User denied the request for Geolocation.");
            break;
        case error.POSITION_UNAVAILABLE:
            alert("Location information is unavailable.");
            break;
        case error.TIMEOUT:
            alert("The request to get user location timed out.");
            break;
        case error.UNKNOWN_ERROR:
            alert("An unknown error occurred.");
            break;
    }
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

initSearch();

