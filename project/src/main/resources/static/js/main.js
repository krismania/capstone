var map;

var urlAvail = '/img/vehicle-pin-available.png';
var urlUnavail = '/img/vehicle-pin-unavailable.png';
var allMarkers = [];

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
        findNearestCar(userLat, userLong);
}

//https://www.htmlgoodies.com/beyond/javascript/calculate-the-distance-between-two-points-in-your-web-apps.html
//this function was taken from the website above.
function haversineFormula(lat1, lon1, lat2, lon2)
{
	var radlat1 = Math.PI * lat1/180
    var radlat2 = Math.PI * lat2/180
    var radlon1 = Math.PI * lon1/180
    var radlon2 = Math.PI * lon2/180
    var theta = lon1-lon2
    var radtheta = Math.PI * theta/180
    var dist = Math.sin(radlat1) * Math.sin(radlat2) + Math.cos(radlat1) * Math.cos(radlat2) * Math.cos(radtheta);
    dist = Math.acos(dist)
    dist = dist * 180/Math.PI
    dist = dist * 60 * 1.1515
    dist = dist * 1.609344
    return dist
}

function findNearestCar(userLat, userLong)
{
        var i;
        var minDist;
        var nearestCar;
        var markerLat;
        var markerLong;
        
        for (i = 0 ; i < allMarkers.length ; i += 1)
        {
        	markerLat = allMarkers[i].getPosition().lat();
        	markerLong = allMarkers[i].getPosition().lng();
            
        	var d = haversineFormula(userLat, markerLat, userLong, markerLong);
        	
            if (i == 0)
            {
                  minDist = d;
                  nearestCar = allMarkers[i].getTitle();
            }
            else
            {
                  if(d < minDist)
                  {
                       minDist = d;
                       nearestCar = allMarkers[i].getTitle();
                  }
            }
        }
        alert('The nearest marker is: ' + nearestCar); 
}

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
	allMarkers.push(marker);
}

initSearch();

