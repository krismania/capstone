package model;

import java.io.Closeable;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.appengine.api.utils.SystemProperty;

import util.Config;
import util.Util;

public class Database implements Closeable {

    final static Logger logger = LoggerFactory.getLogger(Database.class);

    private Connection conn;

    /**
     * Create a database object with an underlying {@link java.sql.Connection}
     * object. This constructor will return a connection to the local
     * development database when run locally, or a connection to the Cloud SQL
     * database when deployed.
     *
     * @throws SQLException
     */
    public Database() {
	try {
	    if (SystemProperty.environment.value() == SystemProperty.Environment.Value.Production) {
		String instance = Config.get("cloudSqlInstance");
		String database = Config.get("cloudSqlDatabase");
		String username = Config.get("cloudSqlUsername");
		String password = Config.get("cloudSqlPassword");

		String url = "jdbc:mysql://google/" + database + "?useSSL=false&cloudSqlInstance=" + instance
			+ "&socketFactory=com.google.cloud.sql.mysql.SocketFactory" + "&user=" + username + "&password="
			+ password;

		logger.info("Connecting to production database");
		this.conn = DriverManager.getConnection(url);
	    } else {
		String database = Config.get("localSqlDatabase");
		String username = Config.get("localSqlUsername");
		String password = Config.get("localSqlPassword");
		String url = "jdbc:mysql://localhost:3306/" + database + "?useSSL=false"
			+ "&serverTimezone=Australia/Melbourne";
		logger.info("Connecting to development database: " + url);
		this.conn = DriverManager.getConnection(url, username, password);
	    }

	    // initialize database
	    initDatabase();
	} catch (Exception e) {
	    // shutdown immediately in the case of an SQL error
	    logger.error(e.getMessage());
	    System.exit(1);
	}
    }

    /**
     * Initialize the database with the required tables
     *
     * @throws SQLException
     */
    private void initDatabase() throws SQLException {
	logger.info("Initializing the database");
	String vehiclesSql = "CREATE TABLE IF NOT EXISTS `vehicles` (" + "`registration` VARCHAR(10) NOT NULL, "
		+ "`make` VARCHAR(50) NOT NULL, " + "`model` VARCHAR(50) NOT NULL, "
		+ "`year` SMALLINT UNSIGNED NOT NULL, " + "`colour` VARCHAR(50) NOT NULL, "
		+ "location POINT NOT NULL, active TINYINT UNSIGNED NOT NULL, " + "PRIMARY KEY (`registration`));";

	String bookingsSql = "CREATE TABLE IF NOT EXISTS `bookings` (" + "`id` INT NOT NULL AUTO_INCREMENT, "
		+ "`timestamp` DATETIME NOT NULL, " + "`registration` VARCHAR(10) NOT NULL, "
		+ "`customer_id` VARCHAR(50) NOT NULL, " + "`duration` SMALLINT UNSIGNED NOT NULL, "
		+ "`start_location` POINT NOT NULL, " + "`end_location` POINT NOT NULL, " + "PRIMARY KEY (`id`), "
		+ "FOREIGN KEY (`registration`) REFERENCES `vehicles`(`registration`));";

	String admin = "CREATE TABLE IF NOT EXISTS `admins` (" + "`admin_id` VARCHAR(50) NOT NULL, "
		+ "PRIMARY KEY (`admin_id`));";

	Statement stmt = this.conn.createStatement();
	stmt.execute(vehiclesSql);
	stmt.execute(bookingsSql);
	stmt.execute(admin);
	stmt.close();
    }

    /**
     * Close the underlying database connection
     *
     * @throws SQLException
     */
    @Override
    public void close() {
	try {
	    logger.info("Closing the database");
	    this.conn.close();
	} catch (SQLException e) {
	    logger.error("Failed to close DB");
	    logger.error(e.getMessage());
	}
    }

