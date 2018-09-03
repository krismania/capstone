package model;

import java.io.Closeable;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
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

    /**
     * Returns a list of vehicle objects
     */
    public List<Vehicle> getVehicles() {
	// TODO: Get vehicles from database
	List<Vehicle> vehicles = new ArrayList<Vehicle>();
	vehicles.add(new Vehicle("ABC123", "Toyota", "Corolla", 2014, "Blue", new Position(-37.808401, 144.956159)));
	vehicles.add(new Vehicle("QRB990", "BMW", "325i", 2003, "Black", new Position(-37.809741, 144.970895)));
	vehicles.add(new Vehicle("TAA325", "Peugeot", "307 SW", 2008, "Grey", new Position(-37.805819, 144.960025)));
	vehicles.add(new Vehicle("UBR666", "Ford", "Falcon", 2013, "Orange", new Position(-37.815603, 144.969967)));
	vehicles.add(new Vehicle("FOK356", "Holden", "Barina", 2017, "White", new Position(-37.814022, 144.961954)));
	vehicles.add(new Vehicle("JTD955", "Holden", "Commadore", 2005, "Grey", new Position(-37.816170, 144.956179)));
	vehicles.add(new Vehicle("BLA555", "Mazda", "3", 2010, "White", new Position(-37.818681, 144.958982)));
	vehicles.add(new Vehicle("QOP299", "Kia", "Rio", 2013, "Pink", new Position(-37.811510, 144.965667)));
	vehicles.add(new Vehicle("YODUDE", "Nissan", "Skyline", 2010, "Black", new Position(-37.810422, 144.968597)));
	vehicles.add(new Vehicle("MAGPIES", "Mercedes-Benz", "CLC200 Kompressor", 2009, "Black",
		new Position(-37.807232, 144.963620)));
	return vehicles;
    }

    /**
     * Returns a list of nearby vehicles to the given position
     */
    public List<NearbyVehicle> getNearbyVehicles(Position position) {
	List<NearbyVehicle> nearVehicles = new ArrayList<NearbyVehicle>();
	List<NearbyVehicle> sortedNearestVehicles = new ArrayList<NearbyVehicle>();
	List<Vehicle> vehicles = new ArrayList<Vehicle>();
	vehicles = getVehicles();

	for (int i = 0; i < vehicles.size(); i++) {
	    String registration = vehicles.get(i).getRegistration();
	    String make = vehicles.get(i).getMake();
	    String model = vehicles.get(i).getModel();
	    int year = vehicles.get(i).getYear();
	    String colour = vehicles.get(i).getColour();
	    Position positionC = vehicles.get(i).getPosition();

	    double distance = Util.distance(position.getLat(), position.getLng(), positionC.getLat(),
		    positionC.getLng());

	    NearbyVehicle nV = new NearbyVehicle(registration, make, model, year, colour, positionC, distance);
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
     */
    public List<Booking> getBookings() {
	// TODO: Get bookings from database
	List<Vehicle> vehicles = getVehicles();
	List<Booking> bookings = new ArrayList<Booking>();
	bookings.add(new Booking(1, LocalDateTime.of(2018, 8, 23, 18, 30), vehicles.get(0), "asdasd6516", 180,
		new Position(-37.816170, 144.956179), new Position(-37.811510, 144.965667)));
	bookings.add(new Booking(2, LocalDateTime.of(2018, 8, 22, 11, 15), vehicles.get(1), "asdasd6516", 360,
		new Position(-37.816170, 144.956179), new Position(-37.811510, 144.965667)));
	return bookings;
    }

    /**
     * Creates a booking, writes it to the database & returns the booking object
     */
    public Booking createBooking(LocalDateTime timestamp, String registration, String customerId, int duration,
	    Position endLocation) {
	// TODO: write to the DB
	Vehicle vehicle = new Vehicle("ABC123", "Toyota", "Corolla", 2014, "Blue",
		new Position(-37.808401, 144.956159));
	return new Booking(1, LocalDateTime.of(2018, 8, 23, 18, 30), vehicle, "asdasd6516", 180, vehicle.getPosition(),
		new Position(-37.811510, 144.965667));
    }

}
