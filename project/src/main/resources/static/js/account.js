document.addEventListener("login", function() {
	// get past bookings
	var user = googleUser.getBasicProfile().getEmail()
	rebu.getBookings(user, function(bookings) {
		console.log(bookings);
		var bookingsDiv = document.getElementById("prev-bookings");
		for (var i = bookings.length-1; i >=0 ; i--) {
			// TODO: server may need to send this as a seperate field
			bookings[i].vehicle.description = bookings[i].vehicle.make + " "
				+ bookings[i].vehicle.model
				+ " (" + bookings[i].vehicle.year + ")";
			bookingsDiv.appendChild(view.previousBooking(bookings[i]));
		}
	});
});
