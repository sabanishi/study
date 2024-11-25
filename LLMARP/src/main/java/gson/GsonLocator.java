package gson;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import gson.tree.HalNodeDeserializer;
import gson.tree.HalNodeSerializer;
import model.tree.*;

public class GsonLocator {
    private static Gson gson;

    private GsonLocator() {
        throw new IllegalStateException("GsonLocator is a Singleton class");
    }

    public static Gson getGson() {
        if (gson == null) {
            gson = createGson();
        }
        return gson;
    }

    private static Gson createGson() {
        GsonBuilder builder = new GsonBuilder();

        builder.registerTypeAdapter(HalNode.class, new HalNodeSerializer());
        builder.registerTypeAdapter(HalNode.class, new HalNodeDeserializer());
        builder.registerTypeAdapter(HalTreeNode.class, new HalNodeSerializer());
        builder.registerTypeAdapter(HalTreeNode.class, new HalNodeDeserializer());
        builder.registerTypeAdapter(HalNormalizeNode.class, new HalNodeSerializer());
        builder.registerTypeAdapter(HalNormalizeNode.class, new HalNodeDeserializer());
        builder.registerTypeAdapter(HalNormalizeInvocationNode.class, new HalNodeSerializer());
        builder.registerTypeAdapter(HalNormalizeInvocationNode.class, new HalNodeDeserializer());
        builder.registerTypeAdapter(HalEmptyNode.class, new HalNodeSerializer());
        builder.registerTypeAdapter(HalEmptyNode.class, new HalNodeDeserializer());

        return builder.create();
    }
}
