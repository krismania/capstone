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
import java.time.format.DateTimeFormatter;
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
     * object. This constructor will return a connection to the local development
     * database when run locally, or a connection to the Cloud SQL database when
     * deployed.
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
		// this fixes an issue with AppEngine Dev Server hot reloads
		Class.forName("com.mysql.jdbc.Driver");
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
	String vehiclesSql = "CREATE TABLE IF NOT EXISTS `vehicles` (`registration` VARCHAR(10) NOT NULL, "
		+ "`make` VARCHAR(50) NOT NULL, " + "`model` VARCHAR(50) NOT NULL, "
		+ "`year` SMALLINT UNSIGNED NOT NULL, " + "`colour` VARCHAR(50) NOT NULL, "
		+ "status TINYINT UNSIGNED NOT NULL, " + "PRIMARY KEY (`registration`));";

	String bookingsSql = "CREATE TABLE IF NOT EXISTS `bookings` (" + "`id` INT NOT NULL AUTO_INCREMENT, "
		+ "`timestamp` DATETIME NOT NULL, " + "`registration` VARCHAR(10) NOT NULL, "
		+ "`customer_id` VARCHAR(50) NOT NULL, " + "`duration` SMALLINT UNSIGNED NOT NULL, "
		+ "PRIMARY KEY (`id`), " + "FOREIGN KEY (`registration`) REFERENCES `vehicles`(`registration`));";

	String admin = "CREATE TABLE IF NOT EXISTS `admins` (" + "`admin_id` VARCHAR(50) NOT NULL, "
		+ "PRIMARY KEY (`admin_id`));";

	String locationSql = "CREATE TABLE IF NOT EXISTS `locations` (`registration` VARCHAR(10) NOT NULL, "
		+ "timestamp DATETIME NOT NULL, location POINT NOT NULL);";

	Statement stmt = this.conn.createStatement();
	stmt.execute(vehiclesSql);
	stmt.execute(bookingsSql);
	stmt.execute(admin);
	stmt.execute(locationSql);
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
	    Position position, int status) {

	logger.info("Insert Vehicles");
	try {
	    String queryVehTable = "INSERT INTO vehicles (registration, make, model,year,colour,status) VALUES (?,?,?,?,?,?);";
	    String queryLocTable = "INSERT INTO locations (registration, timestamp, location) VALUES (?,?,POINT(?,?));";
	    PreparedStatement pStmntVeh = this.conn.prepareStatement(queryVehTable);
	    PreparedStatement pStmntLoc = this.conn.prepareStatement(queryLocTable);

	    pStmntVeh.setString(1, registration);
	    pStmntVeh.setString(2, make);
	    pStmntVeh.setString(3, model);
	    pStmntVeh.setInt(4, year);
	    pStmntVeh.setString(5, colour);
	    pStmntVeh.setInt(6, status);

	    pStmntVeh.executeUpdate();
	    pStmntVeh.close();

	    pStmntLoc.setString(1, registration);
	    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
	    LocalDateTime dateTime = LocalDateTime.parse("2000-01-01 00:00:00", formatter);
	    pStmntLoc.setTimestamp(2, Timestamp.valueOf(dateTime));
	    pStmntLoc.setDouble(3, position.getLat());
	    pStmntLoc.setDouble(4, position.getLng());

	    pStmntLoc.executeUpdate();
	    pStmntLoc.close();

	    return new Vehicle(registration, make, model, year, colour, position, status);
	} catch (SQLException e) {
	    logger.warn("SQL error while inserting vehicle: " + e.getMessage());
	    return null;
	}
    }

    /**
     * Returns a list of vehicle objects
     */
    public List<Vehicle> getVehicles() {
	List<Vehicle> vehicles = new ArrayList<Vehicle>();
	try {
	    Statement stmt = this.conn.createStatement();
	    ResultSet rs = stmt
		    .executeQuery("SELECT `registration`, `make`, `model`, `year`, `colour`, status FROM `vehicles`");
	    while (rs.next()) {
		String registration = rs.getString("registration");
		String make = rs.getString("make");
		String model = rs.getString("model");
		int year = rs.getInt("year");
		String colour = rs.getString("colour");
		// construct the object
		Position location = getVehiclePosition(registration);
		int avaliable = rs.getInt("status");
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
	    ResultSet rs = stmt.executeQuery("SELECT `registration`, `make`, `model`, `year`, `colour` "
		    + "FROM `vehicles` WHERE vehicles.registration NOT IN" + "(SELECT registration from bookings"
		    + " WHERE (timestamp + INTERVAL duration MINUTE) > NOW() ) AND status = 0;");
	    while (rs.next()) {
		String registration = rs.getString("registration");
		String make = rs.getString("make");
		String model = rs.getString("model");
		int year = rs.getInt("year");
		String colour = rs.getString("colour");
		// construct the object
		Position location = getVehiclePosition(registration);
		Vehicle vehicle = new Vehicle(registration, make, model, year, colour, location, 0);
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
	    int status = vehicles.get(i).getStatus();

	    double distance = Util.distance(position.getLat(), position.getLng(), positionC.getLat(),
		    positionC.getLng());

	    NearbyVehicle nV = new NearbyVehicle(registration, make, model, year, colour, positionC, status, distance);
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
		    + " vh.registration, vh.make, vh.model, vh.year, vh.colour, vh.status" + " FROM bookings as bk"
		    + " LEFT JOIN vehicles as vh ON bk.registration=vh.registration;");
	    while (rs.next()) {
		int id = rs.getInt("id");
		LocalDateTime timestamp = rs.getTimestamp("timestamp").toLocalDateTime();
		String customer_id = rs.getString("customer_id");
		int duration = rs.getInt("duration");

		String registration = rs.getString("registration");
		String make = rs.getString("make");
		String model = rs.getString("model");
		int year = rs.getInt("year");
		String colour = rs.getString("colour");
		Position car_curr_pos = getVehiclePosition(registration);
		int status = rs.getInt("status");

		Position start = getVehiclePositionByTime(registration, timestamp);

		Vehicle vehicle = new Vehicle(registration, make, model, year, colour, car_curr_pos, status);
		Booking booking = new Booking(id, timestamp, vehicle, customer_id, duration, start);

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
    public Booking createBooking(LocalDateTime timestamp, String registration, String customerId, int duration) {
	logger.info("Create Booking for " + customerId);
	try {

	    // CHECK
	    // Checks this timestamp to see if its booked already for the same car.
	    if (!isCarBooked(timestamp, registration)) {
		if (!isUserDoubleBooked(timestamp, customerId)) {
		    // INSERT

		    String query = "INSERT INTO bookings " + "(timestamp, registration, customer_id, duration) VALUES "
			    + "(?, ?, ?, ?)";

		    PreparedStatement pStmnt = this.conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);

		    pStmnt.setTimestamp(1, Timestamp.valueOf(timestamp));
		    pStmnt.setString(2, registration);
		    pStmnt.setString(3, customerId);
		    pStmnt.setInt(4, duration);

		    pStmnt.executeUpdate();

		    Position startLocation = getVehiclePosition(registration);
		    // get the inserted booking's ID
		    ResultSet rs = pStmnt.getGeneratedKeys();
		    if (rs.next()) {
			int id = rs.getInt(1);
			pStmnt.close();

			Vehicle vehicle = getVehicleByReg(registration);
			logger.info("Successfully inserted booking");
			return new Booking(id, timestamp, vehicle, customerId, duration, startLocation);
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
	    ResultSet rs = stmt.executeQuery("SELECT registration, make, model, year, colour, "
		    + "status FROM vehicles WHERE registration LIKE '" + registration + "';");

	    if (rs.next()) {
		String rego = rs.getString("registration");
		String make = rs.getString("make");
		String model = rs.getString("model");
		int year = rs.getInt("year");
		String colour = rs.getString("colour");
		int status = rs.getInt("status");

		position = getVehiclePosition(rego);
		v = new Vehicle(rego, make, model, year, colour, position, status);

	    }

	} catch (SQLException e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	}
	return v;
    }

    public boolean isCarBooked(LocalDateTime currtime, String registration) {
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

    // Work in progress, will probably merge it together with CarDoubleBooked after
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

    public Boolean changeVehicleStatus(String registration, int status) {
	try {
	    String query = "UPDATE vehicles set status = ? WHERE registration = ?;";
	    PreparedStatement ps = this.conn.prepareStatement(query);

	    ps.setInt(1, status);
	    ps.setString(2, registration);
	    ps.executeUpdate();

	    return true;

	} catch (SQLException e) {
	    logger.error(e.getMessage());
	    return false;
	}
    }

    public List<Booking> getBookingsOfUser(String clientId) {
	logger.info("Get Bookings for " + clientId);
	List<Booking> bookings = new ArrayList<Booking>();

	try {
	    String sql = "SELECT bk.id, bk.timestamp, bk.customer_id, bk.duration, vh.registration, vh.make, vh.model, vh.year, vh.colour, vh.status "
		    + "FROM bookings as bk LEFT JOIN vehicles as vh ON bk.registration=vh.registration "
		    + "WHERE bk.customer_id = ? AND date_add(bk.timestamp, interval bk.duration minute) < now()";
	    PreparedStatement stmt = this.conn.prepareStatement(sql);
	    stmt.setString(1, clientId);
	    ResultSet rs = stmt.executeQuery();
	    while (rs.next()) {
		int bookingId = rs.getInt("id");
		LocalDateTime timestamp = rs.getTimestamp("timestamp").toLocalDateTime();
		String customer_id = rs.getString("customer_id");
		int duration = rs.getInt("duration");
		;

		String registration = rs.getString("registration");
		String make = rs.getString("make");
		String model = rs.getString("model");
		int year = rs.getInt("year");
		String colour = rs.getString("colour");

		int status = rs.getInt("status");

		Position start = getVehiclePositionByTime(registration, timestamp);
		Position car_curr_pos = getVehicleLastPosition(registration, LocalDateTime.now());

		Vehicle vehicle = new Vehicle(registration, make, model, year, colour, car_curr_pos, status);
		Booking booking = new Booking(bookingId, timestamp, vehicle, customer_id, duration, start);

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
    public Boolean editBooking(int id, LocalDateTime timestamp, String registration, String customerId, int duration) {

	logger.info("Editing  Booking id:" + id);
	try {
	    if (bookingExists(id)) {
		if (checkReg(registration)) {
		    // Gets the latest timestamp of a car booking.
		    String query = "UPDATE bookings set timestamp = ?, registration = ?, customer_id = ?, duration = ? WHERE id = "
			    + id + ";";

		    PreparedStatement ps = this.conn.prepareStatement(query);

		    ps.setTimestamp(1, Timestamp.valueOf(timestamp));
		    ps.setString(2, registration);
		    ps.setString(3, customerId);
		    ps.setInt(4, duration);

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

    public Position getVehiclePosition(String registration) {
	LocalDateTime now = LocalDateTime.now();
	try {
	    if (isCarBooked(now, registration)) {
		String query = "SELECT ST_X(location) as lat, ST_Y(location) as lng FROM locations WHERE registration = '"
			+ registration + "' AND MINUTE(NOW()) >= MINUTE(timestamp) "
			+ "ORDER BY timestamp DESC LIMIT 1;";

		Statement stmt = this.conn.createStatement();
		ResultSet rs = stmt.executeQuery(query);

		rs.next();
		double lat = rs.getDouble("lat");
		double lng = rs.getDouble("lng");
		Position carLocation = new Position(lat, lng);
		return carLocation;
	    } else {
		String query = "SELECT ST_X(location) as lat, ST_Y(location) as lng FROM locations WHERE registration = '"
			+ registration + "' AND MINUTE(timestamp) = 0  " + "ORDER BY timestamp DESC LIMIT 1;";

		Statement stmt = this.conn.createStatement();
		ResultSet rs = stmt.executeQuery(query);

		rs.next();
		double lat = rs.getDouble("lat");
		double lng = rs.getDouble("lng");
		Position carLocation = new Position(lat, lng);
		return carLocation;
	    }
	} catch (SQLException e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	    return null;
	}
    }

    public Position getVehiclePositionByTime(String registration, LocalDateTime dateTime) {
	try {
	    String query = "SELECT ST_X(location) as lat, ST_Y(location) as lng FROM locations WHERE registration = '"
		    + registration + "' AND timestamp <= '" + dateTime + "' ORDER BY timestamp DESC LIMIT 1;";

	    Statement stmt = this.conn.createStatement();
	    ResultSet rs = stmt.executeQuery(query);

	    rs.next();
	    double lat = rs.getDouble("lat");
	    double lng = rs.getDouble("lng");
	    Position carLocation = new Position(lat, lng);
	    return carLocation;

	} catch (SQLException e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	    return null;
	}
    }

    public Position getVehicleLastPosition(String registration, LocalDateTime dateTime) {
	try {
	    String query = "SELECT ST_X(location) as lat, ST_Y(location) as lng FROM locations WHERE registration = '"
		    + registration + "' AND MINUTE(timestamp) <= '" + dateTime.getMinute()
		    + "' ORDER BY timestamp DESC LIMIT 1;";

	    Statement stmt = this.conn.createStatement();
	    ResultSet rs = stmt.executeQuery(query);

	    rs.next();
	    double lat = rs.getDouble("lat");
	    double lng = rs.getDouble("lng");
	    Position carLocation = new Position(lat, lng);
	    return carLocation;

	} catch (SQLException e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	    return null;
	}
    }

    public Booking getBookingNow(String clientId) throws SQLException {
	Booking br = null;

	Statement stmt = this.conn.createStatement();
	ResultSet rs = stmt.executeQuery("SELECT bk.id, bk.timestamp, bk.customer_id, bk.duration,"
		+ " vh.registration, vh.make, vh.model, vh.year, vh.colour, vh.status" + " FROM bookings as bk"
		+ " LEFT JOIN vehicles as vh ON bk.registration=vh.registration WHERE (timestamp + INTERVAL duration MINUTE) > NOW() AND customer_id LIKE '"
		+ clientId + "';");

	while (rs.next()) {
	    int id = rs.getInt("id");
	    LocalDateTime timestamp = rs.getTimestamp("timestamp").toLocalDateTime();
	    String customer_id = rs.getString("customer_id");
	    int duration = rs.getInt("duration");

	    String registration = rs.getString("registration");
	    String make = rs.getString("make");
	    String model = rs.getString("model");
	    int year = rs.getInt("year");
	    String colour = rs.getString("colour");
	    Position car_curr_pos = getVehiclePosition(registration);
	    int status = rs.getInt("status");

	    Position start = getVehiclePositionByTime(registration, timestamp);

	    Vehicle vehicle = new Vehicle(registration, make, model, year, colour, car_curr_pos, status);
	    br = new Booking(id, timestamp, vehicle, customer_id, duration, start);
	}
	return br;
    }

    public int checkVehicleStatus(String reg) throws SQLException {
	int status = 1;
	Statement stmt = this.conn.createStatement();
	ResultSet rs = stmt
		.executeQuery("SELECT vh.status FROM vehicles as vh WHERE vh.registration LIKE '" + reg + "';");

	while (rs.next()) {
	    status = rs.getInt("status");
	}
	return status;
    }

    /**
     * Checks if the given user has administrator permissions.
     *
     * @return {@code true} if the user is an administrator
     */
    public boolean isAdmin(String clientId) {
	logger.info("Checking if user " + clientId + " is an admin");
	String query = "select 1 from `admins` where `admin_id` = ?";
	try (PreparedStatement ps = this.conn.prepareStatement(query)) {
	    ps.setString(1, clientId);
	    ResultSet rs = ps.executeQuery();
	    // if the query has any rows, the user is in the admin table
	    // next() will return false if there are no rows.
	    return rs.next();
	} catch (SQLException e) {
	    logger.error(e.getMessage());
	    return false;
	}
    }

    public boolean endBooking(String clientid, LocalDateTime currTime) {

	try {

	    // Gets the latest timestamp of a car booking.
	    String query1 = "SELECT * FROM bookings WHERE customer_id = '" + clientid + "' ORDER BY id DESC LIMIT 1;";

	    // PreparedStatement ps = this.conn.prepareStatement(query);
	    Statement stmt = this.conn.createStatement();
	    ResultSet rs = stmt.executeQuery(query1);
	    LocalDateTime bookingTimeStart = null;
	    boolean bookingEnded = true;
	    int id = 0; // stores the id of booking for query2

	    if (rs.next()) {
		// Gets when the car is going to end.
		bookingTimeStart = rs.getTimestamp("timestamp").toLocalDateTime();
		id = rs.getInt("id");
		bookingEnded = hasBookingEnded(id, currTime); // TRUE if ended, False if hasnt.

	    }
	    // Only do this if booking hasnt ended and checks for current time.
	    if ((bookingTimeStart.isBefore(currTime) || bookingTimeStart.isEqual(currTime)) && !bookingEnded) {

		int minutes = (int) compareTwoTimeStamps(Timestamp.valueOf(bookingTimeStart),
			Timestamp.valueOf(currTime));
		logger.info("start " + id + "minutes: " + minutes);
		rs.close();
		stmt.close();

		String query2 = "UPDATE bookings set duration = ? WHERE id = '" + id + "' AND customer_id = '"
			+ clientid + "';";
		PreparedStatement ps = this.conn.prepareStatement(query2);

		ps.setInt(1, minutes);

		ps.executeUpdate();
		ps.close();

		logger.info("Ended Booking.");
		return true;
	    } else {
		logger.info("Error.");
		return false; // Bad Request.
	    }

	} catch (SQLException e) {
	    logger.error(e.getMessage());
	    return false;
	}

    }

    public static long compareTwoTimeStamps(java.sql.Timestamp oldTime, java.sql.Timestamp currentTime) {
	long milliseconds1 = oldTime.getTime();
	long milliseconds2 = currentTime.getTime();

	long diff = milliseconds2 - milliseconds1;
	long diffSeconds = diff / 1000;
	long diffMinutes = diff / (60 * 1000);
	long diffHours = diff / (60 * 60 * 1000);
	long diffDays = diff / (24 * 60 * 60 * 1000);

	return diffMinutes;
    }

    // CHECKS WHETHER CURRENT BOOKING HAS ENDED TRUE IF HAS , FALSE OTHERWISE
    public Boolean hasBookingEnded(int id, LocalDateTime currtime) {

	logger.info("Check if booking has ended. ");
	try {

	    String query = "SELECT timestamp, duration FROM bookings WHERE id = " + id + ";";

	    Statement stmt = this.conn.createStatement();
	    ResultSet rs = stmt.executeQuery(query);
	    int duration = 0;
	    if (rs.next()) {
		// Gets when the car is going to end.
		duration = rs.getInt("duration");
		LocalDateTime startTime = rs.getTimestamp("timestamp").toLocalDateTime();
		LocalDateTime endTime = startTime.plusMinutes(duration);

		if (currtime.isAfter(startTime) && currtime.isBefore(endTime)) {
		    logger.info(" Booking is still in session. " + duration);
		    rs.close();
		    stmt.close();
		    return false; // Booking has not ended.
		}

	    }

	} catch (SQLException e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	    return true; // errorz
	}
	logger.info(" Booking Timestamp error occured. "); // the timestamps are incorrect.
	return true; // Booking ended.

    }

    // Uses the ID of the booking to edit the booking.
    public Boolean extendBooking(String customerId, int extendedduration, LocalDateTime currTime) {

	logger.info("Extending Booking of: " + customerId);
	try {
	    // finds the id to check if booking has ended.
	    String query1 = "SELECT * FROM bookings WHERE customer_id = '" + customerId + "' ORDER BY id DESC LIMIT 1;";

	    Statement stmt = this.conn.createStatement();
	    ResultSet rs = stmt.executeQuery(query1);
	    LocalDateTime bookingTimeStart = null;
	    int id = 0;
	    int duration = 0;
	    boolean bookingEnded = true;

	    if (rs.next()) {
		// Gets when the car is going to end.
		bookingTimeStart = rs.getTimestamp("timestamp").toLocalDateTime();
		id = rs.getInt("id");
		duration = rs.getInt("duration");
		bookingEnded = hasBookingEnded(id, currTime); // TRUE if booking ended, False if hasnt.
	    }

	    rs.close();
	    stmt.close();

	    if (!bookingEnded) {
		String query2 = "UPDATE bookings set duration = ? + " + extendedduration + " WHERE customer_id = '"
			+ customerId + "' ORDER BY id DESC LIMIT 1;";

		PreparedStatement ps = this.conn.prepareStatement(query2);

		ps.setInt(1, duration);

		ps.executeUpdate();

		ps.close();
		logger.info("Successfully extended");
		return true;
	    } else {
		logger.info("Extension Failed");
		return false; // Bad Request
	    }
	    // Gets the latest timestamp of a car booking.

	} catch (SQLException e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	    return false;
	}
    }

}
