import static spark.Spark.get;
import static spark.Spark.port;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class Main {

    public static void main(String[] args) {
	port(4567);

	Properties prop = new Properties();

	// Load properties
	try (InputStream propFile = new FileInputStream("config.properties")) {
	    prop.load(propFile);
	} catch (FileNotFoundException e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	} catch (IOException e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	}

	String mapsApiKey = prop.getProperty("mapsApiKey");

	get("/", (req, res) -> {
	    Map<String, Object> model = new HashMap<>();
	    model.put("mapsApiKey", mapsApiKey);
	    return Util.render(model, "index");
	});
    }

}
