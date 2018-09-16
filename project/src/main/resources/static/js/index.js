document.addEventListener("login", function() {
    // check if the newly logged in user has a booking
    displayCurrentBooking()
});

document.addEventListener("logout", function() {
    // hide current booking if it was displayed
    removeCurrentBooking();
});

// Display the geolocate button initially
showGeoButton();
