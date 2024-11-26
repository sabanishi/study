package model.tree;

import com.github.gumtreediff.tree.Tree;

public class HalNormalizeInvocationNode extends HalTreeNode {
    protected HalNormalizeInvocationNode(String type, String label, Tree original, int pos, int length, String rawText) {
        super(type, label, original, pos, length, rawText);
    }

    public HalNormalizeInvocationNode() {
        super();
    }

    public static HalNormalizeInvocationNode of(HalTreeNode node) {
        String type = "NORMALIZED_METHOD_INVOCATION_ARGUMENTS";
        String label = "<$V" + node.getId() + ">";
        HalNormalizeInvocationNode normalizeNode = new HalNormalizeInvocationNode(type, label, node.getOriginal(), node.getPos(), node.getLength(), node.getRawText());
        normalizeNode.setId(node.getId());
        for (HalNode child : node.getChildren()) {
            normalizeNode.addChild(child);
        }
        return normalizeNode;
    }

    @Override
    public boolean equalsInternal(Object obj) {
        if (!(obj instanceof HalNormalizeInvocationNode treeNode)) {
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
