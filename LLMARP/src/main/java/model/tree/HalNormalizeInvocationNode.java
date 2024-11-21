package model.tree;

import com.github.gumtreediff.tree.Tree;

public class HalNormalizeInvocationNode extends HalTreeNode {
    protected HalNormalizeInvocationNode(String type, String label, Tree original, int pos, int length) {
        super(type, label, original, pos, length);
    }

    public HalNormalizeInvocationNode() {
        super();
    }

    public static HalNormalizeInvocationNode of(HalTreeNode node) {
        String type = "NORMALIZED_METHOD_INVOCATION_ARGUMENTS";
        HalNormalizeInvocationNode normalizeNode = new HalNormalizeInvocationNode(type, node.getLabel(), node.getOriginal(), node.getPos(), node.getLength());
        normalizeNode.setId(node.getId());
        for (HalNode child : node.getChildren()) {
            normalizeNode.addChild(child);
        }
        return normalizeNode;
    }

    @Override
    public boolean equals(HalNode node) {
        if (!(node instanceof HalNormalizeInvocationNode treeNode)) {
            return false;
        }

        return this.getLabel().equals(treeNode.getLabel())
                && this.getType().equals(treeNode.getType())
                && this.getPos() == treeNode.getPos()
                && this.getLength() == treeNode.getLength();
    }

    @Override
    protected HalTreeNode copyMe() {
        return of(this);
    }

    @Override
    public String toHashString(int depth) {
        return "NORMALIZED_INVOCATION_ARGUMENTS" + super.toHashString(depth);
    }
}
