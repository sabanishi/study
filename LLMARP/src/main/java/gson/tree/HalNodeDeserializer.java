package gson.tree;

import com.google.gson.*;
import model.tree.HalNode;

import java.lang.reflect.Type;

public class HalNodeDeserializer implements JsonDeserializer<HalNode> {
    @Override
    public HalNode deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        JsonObject jsonObject = json.getAsJsonObject();
        return HalNode.makeFromJson(jsonObject);
    }
}
