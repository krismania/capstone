package model;

import java.io.Closeable;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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

	// set tz on production db
	if (SystemProperty.environment.value() == SystemProperty.Environment.Value.Production) {
	    try (Statement tzStmt = this.conn.createStatement()) {
		tzStmt.execute("set time_zone = 'Australia/Melbourne'");
	    }
	}

	String vehiclesSql = "CREATE TABLE IF NOT EXISTS `vehicles` (`registration` VARCHAR(10) NOT NULL, "
		+ "`make` VARCHAR(50) NOT NULL, " + "`model` VARCHAR(50) NOT NULL, "
		+ "`year` SMALLINT UNSIGNED NOT NULL, " + "`colour` VARCHAR(50) NOT NULL, "
		+ "status TINYINT UNSIGNED NOT NULL, " + "`type` VARCHAR(50) NOT NULL, "
		+ "PRIMARY KEY (`registration`))";

	String bookingsSql = "CREATE TABLE IF NOT EXISTS `bookings` (" + "`id` INT NOT NULL AUTO_INCREMENT, "
		+ "`timestamp` DATETIME NOT NULL, " + "`registration` VARCHAR(10) NOT NULL, "
		+ "`customer_id` VARCHAR(50) NOT NULL, " + "`duration` SMALLINT UNSIGNED NOT NULL, "
		+ "PRIMARY KEY (`id`), " + "FOREIGN KEY (`registration`) REFERENCES `vehicles`(`registration`))";

	String admin = "CREATE TABLE IF NOT EXISTS `admins` (" + "`admin_id` VARCHAR(50) NOT NULL, "
		+ "PRIMARY KEY (`admin_id`))";

	String locationSql = "CREATE TABLE IF NOT EXISTS `locations` (`registration` VARCHAR(10) NOT NULL, "
		+ "timestamp DATETIME NOT NULL, location POINT NOT NULL)";

	String users = "CREATE TABLE IF NOT EXISTS `users` (`cid` VARCHAR(50) NOT NULL, "
		+ "`email` VARCHAR(50) NOT NULL, " + "PRIMARY KEY (`cid`))";

	String cost = "CREATE TABLE IF NOT EXISTS `costs` (`type` VARCHAR(50) NOT NULL, "
		+ "`rate` DECIMAL(20, 2) NOT NULL, " + "`base` INT NOT NULL, " + "PRIMARY KEY (`type`))";

	Statement stmt = this.conn.createStatement();
	stmt.execute(vehiclesSql);
	stmt.execute(cost);
	stmt.execute(bookingsSql);
	stmt.execute(admin);
	stmt.execute(locationSql);
	stmt.execute(users);
	stmt.close();

	try (Statement timeStmt = this.conn.createStatement()) {
	    ResultSet rs = timeStmt.executeQuery("select now()");
	    if (rs.next()) {
		logger.warn("SQL Server time: " + rs.getString(1));
	    } else {
		logger.warn("SQL Server time is unknown");
	    }
	}
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
	    Position position, int status, String type) {

	logger.info("Insert Vehicles");
	try {
	    String queryVehTable = "INSERT INTO vehicles (registration, make, model,year,colour,status,type) VALUES (?,?,?,?,?,?,?)";
	    String queryLocTable = "INSERT INTO locations (registration, timestamp, location) VALUES (?,?,POINT(?,?))";
	    PreparedStatement pStmntVeh = this.conn.prepareStatement(queryVehTable);
	    PreparedStatement pStmntLoc = this.conn.prepareStatement(queryLocTable);

	    pStmntVeh.setString(1, registration);
	    pStmntVeh.setString(2, make);
	    pStmntVeh.setString(3, model);
	    pStmntVeh.setInt(4, year);
	    pStmntVeh.setString(5, colour);
	    pStmntVeh.setInt(6, status);
	    pStmntVeh.setString(7, type);

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

	    return new Vehicle(registration, make, model, year, colour, position, status, type);
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
	    ResultSet rs = stmt.executeQuery(
		    "SELECT `registration`, `make`, `model`, `year`, `colour`, status, `type` FROM `vehicles`");
	    while (rs.next()) {
		String registration = rs.getString("registration");
		String make = rs.getString("make");
		String model = rs.getString("model");
		int year = rs.getInt("year");
		String colour = rs.getString("colour");
		// construct the object
		Position location = getVehiclePosition(registration);
		int avaliable = rs.getInt("status");
		String type = rs.getString("type");
		Vehicle vehicle = new Vehicle(registration, make, model, year, colour, location, avaliable, type);
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
	    ResultSet rs = stmt.executeQuery("SELECT `registration`, `make`, `model`, `year`, `colour`, `type` "
		    + "FROM `vehicles` WHERE vehicles.registration NOT IN" + "(SELECT registration from bookings"
		    + " WHERE (timestamp + INTERVAL duration MINUTE) > NOW() ) AND status = 0;");
	    while (rs.next()) {
		String registration = rs.getString("registration");
		String make = rs.getString("make");
		String model = rs.getString("model");
		int year = rs.getInt("year");
		String colour = rs.getString("colour");
		String type = rs.getString("type");
		// construct the object
		Position location = getVehiclePosition(registration);
		Vehicle vehicle = new Vehicle(registration, make, model, year, colour, location, 0, type);
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
	    String type = vehicles.get(i).getType();

	    double distance = Util.distance(position.getLat(), position.getLng(), positionC.getLat(),
		    positionC.getLng());

	    NearbyVehicle nV = new NearbyVehicle(registration, make, model, year, colour, positionC, status, type,
		    distance);
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
     * Gets the list of locations which the given booking traveled through.
     * Currently configured to return from sample data.
     */
    public List<Position> getRouteOfVehicle(Booking booking) {
	logger.info("Getting route for booking with ID " + booking.getId());
	List<Position> route = new ArrayList<>();

	String sql = "select st_x(`location`) as `lat`, st_y(`location`) as `lng` FROM `locations` "
		+ "where `registration` like ? " + "and minute(`timestamp`) > minute(?) order by `timestamp` asc";

	try (PreparedStatement ps = this.conn.prepareStatement(sql)) {
	    ps.setString(1, booking.getVehicle().getRegistration());
	    ps.setTimestamp(2, Timestamp.valueOf(booking.getTimestamp()));

	    logger.info("Executing query " + ps.toString());

	    ResultSet rs = ps.executeQuery();
	    while (rs.next()) {
		double lat = rs.getDouble("lat");
		double lng = rs.getDouble("lng");
		route.add(new Position(lat, lng));
	    }
	    rs.close();
	} catch (SQLException e) {
	    logger.error("Couldn't get route information for booking with ID " + booking.getId(), e);
	}

	return route;
    }

    /**
     * Gets a booking from the database based on it's ID
     *
     * @throws SQLException
     */
    public Booking getBooking(int id) throws SQLException {
	logger.info("Getting booking with ID " + id);

	String sql = "SELECT bk.id, bk.timestamp, bk.customer_id, bk.duration, vh.registration, vh.make, vh.model, vh.year, vh.colour, vh.status, vh.type, costs.rate, costs.base "
		+ "FROM bookings as bk LEFT JOIN vehicles as vh ON bk.registration=vh.registration, costs "
		+ "WHERE bk.id = ? ;";

	try (PreparedStatement ps = this.conn.prepareStatement(sql)) {
	    ps.setInt(1, id);
	    ResultSet rs = ps.executeQuery();
	    if (rs.next()) {
		// construct vehicle & booking
		LocalDateTime timestamp = rs.getTimestamp("timestamp").toLocalDateTime();
		String customer_id = rs.getString("customer_id");

		// COST CALCULATION
		int duration = rs.getInt("duration");
		int rate = rs.getInt("rate");
		int base = rs.getInt("base");
		double price = calculateCost(rate, base, duration);

		String registration = rs.getString("registration");
		String make = rs.getString("make");
		String model = rs.getString("model");
		int year = rs.getInt("year");
		String colour = rs.getString("colour");
		Position car_curr_pos = getVehiclePosition(registration);
		int status = rs.getInt("status");
		String type = rs.getString("type");
		Position start = getVehiclePositionByTime(registration, timestamp);

		Vehicle vehicle = new Vehicle(registration, make, model, year, colour, car_curr_pos, status, type);
		Booking booking = new Booking(id, timestamp, vehicle, customer_id, duration, start, price);

		return booking;
	    } else {
		logger.warn("No booking with ID " + id);
	    }
	} catch (SQLException e) {
	    logger.error("Failed to get booking with ID " + id, e);
	}
	return null;
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
		    + " vh.registration, vh.make, vh.model, vh.year, vh.colour, vh.status, vh.type, costs.rate, costs.base"
		    + " FROM bookings as bk" + " LEFT JOIN vehicles as vh ON bk.registration=vh.registration, costs "
		    + "WHERE vh.type = costs.type;");
	    while (rs.next()) {
		int id = rs.getInt("id");
		LocalDateTime timestamp = rs.getTimestamp("timestamp").toLocalDateTime();
		String customer_id = rs.getString("customer_id");

		// COST CALCULATION
		int duration = rs.getInt("duration");
		int rate = rs.getInt("rate");
		int base = rs.getInt("base");
		double price = calculateCost(rate, base, duration);

		String registration = rs.getString("registration");
		String make = rs.getString("make");
		String model = rs.getString("model");
		int year = rs.getInt("year");
		String colour = rs.getString("colour");
		Position car_curr_pos = getVehiclePosition(registration);
		int status = rs.getInt("status");
		String type = rs.getString("type");

		Position start = getVehiclePositionByTime(registration, timestamp);

		Vehicle vehicle = new Vehicle(registration, make, model, year, colour, car_curr_pos, status, type);
		Booking booking = new Booking(id, timestamp, vehicle, customer_id, duration, start, price);

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

			// initial cost always 0. - Only when booking ends does the cost gets
			// calculated.
			return new Booking(id, timestamp, vehicle, customerId, duration, startLocation, 0);
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
	    String query = "SELECT registration, make, model, year, colour, type, "
		    + "status FROM vehicles WHERE registration LIKE ?";
	    PreparedStatement ps = this.conn.prepareStatement(query);

	    ps.setString(1, registration);

	    ResultSet rs = ps.executeQuery();

	    if (rs.next()) {
		String rego = rs.getString("registration");
		String make = rs.getString("make");
		String model = rs.getString("model");
		int year = rs.getInt("year");
		String colour = rs.getString("colour");
		int status = rs.getInt("status");
		String type = rs.getString("type");
		position = getVehiclePosition(rego);
		v = new Vehicle(rego, make, model, year, colour, position, status, type);

	    }

	} catch (SQLException e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	}
	return v;
    }

    public boolean isCarBooked(LocalDateTime currtime, String registration) {
	logger.debug("Checking if vehicle:" + registration + " is double booked.");
	try {
	    // Gets the latest timestamp of a car booking.
	    String query = "SELECT timestamp,duration FROM bookings WHERE registration = ? ORDER BY id DESC LIMIT 1";
	    PreparedStatement ps = this.conn.prepareStatement(query);

	    ps.setString(1, registration);

	    ResultSet rs = ps.executeQuery();

	    if (rs.next()) {
		// Gets when the car is going to end.
		LocalDateTime bookingTime = rs.getTimestamp("timestamp").toLocalDateTime();
		LocalDateTime endtime = bookingTime.plusMinutes(rs.getInt(2));

		if (currtime.isBefore(endtime) || currtime.isEqual(endtime)) {
		    logger.info("Error vehicle:" + registration + " is Already Booked");
		    rs.close();
		    ps.close();
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
	    String query = "SELECT timestamp,duration FROM bookings WHERE customer_id = ? "
		    + "ORDER BY id DESC LIMIT 1";
	    PreparedStatement ps = this.conn.prepareStatement(query);

	    ps.setString(1, customerId);

	    ResultSet rs = ps.executeQuery();

	    if (rs.next()) {
		// Gets when the car is going to end.
		LocalDateTime bookingTime = rs.getTimestamp("timestamp").toLocalDateTime();
		LocalDateTime endtime = bookingTime.plusMinutes(rs.getInt(2));

		if (currtime.isBefore(endtime) || currtime.isEqual(endtime)) {
		    logger.info("Error " + customerId + " Double Booked.");
		    rs.close();
		    ps.close();
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
	    String query = "UPDATE vehicles set status = ? WHERE registration = ?";
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

	    String sql = "SELECT bk.id, bk.timestamp, bk.customer_id, bk.duration, vh.registration, vh.make, vh.model, vh.year, vh.colour, vh.status, vh.type, costs.rate, costs.base "
		    + "FROM bookings as bk LEFT JOIN vehicles as vh ON bk.registration=vh.registration, costs "
		    + "WHERE bk.customer_id = ? AND date_add(bk.timestamp, interval bk.duration minute) < now() AND vh.type = costs.type "
		    + "ORDER BY id DESC;";
	    PreparedStatement stmt = this.conn.prepareStatement(sql);
	    stmt.setString(1, clientId);
	    ResultSet rs = stmt.executeQuery();
	    while (rs.next()) {
		int bookingId = rs.getInt("id");
		LocalDateTime timestamp = rs.getTimestamp("timestamp").toLocalDateTime();
		String customer_id = rs.getString("customer_id");

		// COST CALCULATION
		int duration = rs.getInt("duration");
		int rate = rs.getInt("rate");
		int base = rs.getInt("base");
		double price = calculateCost(rate, base, duration);

		String registration = rs.getString("registration");
		String make = rs.getString("make");
		String model = rs.getString("model");
		int year = rs.getInt("year");
		String colour = rs.getString("colour");

		int status = rs.getInt("status");
		String type = rs.getString("type");
		Position start = getVehiclePositionByTime(registration, timestamp);
		Position car_curr_pos = getVehicleLastPosition(registration, Util.getCurrentTime());

		Vehicle vehicle = new Vehicle(registration, make, model, year, colour, car_curr_pos, status, type);
		Booking booking = new Booking(bookingId, timestamp, vehicle, customer_id, duration, start, price);

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
	    String query = "DELETE FROM bookings WHERE id = ?";
	    PreparedStatement ps = this.conn.prepareStatement(query);

	    ps.setInt(1, id);
	    int rowsAffected = ps.executeUpdate();

	    if (rowsAffected == 1) {
		return true;
	    } else {
		return false;
	    }
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
		    String query = "UPDATE bookings set timestamp = ?, registration = ?, customer_id = ?, duration = ? WHERE id = ?";
		    PreparedStatement ps = this.conn.prepareStatement(query);

		    ps.setTimestamp(1, Timestamp.valueOf(timestamp));
		    ps.setString(2, registration);
		    ps.setString(3, customerId);
		    ps.setInt(4, duration);
		    ps.setInt(5, id);

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
	    String query = "SELECT * FROM bookings where id=?";
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
	    String query = "SELECT * FROM vehicles where registration=?";
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
	LocalDateTime now = Util.getCurrentTime();
	try {
	    if (isCarBooked(now, registration)) {

		String query = "SELECT ST_X(location) as lat, ST_Y(location) as lng FROM locations WHERE registration = ? "
			+ "AND MINUTE(NOW()) >= MINUTE(timestamp) ORDER BY timestamp DESC LIMIT 1";
		PreparedStatement ps = this.conn.prepareStatement(query);

		ps.setString(1, registration);

		ResultSet rs = ps.executeQuery();

		rs.next();
		double lat = rs.getDouble("lat");
		double lng = rs.getDouble("lng");
		Position carLocation = new Position(lat, lng);
		ps.close();
		rs.close();
		return carLocation;
	    } else {
		String query = "SELECT ST_X(location) as lat, ST_Y(location) as lng FROM locations WHERE registration = ?"
			+ " AND MINUTE(timestamp) = 0 ORDER BY timestamp DESC LIMIT 1";
		PreparedStatement ps = this.conn.prepareStatement(query);

		ps.setString(1, registration);

		ResultSet rs = ps.executeQuery();

		rs.next();
		double lat = rs.getDouble("lat");
		double lng = rs.getDouble("lng");
		Position carLocation = new Position(lat, lng);
		ps.close();
		rs.close();
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
	    String query = "SELECT ST_X(location) as lat, ST_Y(location) as lng FROM locations WHERE registration = ?"
		    + " AND timestamp <= ? ORDER BY timestamp DESC LIMIT 1";
	    PreparedStatement ps = this.conn.prepareStatement(query);

	    ps.setString(1, registration);
	    ps.setTimestamp(2, Timestamp.valueOf(dateTime));

	    ResultSet rs = ps.executeQuery();

	    rs.next();
	    double lat = rs.getDouble("lat");
	    double lng = rs.getDouble("lng");
	    Position carLocation = new Position(lat, lng);
	    ps.close();
	    rs.close();
	    return carLocation;

	} catch (SQLException e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	    return null;
	}
    }

    public Position getVehicleLastPosition(String registration, LocalDateTime dateTime) {
	try {
	    String query = "SELECT ST_X(location) as lat, ST_Y(location) as lng FROM locations WHERE registration = ?"
		    + " AND MINUTE(timestamp) <= ? ORDER BY timestamp DESC LIMIT 1";
	    PreparedStatement ps = this.conn.prepareStatement(query);

	    ps.setString(1, registration);
	    ps.setTimestamp(2, Timestamp.valueOf(dateTime));

	    ResultSet rs = ps.executeQuery();

	    rs.next();
	    double lat = rs.getDouble("lat");
	    double lng = rs.getDouble("lng");
	    Position carLocation = new Position(lat, lng);
	    ps.close();
	    rs.close();
	    return carLocation;

	} catch (SQLException e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	    return null;
	}
    }

    public Booking getBookingNow(String clientId) throws SQLException {
	String query = "SELECT bk.id, bk.timestamp, bk.customer_id, bk.duration, vh.registration, vh.make, vh.model, vh.year, vh.colour, vh.status, vh.type, costs.base, costs.rate "
		+ "FROM bookings as bk left join vehicles as vh on bk.registration = vh.registration , costs "
		+ "WHERE customer_id like ? and date_add(`timestamp`, interval `duration` minute) > now() AND costs.type = vh.type limit 1;";

	PreparedStatement ps = this.conn.prepareStatement(query);

	ps.setString(1, clientId);

	ResultSet rs = ps.executeQuery();

	if (rs.next()) {
	    int id = rs.getInt("id");
	    LocalDateTime timestamp = rs.getTimestamp("timestamp").toLocalDateTime();
	    String customer_id = rs.getString("customer_id");
	    // COST CALCULATION
	    int duration = rs.getInt("duration");
	    // booking hasnt ended just use the base
	    int base = rs.getInt("base");

	    String registration = rs.getString("registration");
	    String make = rs.getString("make");
	    String model = rs.getString("model");
	    int year = rs.getInt("year");
	    String colour = rs.getString("colour");
	    Position car_curr_pos = getVehiclePosition(registration);
	    int status = rs.getInt("status");
	    String type = rs.getString("type");
	    Position start = getVehiclePositionByTime(registration, timestamp);

	    ps.close();
	    rs.close();

	    Vehicle vehicle = new Vehicle(registration, make, model, year, colour, car_curr_pos, status, type);
	    return new Booking(id, timestamp, vehicle, customer_id, duration, start, base);
	} else {
	    return null;
	}
    }

    public int checkVehicleStatus(String reg) throws SQLException {
	int status = 1;

	String query = "SELECT vh.status FROM vehicles as vh WHERE vh.registration LIKE ?";
	PreparedStatement ps = this.conn.prepareStatement(query);

	ps.setString(1, reg);
	ResultSet rs = ps.executeQuery();

	while (rs.next()) {
	    status = rs.getInt("status");
	}
	ps.close();
	rs.close();
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

    public void addUser(String cid, String email) throws SQLException {

	String sql = "SELECT cid FROM users WHERE cid LIKE ?";
	PreparedStatement stmt = this.conn.prepareStatement(sql);
	stmt.setString(1, cid);
	ResultSet rs = stmt.executeQuery();

	if (!rs.isBeforeFirst()) {
	    String query = "INSERT INTO users " + "(cid, email) VALUES (?, ?)";
	    PreparedStatement pStmnt = this.conn.prepareStatement(query);
	    pStmnt.setString(1, cid);
	    pStmnt.setString(2, email);
	    pStmnt.executeUpdate();
	    pStmnt.close();

	    logger.info("Adding to users database email: " + email);
	} else {
	    logger.info("Users table already has email: " + email);
	}

    }

    public String getCid(String email) throws SQLException {

	String cid = null;
	String sql = "SELECT cid FROM users WHERE email LIKE ?";
	PreparedStatement stmt = this.conn.prepareStatement(sql);
	stmt.setString(1, email);
	ResultSet rs = stmt.executeQuery();

	if (rs.next()) {
	    cid = rs.getString("cid");
	    System.out.println(cid);
	    logger.info("Client ID of user: " + cid);

	}
	stmt.close();
	rs.close();
	return cid;
    }

    public Booking endBooking(String clientId) {
	try {
	    Booking currentBooking = getBookingNow(clientId);
	    boolean accept = false;

	    if (currentBooking != null) {
		// calculate the number of minutes between the current time & the booking start
		LocalDateTime start = currentBooking.getTimestamp();
		LocalDateTime current = Util.getCurrentTime();
		int newDuration = (int) Math.ceil(Duration.between(start, current).toMinutes());

		// update the booking record
		String update = "update `bookings` set `duration` = ? where `id` = ?";
		PreparedStatement ps = this.conn.prepareStatement(update);

		logger.info("Setting duration of booking " + currentBooking.getId() + " to " + newDuration);

		ps.setInt(1, newDuration);
		ps.setInt(2, currentBooking.getId());

		int affectedRows = ps.executeUpdate();
		if (affectedRows == 1) { // if something changed, proceed with cost calculation and return.
		    ps.close();

		    // gets latest booking of customer.
		    String query = "SELECT bk.id, bk.timestamp, bk.customer_id, bk.duration, vh.registration, vh.make, vh.model, vh.year, vh.colour, vh.status, vh.type, costs.base, costs.rate "
			    + "FROM bookings as bk left join vehicles as vh on bk.registration = vh.registration , costs "
			    + "WHERE customer_id like ? AND costs.type = vh.type " + "ORDER BY id DESC limit 1;";
		    ps = this.conn.prepareStatement(query);

		    ps.setString(1, clientId);

		    ResultSet rs = ps.executeQuery();
		    logger.info("bug");
		    if (rs.next()) {

			int bookingId = rs.getInt("id");
			LocalDateTime timestamp = rs.getTimestamp("timestamp").toLocalDateTime();
			String customer_id = rs.getString("customer_id");

			// COST CALCULATION
			int duration = rs.getInt("duration");
			int rate = rs.getInt("rate");
			int base = rs.getInt("base");
			double price = calculateCost(rate, base, duration);

			String registration = rs.getString("registration");
			String make = rs.getString("make");
			String model = rs.getString("model");
			int year = rs.getInt("year");
			String colour = rs.getString("colour");

			int status = rs.getInt("status");
			String type = rs.getString("type");
			Position startpos = getVehiclePositionByTime(registration, timestamp);
			Position car_curr_pos = getVehicleLastPosition(registration, Util.getCurrentTime());

			Vehicle vehicle = new Vehicle(registration, make, model, year, colour, car_curr_pos, status,
				type);
			Booking booking = new Booking(bookingId, timestamp, vehicle, customer_id, duration, startpos,
				price);
			rs.close();
			ps.close();
			return booking;
		    } else {
			return null;
		    }

		}

	    }
	} catch (SQLException e) {
	    logger.error(e.getMessage());
	    e.printStackTrace();
	}
	return null;
    }

    public static long compareTwoTimeStamps(java.sql.Timestamp oldTime, java.sql.Timestamp currentTime) {
	long milliseconds1 = oldTime.getTime();
	long milliseconds2 = currentTime.getTime();

	long diff = milliseconds2 - milliseconds1;
	long diffMinutes = diff / (60 * 1000);

	return diffMinutes;
    }

    // CHECKS WHETHER CURRENT BOOKING HAS ENDED TRUE IF HAS , FALSE OTHERWISE
    public Boolean hasBookingEnded(int id, LocalDateTime currtime) {

	logger.info("Check if booking has ended. ");
	try {

	    String query = "SELECT timestamp, duration FROM bookings WHERE id = ?";

	    PreparedStatement ps = this.conn.prepareStatement(query);
	    ps.setInt(1, id);
	    ResultSet rs = ps.executeQuery();

	    int duration = 0;
	    if (rs.next()) {
		// Gets when the car is going to end.
		duration = rs.getInt("duration");
		LocalDateTime startTime = rs.getTimestamp("timestamp").toLocalDateTime();
		LocalDateTime endTime = startTime.plusMinutes(duration);

		if (currtime.isAfter(startTime) && currtime.isBefore(endTime)) {
		    logger.info(" Booking is still in session. " + duration);
		    rs.close();
		    ps.close();
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
    public Boolean extendBooking(String clientId, int extraDuration) {
	logger.info("Extending Booking of: " + clientId);
	try {
	    Booking currentBooking = getBookingNow(clientId);
	    if (currentBooking != null) {
		String update = "update `bookings` set `duration` = `duration` + ? where id = ?";
		PreparedStatement ps = this.conn.prepareStatement(update);
		ps.setInt(1, extraDuration);
		ps.setInt(2, currentBooking.getId());

		int affectedRows = ps.executeUpdate();

		if (affectedRows == 1) {
		    return true;
		}
	    }
	} catch (SQLException e) {
	    logger.error(e.getMessage());
	    e.printStackTrace();
	}

	return false;
    }

    public Boolean editVehicle(String registration, String make, String model, int year, String colour, int status) {
	logger.info("Editing  Vehicle with Rego:" + registration);
	try {
	    if (checkReg(registration)) {
		String query = "UPDATE vehicles set make = ?, model = ?, year = ?, colour = ?, status = ? "
			+ "WHERE registration = ?";

		PreparedStatement ps = this.conn.prepareStatement(query);

		ps.setString(1, make);
		ps.setString(2, model);
		ps.setInt(3, year);
		ps.setString(4, colour);
		ps.setInt(5, status);
		ps.setString(6, registration);

		ps.executeUpdate();

		ps.close();
		logger.info("Successfully edited");
		ps.close();
		return true;
	    }

	} catch (SQLException e) {
	    e.printStackTrace();
	    return false;
	}

	return false;
    }

    public float calculateCost(String reg, int duration) throws SQLException {

	String type = null;
	int rate = 0;
	float cost;

	String sql = "SELECT vh.type FROM vehicles as vh WHERE vh.registration LIKE ?";
	PreparedStatement ps = this.conn.prepareStatement(sql);
	ps.setString(1, reg);

	ResultSet rs = ps.executeQuery();
	while (rs.next()) {
	    type = rs.getString("type");
	}
	rs.close();

	String sql2 = "SELECT c.rate FROM costs as c WHERE c.type LIKE ?";
	PreparedStatement ps2 = this.conn.prepareStatement(sql2);
	ps2.setString(1, type);
	ResultSet rs2 = ps2.executeQuery();
	while (rs2.next()) {
	    rate = rs2.getInt("rate");
	}
	rs2.close();

	cost = rate * duration;
	// for testing purpose
	System.out.println(cost);
	logger.info("Cost for car: $" + cost);
	return cost;
    }

    public double calculateCost(int rate, int base, int duration) {
	double overtime = (double) duration / 30;
	double totalprice = base + rate * Math.ceil(overtime); // always rounds up, so we charge an extra 30min if
							       // overtime 30 min intervals.
	logger.info("Costs : " + totalprice);
	return totalprice;

    }

    /**
     * Creates a map from the vehicle tier rates stored in the database
     */
    public Map<String, Double> getRates() {
	Map<String, Double> rates = new LinkedHashMap<>();

	String ratesSql = "select `type`, `rate` from `costs`";
	try (PreparedStatement ps = this.conn.prepareStatement(ratesSql)) {
	    ResultSet rs = ps.executeQuery();
	    while (rs.next()) {
		rates.put(rs.getString("type"), rs.getDouble("rate"));
	    }
	} catch (SQLException e) {
	    logger.error("Couldn't get rates from db", e);
	}

	return rates;
    }

    /**
     * Sets the rates in the database according to the passed in map. This method
     * doesn't support adding/removing rates.
     *
     * @return {@code true} on success
     */
    public boolean setRates(Map<String, Double> rates) {
	String ratesSql = "update `costs` set `rate` = ? where `type` = ?";
	try (PreparedStatement ps = this.conn.prepareStatement(ratesSql)) {
	    for (Map.Entry<String, Double> entry : rates.entrySet()) {
		String tier = entry.getKey();
		Double rate = entry.getValue();
		// execute update
		ps.setDouble(1, rate);
		ps.setString(2, tier);
		ps.executeUpdate();
	    }
	    return true;
	} catch (SQLException e) {
	    logger.error("Couldn't set rates in db", e);
	}
	return false;

    }
}
