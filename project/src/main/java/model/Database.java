package model;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.appengine.api.utils.SystemProperty;

import controllers.ApiController;
import util.Config;

public class Database {

    final static Logger logger = LoggerFactory.getLogger(ApiController.class);

    private Connection conn;

    /**
     * Create a database object with an underlying {@link java.sql.Connection}
     * object. This constructor will return a connection to the local development
     * database when run locally, or a connection to the Cloud SQL database when
     * deployed.
     * 
     * @throws SQLException
     */
    public Database() throws SQLException {
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
    }

    /**
     * Initialize the database with the required tables
     * 
     * @throws SQLException
     */
    private void initDatabase() throws SQLException {
	logger.info("Initializing the database");
	String vehiclesSql = "CREATE TABLE IF NOT EXISTS `vehicles` (" + "`registration` VARCHAR(10) NOT NULL, "
		+ "`model` VARCHAR(50) NOT NULL, " + "`year` SMALLINT UNSIGNED, " + "`colour` VARCHAR(50) NOT NULL, "
		+ "PRIMARY KEY (`registration`));";

	String bookingsSql = "CREATE TABLE IF NOT EXISTS `bookings` (" + "`id` INT NOT NULL AUTO_INCREMENT, "
		+ "`timestamp` DATETIME NOT NULL, " + "`registration` VARCHAR(10) NOT NULL, "
		+ "`customer_id` VARCHAR(50) NOT NULL, " + "`start_location` POINT NOT NULL, "
		+ "`end_location` POINT NOT NULL, " + "PRIMARY KEY (`id`), "
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
    public void close() throws SQLException {
	logger.info("Closing the database");
	this.conn.close();
    }

}
