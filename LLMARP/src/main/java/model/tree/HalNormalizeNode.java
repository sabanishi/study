package model.tree;

import com.github.gumtreediff.tree.Tree;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;

public class HalNormalizeNode extends HalTreeNode {
    private String originalLabel;

    protected HalNormalizeNode(String type, String label, Tree original, int pos, int length, String rawText, String originalLabel) {
        super(type, label, original, pos, length, rawText);
        this.originalLabel = originalLabel;
    }

    protected HalNormalizeNode() {
        super();
    }

    public static HalNormalizeNode of(HalTreeNode node) {
        String label = "$V" + node.getId();
        HalNormalizeNode normalizeNode = new HalNormalizeNode(node.getType(), label, node.getOriginal(), node.getPos(), node.getLength(), node.getRawText(), node.getLabel());
        normalizeNode.setId(node.getId());
        for (HalNode child : node.getChildren()) {
            normalizeNode.addChild(child);
        }
        return normalizeNode;
    }

    @Override
    public boolean equalsInternal(Object obj) {
        if (!(obj instanceof HalNormalizeNode treeNode)) {
            return false;
        }

        return this.getLabel().equals(treeNode.getLabel())
                && this.getType().equals(treeNode.getType());
    }

    @Override
    protected HalTreeNode copyMe() {
        return of(this);
    }

    @Override
    protected void makeToJsonInternal(JsonObject jsonObject, JsonSerializationContext context) {
        super.makeToJsonInternal(jsonObject, context);
        jsonObject.addProperty("original_text", originalLabel);
    }

    @Override
    protected void makeFromJsonInternal(JsonObject jsonObject) {
        super.makeFromJsonInternal(jsonObject);
        originalLabel = jsonObject.get("original_text").getAsString();
    }

    @Override
    public String makeNormalizeTextInternal(String rawText) {
        return rawText.replace(rawText, getLabel());
    }
}
