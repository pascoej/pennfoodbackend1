package pennfood.PennFood;

/**
 * Created by john on 9/5/15.
 */
public class Food {
    private String name;
    private String id;
    private double calories;

    public Food(String name, String id, double calories) {
        this.name = name;
        this.id = id;
        this.calories = calories;
    }

    public void setCalories(double calories) {
        this.calories = calories;
    }

    public String getName() {
        return name;
    }

    public String getId() {
        return id;
    }

    public double getCalories() {
        return calories;
    }
}
