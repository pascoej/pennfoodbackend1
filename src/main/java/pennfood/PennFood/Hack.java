package pennfood.PennFood;

import com.clarifai.api.Tag;

import java.util.List;

/**
 * Created by john on 9/5/15.
 */
public class Hack {
    private static double cutoff =0.80;
    private List<String> tags;
    private String result;

    public Hack(List<String> tags, String result) {
        this.tags = tags;
        this.result = result;
    }
    public boolean isSatis(List<Tag> inTag) {
        int satis = 0;
        for (String tag : tags) {
            for (Tag iTag : inTag) {
                if (iTag.getName().equalsIgnoreCase(tag) && iTag.getProbability() > cutoff) {
                    satis += 1;
                }
            }
        }
        return (satis >= tags.size());
    }

    public String getResult() {
        return result;
    }
}
