package pennfood.PennFood;

import com.mongodb.BasicDBObject;

import java.util.Date;

/**
 * Created by john on 9/5/15.
 */
public class Food extends BasicDBObject {

    public String getName() {
        return getString("name");
    }
    public Date getDate() {
        return getDate("date");
    }
    public void setDate(Date date) {
        put("date",date);
    }

    public String getId() {
        return getString("id");
    }
    public String getGroup() {
        return getString("group");
    }
    public void setGroup(String group) {
        put("group",group);
    }
    public double getCalories() {
        return getDouble("cal");
    }

    public String getUserId() {
        return getString("user_id");
    }
    public void setUserId(String userId) {
        put("user_id",userId);
    }
    public void setId(String id) {
        put("user_id",id);
    }public void setCalories(double cal) {
        put("cal",cal);
    }
    public void setName(String name) {
        put("name",name);
    }
    public void setCity(String city) {
        put("city",city);
    }
    public String getCity() {
        return getString("city");
    }
}