    public Vehicle insertVehicle(String registration, String make, String model, int year, String colour,
	    Position position, int active) {

	logger.info("Insert Vehicles");
	Vehicle v = null;
	try {
	    String query = "INSERT INTO vehicles (registration, make, model,year,colour,location,active) VALUES (?,?,?,?,?, POINT(?,?),?);";
	    PreparedStatement pStmnt = this.conn.prepareStatement(query);

	    pStmnt.setString(1, registration);
	    pStmnt.setString(2, make);
	    pStmnt.setString(3, model);
	    pStmnt.setInt(4, year);
	    pStmnt.setString(5, colour);
	    pStmnt.setDouble(6, position.getLat());
	    pStmnt.setDouble(7, position.getLng());
	    pStmnt.setInt(8, active);

	    pStmnt.executeUpdate();
	    pStmnt.close();

	    v = new Vehicle(registration, make, model, year, colour, position, active);
	} catch (SQLException e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	}
	return v;
    }

    /**
     * Returns a list of vehicle objects
     */
    public List<Vehicle> getVehicles() {
	List<Vehicle> vehicles = new ArrayList<Vehicle>();
	try {
	    Statement stmt = this.conn.createStatement();
	    ResultSet rs = stmt.executeQuery(
		    "SELECT `registration`, `make`, `model`, `year`, `colour`, ST_X(`location`) as `loc_x`, ST_Y(`location`) as `loc_y`, active FROM `vehicles`");
	    while (rs.next()) {
		String registration = rs.getString("registration");
		String make = rs.getString("make");
		String model = rs.getString("model");
		int year = rs.getInt("year");
		String colour = rs.getString("colour");
		double loc_x = rs.getDouble("loc_x");
		double loc_y = rs.getDouble("loc_y");
		// construct the object
		Position location = new Position(loc_x, loc_y);
		int avaliable = rs.getInt("active");
		Vehicle vehicle = new Vehicle(registration, make, model, year, colour, location, avaliable);
		vehicles.add(vehicle);
	    }
	    return vehicles;
	} catch (SQLException e) {
	    logger.error(e.getMessage());
	    // return an empty list in case of an error
	    return new ArrayList<Vehicle>();
	}
    }

    public List<Vehicle> getAvailableVehicles() {
	List<Vehicle> vehicles = new ArrayList<Vehicle>();
	try {
	    Statement stmt = this.conn.createStatement();
	    ResultSet rs = stmt.executeQuery(
		    "SELECT `registration`, `make`, `model`, `year`, `colour`, ST_X(`location`) as `loc_x`, "
			    + "ST_Y(`location`) as `loc_y` FROM `vehicles` WHERE vehicles.registration NOT IN"
			    + "(SELECT registration from bookings"
			    + " WHERE (timestamp + INTERVAL duration MINUTE) > NOW() ) AND active = 1;");
	    while (rs.next()) {
		String registration = rs.getString("registration");
		String make = rs.getString("make");
		String model = rs.getString("model");
		int year = rs.getInt("year");
		String colour = rs.getString("colour");
		double loc_x = rs.getDouble("loc_x");
		double loc_y = rs.getDouble("loc_y");
		// construct the object
		Position location = new Position(loc_x, loc_y);
		Vehicle vehicle = new Vehicle(registration, make, model, year, colour, location, 1);
		vehicles.add(vehicle);
	    }
	    return vehicles;
	} catch (SQLException e) {
	    logger.error(e.getMessage());
	    // return an empty list in case of an error
	    return new ArrayList<Vehicle>();
	}
    }

