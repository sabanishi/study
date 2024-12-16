package model.tree;

import com.github.gumtreediff.tree.Tree;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import org.apache.commons.lang3.StringUtils;
import util.Pair;

import java.util.List;

public class HalRootNode extends HalNode{

    public static HalRootNode of(List<Pair<Tree,String>> child,String rawText){
        HalRootNode root = new HalRootNode();
        for(Pair<Tree,String> pair:child){
            HalTreeNode childNode = HalTreeNode.of(pair.getFirst(),pair.getSecond());
            root.addChild(childNode);
        }

        int pos = root.getChildren().get(0).getPos();
        int length = root.getChildren().get(root.getChildren().size()-1).getPos() + root.getChildren().get(root.getChildren().size()-1).getLength() - pos;

        root.rawText = rawText;
        root.pos = pos;
        root.length = length;

        return root;
    }

    public static HalRootNode of(HalRootNode tree) {
        HalRootNode node = new HalRootNode();
        node.id = tree.getId();
        node.rawText = tree.getRawText();
        node.pos = tree.getPos();
        node.length = tree.getLength();
        return node;
    }

    @Override
    public String toString(int depth) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("%s%d %s\n", StringUtils.repeat("  ", depth), id, "ROOT"));
        for (HalNode child : children) {
            sb.append(child.toString(depth + 1));
        }
        return sb.toString();
    }

    @Override
    public String toHashString(int depth) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("%s%d %s\n", StringUtils.repeat("  ", depth), id, "ROOT"));
        for (HalNode child : children) {
            sb.append(child.toHashString(depth + 1));
        }
        return sb.toString();
    }

    @Override
    public HalNode deepCopy() {
        HalRootNode node = HalRootNode.of(this);
        node.setId(id);
        for (HalNode child : children) {
            node.addChild(child.deepCopy());
        }
        return node;
    }

    @Override
    protected boolean equalsInternal(Object obj) {
        return (obj instanceof HalRootNode);
    }

    @Override
    public int hashCode() {
        int hash = 0;
        for (HalNode child : children) {
            hash += child.hashCode();
        }
        return hash;
    }

    @Override
    protected boolean isSameTree(Tree tree) {
        return false;
    }

    @Override
    protected void makeToJsonInternal(JsonObject jsonObject, JsonSerializationContext context) {
    }

    @Override
    protected void makeFromJsonInternal(JsonObject jsonObject) {
    }

    @Override
    protected String makeNormalizeTextInternal(String baseText) {
        return "";
    }
}