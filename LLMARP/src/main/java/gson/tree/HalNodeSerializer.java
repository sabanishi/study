package gson.tree;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import model.tree.HalNode;
import com.google.gson.JsonSerializer;

import java.lang.reflect.Type;

public class HalNodeSerializer implements JsonSerializer<HalNode>{
    @Override
    public JsonElement serialize(HalNode src, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject jsonObject = new JsonObject();
        src.makeJsonObject(jsonObject, context);
        return jsonObject;
    }
}