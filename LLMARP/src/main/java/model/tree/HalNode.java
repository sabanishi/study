package model.tree;

import com.github.gumtreediff.tree.Tree;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import model.Hash;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Getter
public abstract class HalNode {
    @Getter(lazy = true)
    private final Hash hash = Hash.of(toHashString(0));
    @Getter(lazy = true)
    private final String text = toHashString(0);
    @Setter(AccessLevel.PUBLIC)
    protected int id = -1;
    protected int pos;
    protected int length;
    protected List<HalNode> children = new ArrayList<>();
    protected HalTreeNode parent;
    @Setter(AccessLevel.PUBLIC)
    protected String rawText;
    @Getter(lazy = true)
    private final String normalizeText = makeNormalizeText();

    public static HalNode makeFromJson(JsonObject jsonObject) {
        String className = jsonObject.get("class").getAsString();
        HalNode node;
        switch (className) {
            case "HalTreeNode":
                node = new HalTreeNode();
                break;
            case "HalNormalizeNode":
                node = new HalNormalizeNode();
                break;
            case "HalNormalizeInvocationNode":
                node = new HalNormalizeInvocationNode();
                break;
            case "HalEmptyNode":
                node = new HalEmptyNode();
                break;
            default:
                log.error("Unknown class: {}", className);
                return null;
        }

        node.id = jsonObject.get("id").getAsInt();
        node.pos = jsonObject.get("pos").getAsInt();
        node.length = jsonObject.get("length").getAsInt();
        node.rawText = jsonObject.get("raw_text").getAsString();

        List<JsonElement> childElements = jsonObject.getAsJsonArray("children").asList();

        HalTreeNode halNode = (HalTreeNode) node;
        for (JsonElement element : childElements) {
            JsonObject childObject = element.getAsJsonObject();
            HalNode child = makeFromJson(childObject);
            if (child == null) continue;
            child.parent = halNode;
            node.children.add(child);
        }

        node.makeFromJsonInternal(jsonObject);

        return node;
    }

    public Iterable<HalNode> preOrder() {
        List<HalNode> list = new ArrayList<>();
        return this.preOrder(list);
    }

    protected List<HalNode> preOrder(List<HalNode> list) {
        list.add(this);
        for (HalNode child : children) {
            list = child.preOrder(list);
        }
        return list;
    }

    public HalNode searchByGumTree(Tree target) {
        if (isSameTree(target)) {
            return this;
        }
        for (HalNode child : children) {
            HalNode result = child.searchByGumTree(target);
            if (result != null) {
                return result;
            }
        }
        return null;
    }

    public HalNode searchById(int id) {
        if (this.id == id) {
            return this;
        }
        for (HalNode child : children) {
            HalNode result = child.searchById(id);
            if (result != null) {
                return result;
            }
        }
        return null;
    }

    public boolean replace(HalNode oldNode, HalNode newNode) {
        for (int i = 0; i < children.size(); i++) {
            HalNode child = children.get(i);
            if(child.getId() == oldNode.getId()){
                children.set(i, newNode);
                newNode.parent = (HalTreeNode) this;
                return true;
            }
            if (child.replace(oldNode, newNode)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String toString() {
        return toString(0);
    }

    public void makeToJson(JsonObject jsonObject, JsonSerializationContext context) {
        jsonObject.addProperty("class", getClass().getSimpleName());
        jsonObject.addProperty("id", id);
        jsonObject.addProperty("pos", pos);
        jsonObject.addProperty("length", length);
        jsonObject.addProperty("hash", getHash().getName());
        jsonObject.addProperty("raw_text", rawText);

        if (parent != null) {
            jsonObject.addProperty("parent", parent.getId());
        }

        JsonArray childList = new JsonArray();
        for (HalNode child : children) {
            childList.add(context.serialize(child));
        }
        jsonObject.add("children", childList);
        makeToJsonInternal(jsonObject, context);
    }

    public String makeNormalizeText() {
        String baseText = rawText.substring(pos, pos + length);
        return makeNormalizeLoop(baseText);
    }

    private String makeNormalizeLoop(String baseText) {
        String newText = makeNormalizeTextInternal(baseText);
        baseText = baseText.replace(baseText, newText);

        for (HalNode child : children) {
            String childOriginalText = rawText.substring(child.getPos(), child.getPos() + child.getLength());
            String childNewText = child.makeNormalizeLoop(childOriginalText);
            baseText = baseText.replace(childOriginalText, childNewText);
        }

        return baseText;
    }

    @Override
    public boolean equals(Object obj) {
        //objがHalNodeのサブクラスである時
        if (!(obj instanceof HalNode node)) return false;

        if (!equalsInternal(obj)) return false;

        if (children.size() != node.children.size()) {
            return false;
        }

        for (HalNode child : children) {
            boolean found = false;
            for (HalNode otherChild : node.children) {
                if (child.equals(otherChild)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                return false;
            }
        }

        return true;
    }

    public abstract String toString(int depth);

    public abstract String toHashString(int depth);

    public abstract HalNode deepCopy();

    protected abstract boolean equalsInternal(Object obj);

    public abstract int hashCode();

    protected abstract boolean isSameTree(Tree tree);

    protected abstract void makeToJsonInternal(JsonObject jsonObject, JsonSerializationContext context);

    protected abstract void makeFromJsonInternal(JsonObject jsonObject);

    protected abstract String makeNormalizeTextInternal(String baseText);

}
