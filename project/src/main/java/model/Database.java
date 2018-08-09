package model;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.appengine.api.utils.SystemProperty;

import controllers.ApiController;
import util.Config;

public class Database {

    final static Logger logger = LoggerFactory.getLogger(ApiController.class);

    private Database() {
	// database class has no constructor
    }

    public static Connection getConnection() throws SQLException {
	if (SystemProperty.environment.value() == SystemProperty.Environment.Value.Production) {
	    String instance = Config.get("cloudSqlInstance");
	    String database = Config.get("cloudSqlDatabase");
	    String username = Config.get("cloudSqlUsername");
	    String password = Config.get("cloudSqlPassword");

	    String url = "jdbc:mysql://google/" + database + "?useSSL=false&cloudSqlInstance=" + instance
		    + "&socketFactory=com.google.cloud.sql.mysql.SocketFactory" + "&user=" + username + "&password="
		    + password;

	    logger.info("Connecting to production database");
	    return DriverManager.getConnection(url);
	} else {
	    String database = Config.get("localSqlDatabase");
	    String username = Config.get("localSqlUsername");
	    String password = Config.get("localSqlPassword");
	    String url = "jdbc:mysql://localhost:3306/" + database;
	    logger.info("Connecting to production database: " + url);
	    return DriverManager.getConnection(url, username, password);
	}
    }
}
