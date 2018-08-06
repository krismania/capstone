import java.util.Map;

import spark.ModelAndView;
import spark.template.handlebars.HandlebarsTemplateEngine;

public class Util {
    private Util() {
	// utility class has no constructor
    }

    public static String render(Map<String, Object> model, String templateName) {
	return new HandlebarsTemplateEngine().render(new ModelAndView(model, templateName + ".hbs"));
    }
}
