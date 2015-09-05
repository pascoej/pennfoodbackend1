package pennfood.PennFood;

import com.clarifai.api.ClarifaiClient;
import com.clarifai.api.RecognitionRequest;
import com.clarifai.api.RecognitionResult;
import com.clarifai.api.Tag;
import org.eclipse.jetty.util.MultiPartInputStreamParser;
import spark.Request;
import spark.Response;
import spark.Route;
import us.monoid.json.JSONArray;
import us.monoid.json.JSONException;
import us.monoid.json.JSONObject;
import us.monoid.web.Resty;

import javax.servlet.MultipartConfigElement;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static spark.Spark.post;

/**
 * Created by john on 9/4/15.
 */
public class Server {
    public static void main(String[] args) {
        getFood("butter");
        post("/uploadimage", new Route() {
            @Override
            public Object handle(Request req, Response res) throws Exception {
                MultipartConfigElement multipartConfigElement = new MultipartConfigElement("/tmp");
                req.raw().setAttribute("org.eclipse.multipartConfig", multipartConfigElement);
                MultiPartInputStreamParser.MultiPart file = (MultiPartInputStreamParser.MultiPart) req.raw().getPart("file");
                System.out.println(getTags(file.getFile()));
                return file.getFile();
            }
        });
    }
    private static String APP_ID = "ZcF98FbHzC_-sbFxe25hYD39cOZ445WrcK74ji6E";
    private static String APP_SECRET = "F4X5ttC6IQ8t0V8ihjf40824MkXJ4_W5glPP6k5J";
    public static List<Tag> getTags(File file) {
        ClarifaiClient clarifai = new ClarifaiClient(APP_ID, APP_SECRET);
        List<RecognitionResult> results =
                clarifai.recognize(new RecognitionRequest(file));
        List<Tag> tags = new ArrayList<>();
        for (Tag tag : results.get(0).getTags()) {
            System.out.println(tag.getName() + ": " + tag.getProbability());
            tags.add(tag);
        }
        return tags;
    }
    public static Food getFood(String in) {
        String url = "http://api.nal.usda.gov/ndb/search/?format=json&q="+in.replace(" ","%20")+"&sort=n&max=25&offset=0&api_key=DCrGYJ3xz3F5JnHMi2WRbIxHLYUvx3Rgn14mce15";
        try {
            JSONArray jsonArray = new Resty().json(url).object().getJSONObject("list").getJSONArray("item");
            JSONObject shortest = null;
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject jsonObject = jsonArray.getJSONObject(i);
                if (shortest != null && shortest.getString("name").length() > jsonObject.getString("name").length()) {
                    shortest = jsonObject;
                }else {
                    shortest = jsonObject;
                }
            }
            if (shortest == null) {
                return null;
            }
            String id = shortest.getString("ndbno");
            JSONObject jsonObject = new Resty().json("http://api.nal.usda.gov/ndb/reports/?ndbno=" + id + "&type=b&format=json&api_key=DCrGYJ3xz3F5JnHMi2WRbIxHLYUvx3Rgn14mce15").object();
            JSONArray nuArray = jsonObject.getJSONObject("report").getJSONObject("food").getJSONArray("nutrients");
            double kcal = 0;
            for (int i = 0; i < nuArray.length(); i++) {
                JSONObject mObject = nuArray.getJSONObject(i);
                if (mObject.getString("name").equalsIgnoreCase("energy")) {
                    kcal = mObject.getDouble("value");
                    break;
                }
            }
            System.out.println(kcal);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

}
