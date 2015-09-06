package pennfood.PennFood;

import com.clarifai.api.ClarifaiClient;
import com.clarifai.api.RecognitionRequest;
import com.clarifai.api.RecognitionResult;
import com.clarifai.api.Tag;
import com.mongodb.*;
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
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;

import static spark.Spark.get;
import static spark.Spark.post;

/**
 * Created by john on 9/4/15.
 */
public class Server {
    private static List<String> blacklist = new ArrayList<>();
    public static void main(String[] args) {
        try {
            blacklist.addAll(Files.readAllLines(new File("blacklist").toPath()));
        } catch (IOException e) {
            e.printStackTrace();
        }
        post("/uploadimage", new Route() {
            @Override
            public Object handle(Request req, Response res) throws Exception {
                MultipartConfigElement multipartConfigElement = new MultipartConfigElement("/tmp");
                req.raw().setAttribute("org.eclipse.multipartConfig", multipartConfigElement);
                MultiPartInputStreamParser.MultiPart file = (MultiPartInputStreamParser.MultiPart) req.raw().getPart("file");
                List<Tag> tags = getTags(file.getFile());

                for (Tag tag : tags) {
                    if (!blacklist.contains(tag.getName())) {
                        Food food = getFood(tag.getName(),"1");
                        if (food != null) {
                            storeInServer(food);
                            System.out.println("ueouaoeuoa");
                            return caloriesToday("1");
                        }
                    }
                }

                return "food not found";
            }
        });
        get("/uploadimage", new Route() {
            @Override
            public Object handle(Request req, Response res) throws Exception {
                return "<form action=\"/uploadimage\" method=\"post\" enctype=\"multipart/form-data\">\n" +
                        "<input type=\"file\" accept=\"image/*\" capture=\"camera\" name=\"file\">\n" +
                        "  <input type=\"text\" name=\"firstname\">\n<input type=\"submit\">\n" +
                        "</form>";
            }
        });
        get("/stats/:id/:city", new Route() {
            @Override
            public Object handle(Request req, Response res) throws Exception {
                String userId = req.params(":id");
                double calories = caloriesToday(userId);
                double avgCityCalories = avgCaloriesInCityToday(req.params("city"));
                double lastCalories = lastCalories(userId);
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("calories",calories);
                jsonObject.put("avg_city",avgCityCalories);
                jsonObject.put("last_calories",lastCalories);
                return jsonObject.toString();
            }
        });
        get("/log/:id", new Route() {
            @Override
            public Object handle(Request req, Response res) throws Exception {
                String userId = req.params(":id");
                return allCalorieLog(userId).toString();
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
    public static Food getFood(String in, String userId) {
        System.out.println("search id: " + in);
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
            String foodGroup = shortest.getString("group");
            JSONObject jsonObject = new Resty().json("http://api.nal.usda.gov/ndb/reports/?ndbno=" + id + "&type=b&format=json&api_key=DCrGYJ3xz3F5JnHMi2WRbIxHLYUvx3Rgn14mce15").object();
            JSONArray nuArray = jsonObject.getJSONObject("report").getJSONObject("food").getJSONArray("nutrients");
            System.out.println(jsonObject);
            double kcal = 0;
            for (int i = 0; i < nuArray.length(); i++) {
                JSONObject mObject = nuArray.getJSONObject(i);
                if (mObject.getString("name").equalsIgnoreCase("energy")) {
                    kcal = mObject.getDouble("value");
                    break;
                }
            }
            System.out.println("name: " + shortest.getString("name") + " - " + kcal);
            Food food = new Food();
            food.setCalories(kcal);
            food.setId(new Random().nextInt(100000) + "");
            food.setUserId(userId);
            food.setDate(new Date());
            food.setGroup(foodGroup);
            food.setName(shortest.getString("name"));
            return food;
        } catch (IOException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }
    private static void storeInServer(Food food) {
        try {
            MongoClient mongoClient = new MongoClient();
            DB db = mongoClient.getDB("test");
            DBCollection coll = db.getCollection("food_log");
            coll.insert(food);
            mongoClient.close();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
    }
    private static double caloriesToday(String userId) {
        try {
            MongoClient mongoClient = new MongoClient();
            DB db = mongoClient.getDB("test");
            DBCollection coll = db.getCollection("food_log");
            double cal = 0;
            for (DBObject dbObject : coll.find()) {
                if (dbObject.containsField("cal")) {
                    cal += (Double)dbObject.get("cal");
                }
            }
            return cal;
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        return 0;
    }
    public static JSONArray allCalorieLog(String id) {
        try {
            MongoClient mongoClient = new MongoClient();
            DB db = mongoClient.getDB("test");
            DBCollection coll = db.getCollection("food_log");
            JSONArray jsonArray = new JSONArray();
            for (DBObject dbObject : coll.find(new BasicDBObject("user_id",id))) {
                jsonArray.put(dbObject);
            }
            return jsonArray;
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        return null;
    }
    public static double lastCalories(String id) {
        try {
            MongoClient mongoClient = new MongoClient();
            DB db = mongoClient.getDB("test");
            DBCollection coll = db.getCollection("food_log");
            long lastDate = -1;
            DBObject last = null;
            for (DBObject dbObject : coll.find(new BasicDBObject("user_id",id))) {
                Date date = (Date) dbObject.get("date");
                if (last == null || lastDate < date.getTime()) {
                    lastDate = date.getTime();
                    last = dbObject;
                }
            }
            if (last == null) {
                return 0;
            }
            return (double) last.get("cal");
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        return 0;
    }
    public static double avgCaloriesInCityToday(String cityName) {
        try {
            MongoClient mongoClient = new MongoClient();
            DB db = mongoClient.getDB("test");
            DBCollection coll = db.getCollection("food_log");
            double cal = 0;
            List<String> usersCovered = new ArrayList<>();
            for (DBObject dbObject : coll.find()) {
                if (dbObject.containsField("city") && cityName.equalsIgnoreCase((String) dbObject.get("city"))) {
                    cal += (Double)dbObject.get("cal");
                    String userId = (String) dbObject.get("user_id");
                    if (!usersCovered.contains(userId)) {
                        usersCovered.add(userId);
                    }
                }
            }
            if (usersCovered.size() == 0) {
                return 0;
            }
            return cal/usersCovered.size();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        return 0;
    }
    public static ArrayList<String> getMessages(String userId) {
        ArrayList<String> messages = new ArrayList<String>();

        MongoClient mongoClient = new MongoClient();
        DB db = mongoClient.getDB("test");
        DBCollection coll = db.getCollection("food_log");
        DateTime dt = new DateTime();
        hours = dt.getHoursOfDay()-5;
        int currentCalories = caloriesToday(userId);
        int targetCalories = 0;
        if (hours > 0) {
            targetCalories = (2250/14)*hours;
        }

        if (currentCalories < targetCalories-50) {
            messages.add("Are you sure that's enough?");
        } else if (currentCalories > targetCalories+50) {
            messages.add("Watch out! You're passing your limit.")
        }

        String zipcode = "19148"; //change
        String metro = "philly"; //change
        Resty r = new Resty();
        try {
            NodeList results = r.xml("https://api.everyblock.com/content/"+metro+"/locations/" + zipcode + "/timeline/events/?token=6e54dcfff8edba4f8df29d93ee19aaececfe5589")
                    .get("root/results/list-item");
            for (int i=0; i<results.getLength(); i++){
                Node item = results.item(i);
                String description = results.item(i).getTextContent().toLowerCase();
                if (currentCalories < targetCalories-50) {
                    for (String e:eatMore) {
                        if (description.contains(e)) {
                            messages.add("SUGGESTED EVENT: "+item.getChildNodes().item(1).getTextContent());
                            break;
                        }
                    }
                } else if (currentCalories > targetCalories+50) {
                    for (String e:exerciseMore) {
                        if (description.contains(e)) {
                            messages.add("SUGGESTED EVENT: " + item.getChildNodes().item(1).getTextContent());
                            break;
                        }
                    }
                }
            }

            results = r.xml("https://api.everyblock.com/content/"+metro+"/topnews/events/?token=6e54dcfff8edba4f8df29d93ee19aaececfe5589")
                    .get("root/results/list-item");

            for (int i=0; i<results.getLength(); i++){
                Node item = results.item(i);
                String description = results.item(i).getTextContent().toLowerCase();
                if (currentCalories < targetCalories-50) {
                    for (String e:eatMore) {
                        if (description.contains(e)) {
                            messages.add("SUGGESTED EVENT: " + item.getChildNodes().item(1).getTextContent());
                            break;
                        }
                    }
                } else if (currentCalories > targetCalories+50) {
                    for (String e:exerciseMore) {
                        if (description.contains(e)) {
                            messages.add("SUGGESTED EVENT: "+item.getChildNodes().item(1).getTextContent());
                            break;
                        }
                    }
                }
            }
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return messages;
    }
}
