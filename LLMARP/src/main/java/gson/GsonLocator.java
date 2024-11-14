package gson;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import gson.tree.HalNodeDeserializer;
import gson.tree.HalNodeSerializer;
import model.tree.HalNode;
import model.tree.HalNormalizeNode;
import model.tree.HalTreeNode;

public class GsonLocator {
    private GsonLocator(){
        throw new IllegalStateException("GsonLocator is a Singleton class");
    }

    private static Gson gson;

    public static Gson getGson(){
        if(gson == null){
            gson = createGson();
        }
        return gson;
    }

    private static Gson createGson(){
        GsonBuilder builder = new GsonBuilder();

        builder.registerTypeAdapter(HalNode.class, new HalNodeSerializer());
        builder.registerTypeAdapter(HalNode.class, new HalNodeDeserializer());
        builder.registerTypeAdapter(HalTreeNode.class, new HalNodeSerializer());
        builder.registerTypeAdapter(HalTreeNode.class, new HalNodeDeserializer());
        builder.registerTypeAdapter(HalNormalizeNode.class, new HalNodeSerializer());
        builder.registerTypeAdapter(HalNormalizeNode.class, new HalNodeDeserializer());

        return builder.create();
    }
}
