package model.tree;

import com.github.gumtreediff.tree.Tree;

public class HalEmptyNode extends HalTreeNode{
    protected HalEmptyNode(String type,String label,Tree original, int pos, int length,String rawText) {
        super(type, label, original, pos, length,rawText);
    }

    protected HalEmptyNode() {
        super();
    }

    public static HalEmptyNode of(HalTreeNode node) {
        String type = "EMPTY_NODE";
        String label = "[$V" + node.getId() + "]";
        HalEmptyNode emptyNode = new HalEmptyNode(type, label, node.getOriginal(), node.getPos(), node.getLength(), node.getRawText());
        emptyNode.setId(node.getId());
        for (HalNode child : node.getChildren()) {
            emptyNode.addChild(child);
        }
        return emptyNode;
    }

    @Override
    public boolean equalsInternal(Object obj) {
        if (!(obj instanceof HalEmptyNode treeNode)) {
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
    public String makeNormalizeTextInternal(String rawText) {
        return rawText.replace(rawText, getLabel());
    }
}