    /**
     * Returns a list of nearby vehicles to the given position
     */
    public List<NearbyVehicle> getNearbyVehicles(Position position) {
	List<NearbyVehicle> nearVehicles = new ArrayList<NearbyVehicle>();
	List<NearbyVehicle> sortedNearestVehicles = new ArrayList<NearbyVehicle>();
	List<Vehicle> vehicles = new ArrayList<Vehicle>();
	vehicles = getAvailableVehicles();

	for (int i = 0; i < vehicles.size(); i++) {
	    String registration = vehicles.get(i).getRegistration();
	    String make = vehicles.get(i).getMake();
	    String model = vehicles.get(i).getModel();
	    int year = vehicles.get(i).getYear();
	    String colour = vehicles.get(i).getColour();
	    Position positionC = vehicles.get(i).getPosition();
	    int active = vehicles.get(i).getActive();

	    double distance = Util.distance(position.getLat(), position.getLng(), positionC.getLat(),
		    positionC.getLng());

	    NearbyVehicle nV = new NearbyVehicle(registration, make, model, year, colour, positionC, active, distance);
	    nearVehicles.add(nV);
	}

	boolean done = true;
	while (done == true) {
	    NearbyVehicle closestCar = null;
	    double closestDist = 0;

	    if (nearVehicles.size() == 0) {
		done = false;
	    } else {
		for (int i = 0; i < nearVehicles.size(); i++) {

		    if (i == 0) {
			closestDist = nearVehicles.get(i).getDistance();
			closestCar = nearVehicles.get(i);

		    } else {

			if (nearVehicles.get(i).getDistance() < closestDist) {

			    closestDist = nearVehicles.get(i).getDistance();
			    closestCar = nearVehicles.get(i);
			}
		    }
		}

		sortedNearestVehicles.add(closestCar);
		nearVehicles.remove(closestCar);
	    }

	}
	return sortedNearestVehicles;
    }

    /**
     * Returns a list of bookings
     *
     * @throws SQLException
     */
    public List<Booking> getBookings() {
	logger.info("Get Booking");
	List<Booking> bookings = new ArrayList<Booking>();

	try {
	    Statement stmt = this.conn.createStatement();
	    ResultSet rs = stmt.executeQuery("SELECT bk.id, bk.timestamp, bk.customer_id, bk.duration,"
		    + " ST_X(start_location) as x_start, ST_Y(start_location) as y_start,"
		    + " ST_X(end_location) as x_end, ST_Y(end_location) as y_end,"
		    + " vh.registration, vh.make, vh.model, vh.year, vh.colour, ST_X(location) as current_x, ST_Y(location) as current_y, vh.active"
		    + " FROM bookings as bk" + " LEFT JOIN vehicles as vh ON bk.registration=vh.registration;");
	    while (rs.next()) {
		int id = rs.getInt("id");
		LocalDateTime timestamp = rs.getTimestamp("timestamp").toLocalDateTime();
		String customer_id = rs.getString("customer_id");
		int duration = rs.getInt("duration");
		double lat_start = rs.getDouble("x_start");
		double lng_start = rs.getDouble("y_start");
		double lat_end = rs.getDouble("x_end");
		double lng_end = rs.getDouble("y_end");
		Position start = new Position(lat_start, lng_start);
		Position end = new Position(lat_end, lng_end);

		String registration = rs.getString("registration");
		String make = rs.getString("make");
		String model = rs.getString("model");
		int year = rs.getInt("year");
		String colour = rs.getString("colour");
		double lat_curr = rs.getDouble("current_x");
		double lng_curr = rs.getDouble("current_y");
		Position car_curr_pos = new Position(lat_curr, lng_curr);
		int active = rs.getInt("active");

		Vehicle vehicle = new Vehicle(registration, make, model, year, colour, car_curr_pos, active);
		Booking booking = new Booking(id, timestamp, vehicle, customer_id, duration, start, end);

		bookings.add(booking);
	    }
	    return bookings;
	} catch (SQLException e) {
	    logger.error(e.getMessage());
	    // return an empty list in case of an error
	    return new ArrayList<Booking>();
	}
    }

