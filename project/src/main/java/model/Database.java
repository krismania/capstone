package model;

import java.io.Closeable;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.appengine.api.utils.SystemProperty;

import util.Config;

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
		String database = Config.get("localSqlDatabase");
		String username = Config.get("localSqlUsername");
		String password = Config.get("localSqlPassword");
		String url = "jdbc:mysql://localhost:3306/" + database + "?useSSL=false";
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
		+ "location POINT NOT NULL, " + "PRIMARY KEY (`registration`));";

	String bookingsSql = "CREATE TABLE IF NOT EXISTS `bookings` (" + "`id` INT NOT NULL AUTO_INCREMENT, "
		+ "`timestamp` DATETIME NOT NULL, " + "`registration` VARCHAR(10) NOT NULL, "
		+ "`customer_id` VARCHAR(50) NOT NULL, " + "`duration` SMALLINT UNSIGNED NOT NULL, "
		+ "`start_location` POINT NOT NULL, " + "`end_location` POINT NOT NULL, " + "PRIMARY KEY (`id`), "
		+ "FOREIGN KEY (`registration`) REFERENCES `vehicles`(`registration`));";

	Statement stmt = this.conn.createStatement();
	stmt.execute(vehiclesSql);
	stmt.execute(bookingsSql);

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

    public void insertVehicle() {

	logger.info("Insert Vehicles");
	try {
	    String query = "INSERT INTO vehicles (registration, make, model,year,colour) VALUES (?,?,?,?,?)";
	    PreparedStatement pStmnt = this.conn.prepareStatement(query);

	    pStmnt.setString(1, "HEH123");
	    pStmnt.setString(2, "TOYOTO");
	    pStmnt.setString(3, "TESTOTO");
	    pStmnt.setInt(4, 2111);
	    pStmnt.setString(5, "BLUE");
	    pStmnt.executeUpdate();

	    pStmnt.close();

	} catch (SQLException e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
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
		    "SELECT `registration`, `make`, `model`, `year`, `colour`, ST_X(`location`) as `loc_x`, ST_Y(`location`) as `loc_y` FROM `vehicles`");
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
		Vehicle vehicle = new Vehicle(registration, make, model, year, colour, location);
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
	List<NearbyVehicle> vehicles = new ArrayList<NearbyVehicle>();
	vehicles.add(new NearbyVehicle("UBR666", "Ford", "Falcon", 2013, "Orange", new Position(-37.815603, 144.969967),
		500));
	vehicles.add(new NearbyVehicle("FOK356", "Holden", "Barina", 2017, "White",
		new Position(-37.814022, 144.961954), 800));
	vehicles.add(new NearbyVehicle("JTD955", "Holden", "Commadore", 2005, "Grey",
		new Position(-37.816170, 144.956179), 1200));
	vehicles.add(
		new NearbyVehicle("BLA555", "Mazda", "3", 2010, "White", new Position(-37.818681, 144.958982), 1650));
	vehicles.add(
		new NearbyVehicle("QOP299", "Kia", "Rio", 2013, "Pink", new Position(-37.811510, 144.965667), 2210));
	return vehicles;
    }

    /**
     * Returns a list of bookings
     *
     * @throws SQLException
     */
    public List<Booking> getBookings() {
	// TODO: Get bookings from database
	logger.info("Get Booking");
	List<Vehicle> vehicles = getVehicles();
	List<Booking> bookings = new ArrayList<Booking>();

	try {
	    Statement stmt = this.conn.createStatement();
	    ResultSet rs = stmt.executeQuery("SELECT bk.id, bk.timestamp, bk.customer_id, bk.duration,"
		    + " ST_X(start_location) as x_start, ST_Y(start_location) as y_start,"
		    + " ST_X(end_location) as x_end, ST_Y(end_location) as y_end,"
		    + " vh.registration, vh.make, vh.model, vh.year, vh.colour, ST_X(location) as current_x, ST_Y(location) as current_y"
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

		Vehicle vehicle = vehicles.get(0);
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
	// TODO: write to the DB

	Calendar calendar = Calendar.getInstance();
	java.sql.Timestamp currtime = new java.sql.Timestamp(calendar.getTime().getTime());

	logger.info("Create Booking");

	// VALUES (?,?,?,?,?)";

	try {
	    String query = "INSERT INTO bookings (id,timestamp, registration,customer_id,duration,start_location,end_location) VALUES (id,?,?,?,?,Point(?,?),Point(?,?))";

	    PreparedStatement pStmnt = this.conn.prepareStatement(query);

	    pStmnt.setTimestamp(1, currtime);
	    pStmnt.setString(2, "HEH123");
	    pStmnt.setString(3, "ABC");
	    pStmnt.setInt(4, 2111);

	    pStmnt.setString(5, "20"); // start lon //stores as BLOB
	    pStmnt.setString(6, "50"); // start lat

	    pStmnt.setString(7, "-123"); // end lon
	    pStmnt.setString(8, "124"); // end lat

	    pStmnt.executeUpdate();

	    pStmnt.close();

	} catch (SQLException e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	}

	Vehicle vehicle = new Vehicle("ABC123", "Toyota", "Corolla", 2014, "Blue",
		new Position(-37.808401, 144.956159));
	return new Booking(1, LocalDateTime.of(2018, 8, 23, 18, 30), vehicle, "asdasd6516", 180, vehicle.getPosition(),
		new Position(-37.811510, 144.965667));
    }

}
