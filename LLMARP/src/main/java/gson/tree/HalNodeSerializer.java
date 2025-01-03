package gson.tree;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import model.tree.HalNode;

import java.lang.reflect.Type;

public class HalNodeSerializer implements JsonSerializer<HalNode> {
    @Override
    public JsonElement serialize(HalNode src, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject jsonObject = new JsonObject();
        src.makeToJson(jsonObject, context);
        return jsonObject;
    }
}