    /**
     * Creates a booking, writes it to the database & returns the booking object
     *
     * @throws SQLException
     */
    public Booking createBooking(LocalDateTime timestamp, String registration, String customerId, int duration,
	    Position startLocation, Position endLocation) {
	logger.info("Create Booking for " + customerId);
	try {

	    // CHECK
	    // Checks this timestamp to see if its booked already for the same
	    // car.
	    if (!isCarDoubleBooked(timestamp, registration)) {
		if (!isUserDoubleBooked(timestamp, customerId)) {
		    // INSERT

		    String query = "INSERT INTO bookings "
			    + "(timestamp, registration, customer_id, duration, start_location, end_location) VALUES "
			    + "(?, ?, ?, ?, Point(?, ?), Point(?, ?))";

		    PreparedStatement pStmnt = this.conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);

		    pStmnt.setTimestamp(1, Timestamp.valueOf(timestamp));
		    pStmnt.setString(2, registration);
		    pStmnt.setString(3, customerId);
		    pStmnt.setInt(4, duration);

		    pStmnt.setDouble(5, startLocation.getLat());
		    pStmnt.setDouble(6, startLocation.getLng());

		    pStmnt.setDouble(7, endLocation.getLat());
		    pStmnt.setDouble(8, endLocation.getLng());

		    pStmnt.executeUpdate();

		    // get the inserted booking's ID
		    ResultSet rs = pStmnt.getGeneratedKeys();
		    if (rs.next()) {
			int id = rs.getInt(1);
			pStmnt.close();

			Vehicle vehicle = getVehicleByReg(registration);
			logger.info("Successfully inserted booking");
			return new Booking(id, timestamp, vehicle, customerId, duration, startLocation, endLocation);
		    }
		}

	    }

	} catch (SQLException e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	}

