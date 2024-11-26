package model.tree;

import com.github.gumtreediff.tree.Tree;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

@Getter
public class HalTreeNode extends HalNode {
    protected String type;
    protected String label;
    protected Tree original;

    protected HalTreeNode() {
    }

    protected HalTreeNode(String type, String label, Tree original, int pos, int length, String rawText) {
        this.type = type;
        this.label = label;
        this.original = original;
        this.pos = pos;
        this.length = length;
        this.rawText = rawText;
    }

    public static HalTreeNode of(Tree tree, String rawText) {
        HalTreeNode node = new HalTreeNode(tree.getType().toString(), tree.getLabel(), tree, tree.getPos(), tree.getLength(), rawText);
        for (Tree child : tree.getChildren()) {
            node.addChild(HalTreeNode.of(child, rawText));
        }

        return node;
    }

    public static HalTreeNode of(HalTreeNode tree, String rawText) {
        HalTreeNode node = new HalTreeNode(tree.getType(), tree.getLabel(), tree.original, tree.getPos(), tree.getLength(), rawText);
        node.id = tree.id;
        return node;
    }

    public void addChild(HalNode child) {
        children.add(child);
        child.parent = this;
    }

    @Override
    public String toString(int depth) {
        String text = String.format("%s%d %s \"%s\"\n", StringUtils.repeat("  ", depth), id, type, label);
        for (HalNode child : children) {
            text = text.concat(child.toString(depth + 1));
        }
        return text;
    }

    @Override
    public String toHashString(int depth) {
        String text = String.format("%s%d %s \"%s\"\n", StringUtils.repeat("  ", depth), id, type, label);
        for (HalNode child : children) {
            text = text.concat(child.toHashString(depth + 1));
        }
        return text;
    }

    @Override
    public HalNode deepCopy() {
        HalTreeNode node = copyMe();
        for (HalNode child : children) {
            node.addChild(child.deepCopy());
        }
        return node;
    }

    @Override
    public boolean equalsInternal(Object obj) {
        if (!(obj instanceof HalTreeNode treeNode)) {
            return false;
        }

        return this.getLabel().equals(treeNode.getLabel())
                && this.getType().equals(treeNode.getType());
    }

    @Override
    public int hashCode() {
        int hash = calcMyHashCode();
        for (HalNode child : children) {
            hash += child.hashCode();
        }
        return hash;
    }

    private int calcMyHashCode() {
        return this.label.hashCode() + this.type.hashCode() + this.length;
    }

    @Override
    protected boolean isSameTree(Tree tree) {
        return this.original.equals(tree);
    }

    @Override
    protected void makeToJsonInternal(JsonObject jsonObject, JsonSerializationContext context) {
        jsonObject.addProperty("type", type);
        jsonObject.addProperty("label", label);
    }

    @Override
    protected void makeFromJsonInternal(JsonObject jsonObject) {
        type = jsonObject.get("type").getAsString();
        label = jsonObject.get("label").getAsString();
    }

    protected HalTreeNode copyMe() {
        return of(this, rawText);
    }

    @Override
    protected String makeNormalizeTextInternal(String oldText) {
        return oldText;
    }
}
