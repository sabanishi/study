package gson.tree;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import model.tree.HalNode;

import java.lang.reflect.Type;

public class HalNodeDeserializer implements JsonDeserializer<HalNode> {
    @Override
    public HalNode deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        return null;
    }
}