	// TODO: throw a custom exception on failure?
	return null;
    }

    public Vehicle getVehicleByReg(String registration) {
	logger.info("Getting vehicle with rego: " + registration);
	Vehicle v = null;
	Position position;
	try {
	    Statement stmt = this.conn.createStatement();
	    ResultSet rs = stmt.executeQuery("SELECT registration, make, model, year, colour, ST_X(location) as lat, "
		    + "ST_Y(location) as lng, active FROM vehicles WHERE registration LIKE '" + registration + "';");

	    if (rs.next()) {
		String rego = rs.getString("registration");
		String make = rs.getString("make");
		String model = rs.getString("model");
		int year = rs.getInt("year");
		String colour = rs.getString("colour");
		double lat = rs.getDouble("lat");
		double lng = rs.getDouble("lng");
		int active = rs.getInt("active");

		position = new Position(lat, lng);
		v = new Vehicle(rego, make, model, year, colour, position, active);

	    }

	} catch (SQLException e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	}
	return v;
    }

    public boolean isCarDoubleBooked(LocalDateTime currtime, String registration) {
	logger.info("Checking if vehicle:" + registration + " is double booked.");
	try {
	    // Gets the latest timestamp of a car booking.
	    String query = "SELECT timestamp,duration FROM bookings " + "WHERE registration = '" + registration + "' "
		    + "ORDER BY id DESC LIMIT 1;";

	    Statement stmt = this.conn.createStatement();
	    ResultSet rs = stmt.executeQuery(query);

	    if (rs.next()) {
		// Gets when the car is going to end.
		LocalDateTime bookingTime = rs.getTimestamp("timestamp").toLocalDateTime();
		LocalDateTime endtime = bookingTime.plusMinutes(rs.getInt(2));

		if (currtime.isBefore(endtime) || currtime.isEqual(endtime)) {
		    logger.info("Error vehicle:" + registration + " is Already Booked");
		    rs.close();
		    stmt.close();
		    return true; // It is double booked.
		}

	    }
	} catch (SQLException e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	}

	return false; // Not double Booked.
    }

    // Work in progress, will probably merge it together with CarDoubleBooked
    // after
    // more testing..
    public boolean isUserDoubleBooked(LocalDateTime currtime, String customerId) {
	logger.info("Checking if user:" + customerId + "double booked.");
	try {
	    // Gets the latest timestamp of a car booking.
	    String query = "SELECT timestamp,duration FROM bookings " + "WHERE customer_id = '" + customerId + "' "
		    + "ORDER BY id DESC LIMIT 1;";

	    Statement stmt = this.conn.createStatement();
	    ResultSet rs = stmt.executeQuery(query);

	    if (rs.next()) {
		// Gets when the car is going to end.
		LocalDateTime bookingTime = rs.getTimestamp("timestamp").toLocalDateTime();
		LocalDateTime endtime = bookingTime.plusMinutes(rs.getInt(2));

		if (currtime.isBefore(endtime) || currtime.isEqual(endtime)) {
		    logger.info("Error " + customerId + " Double Booked.");
		    rs.close();
		    stmt.close();
		    return true; // It is double booked.
		}

	    }
	} catch (SQLException e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	}

	return false; // Not double Booked.
    }

    public Boolean changeVehicleAvailability(String registration, int active) {
	try {
	    String query = "UPDATE vehicles set active = ? WHERE registration = ?;";
	    PreparedStatement ps = this.conn.prepareStatement(query);

	    ps.setInt(1, active);
	    ps.setString(2, registration);
	    ps.executeUpdate();

	    return true;

	} catch (SQLException e) {
	    logger.error(e.getMessage());
	    return false;
	}
    }

    public List<Booking> getBookingsOfUser(String email) {
	logger.info("Get Booking of " + email);
	List<Booking> bookings = new ArrayList<Booking>();

	try {
	    Statement stmt = this.conn.createStatement();
	    ResultSet rs = stmt.executeQuery("SELECT bk.id, bk.timestamp, bk.customer_id, bk.duration,"
		    + " ST_X(start_location) as x_start, ST_Y(start_location) as y_start,"
		    + " ST_X(end_location) as x_end, ST_Y(end_location) as y_end,"
		    + " vh.registration, vh.make, vh.model, vh.year, vh.colour, ST_X(location) as current_x, ST_Y(location) as current_y, vh.active"
		    + " FROM bookings as bk" + " LEFT JOIN vehicles as vh ON bk.registration=vh.registration"
		    + " WHERE bk.customer_id = '" + email + "';");
	    while (rs.next()) {
		int id = rs.getInt("id");
		LocalDateTime timestamp = rs.getTimestamp("timestamp").toLocalDateTime();
		String customer_id = rs.getString("customer_id");
		int duration = rs.getInt("duration");
		double lat_start = rs.getDouble("x_start");
		double lng_start = rs.getDouble("y_start");
		double lat_end = rs.getDouble("x_end");
		double lng_end = rs.getDouble("y_end");
		Position start = new Position(lat_start, lng_start);
		Position end = new Position(lat_end, lng_end);

		String registration = rs.getString("registration");
		String make = rs.getString("make");
		String model = rs.getString("model");
		int year = rs.getInt("year");
		String colour = rs.getString("colour");
		double lat_curr = rs.getDouble("current_x");
		double lng_curr = rs.getDouble("current_y");
		Position car_curr_pos = new Position(lat_curr, lng_curr);
		int active = rs.getInt("active");

		Vehicle vehicle = new Vehicle(registration, make, model, year, colour, car_curr_pos, active);
		Booking booking = new Booking(id, timestamp, vehicle, customer_id, duration, start, end);

		bookings.add(booking);
	    }
	    return bookings;
	} catch (SQLException e) {
	    logger.error(e.getMessage());
	    // return an empty list in case of an error
	    return new ArrayList<Booking>();
	}
    }

    public Boolean deleteBooking(int id) {
	logger.info("Deleting Booking with id: " + id);

	try {
	    Statement stmt = this.conn.createStatement();
	    int result = stmt.executeUpdate("DELETE FROM bookings WHERE id = " + id + ";");

	    if (result != 0)
		return true;
	    else
		return false;
	} catch (SQLException e) {
	    logger.error(e.getMessage());
	    return false;
	}
    }

    // Uses the ID of the booking to edit the booking.
    public Boolean editBooking(int id, LocalDateTime timestamp, String registration, String customerId, int duration,
	    Position startLocation, Position endLocation) {

	logger.info("Editing  Booking id:" + id);
	try {
	    if (bookingExists(id)) {
		if (checkReg(registration)) {
		    // Gets the latest timestamp of a car booking.
		    String query = "UPDATE bookings set timestamp = ?, registration = ?, customer_id = ?, duration = ?, start_location = Point(?,?), end_location = Point(?,?) WHERE id = "
			    + id + ";";

		    PreparedStatement ps = this.conn.prepareStatement(query);

		    ps.setTimestamp(1, Timestamp.valueOf(timestamp));
		    ps.setString(2, registration);
		    ps.setString(3, customerId);
		    ps.setInt(4, duration);

		    ps.setDouble(5, startLocation.getLat());
		    ps.setDouble(6, startLocation.getLng());

		    ps.setDouble(7, endLocation.getLat());
		    ps.setDouble(8, endLocation.getLng());

		    ps.executeUpdate();

		    ps.close();
		    logger.info("Successfully edited");
		    return true;
		}

	    }

	} catch (SQLException e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	    return false;
	}

	return false;
    }

    // Checks if booking exists.
    public Boolean bookingExists(int id) {

	logger.info("Checking if ID exists");
	try {
	    // Gets the latest timestamp of a car booking.
	    String query = "SELECT * FROM bookings where id=?;";
	    PreparedStatement ps = this.conn.prepareStatement(query);

	    ps.setInt(1, id);

	    ResultSet rs = ps.executeQuery();

	    if (rs.next()) {
		rs.close();
		ps.close();
		logger.info("Booking ID exists");
		return true; // This ID exists
	    }
	    rs.close();
	    ps.close();
	    logger.info("Booking ID does not exists");

	    return false; // Does not exist.
	} catch (SQLException e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	    return false;
	}
    }

    // Checks whether vehicles exist.
    public Boolean checkReg(String reg) {

	logger.info("Checking if Vehicle exists");
	try {
	    // Gets the latest timestamp of a car booking.
	    String query = "SELECT * FROM vehicles where registration=?;";
	    PreparedStatement ps = this.conn.prepareStatement(query);

	    ps.setString(1, reg);

	    ResultSet rs = ps.executeQuery();

	    if (rs.next()) {
		rs.close();
		ps.close();
		logger.info("Vehicle exists");
		return true; // This ID exists
	    }
	    rs.close();
	    ps.close();
	    logger.info("Vehicle does not exists");

	    return false; // Does not exist.
	} catch (SQLException e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	    return false;
	}
    }

    public Vehicle getBookingNow(String clientId) throws SQLException {
	Vehicle vehicle = null;

	Statement stmt = this.conn.createStatement();
	ResultSet rs = stmt
		.executeQuery("SELECT `registration`, `make`, `model`, `year`, `colour`, ST_X(`location`) as `loc_x`, "
			+ "ST_Y(`location`) as `loc_y` FROM `vehicles` WHERE vehicles.registration NOT IN"
			+ "(SELECT registration, " + clientId + " from bookings"
			+ " WHERE (timestamp + INTERVAL duration MINUTE) > NOW() ) AND active = 1;");

	while (rs.next()) {
	    String registration = rs.getString("registration");
	    String make = rs.getString("make");
	    String model = rs.getString("model");
	    int year = rs.getInt("year");
	    String colour = rs.getString("colour");
	    double loc_x = rs.getDouble("loc_x");
	    double loc_y = rs.getDouble("loc_y");
	    // construct the object
	    Position location = new Position(loc_x, loc_y);
	    vehicle = new Vehicle(registration, make, model, year, colour, location, 1);
	}

	return vehicle;
    }
}
