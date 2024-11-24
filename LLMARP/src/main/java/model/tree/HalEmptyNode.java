package model.tree;

import com.github.gumtreediff.tree.Tree;

public class HalEmptyNode extends HalTreeNode{
    protected HalEmptyNode(Tree original, int pos, int length) {
        super("", "", original, pos, length);
    }

    protected HalEmptyNode() {
        super();
    }

    public static HalEmptyNode of(HalTreeNode node) {
        HalEmptyNode emptyNode = new HalEmptyNode(node.getOriginal(), node.getPos(), node.getLength());
        emptyNode.setId(node.getId());
        for (HalNode child : node.getChildren()) {
            emptyNode.addChild(child);
        }
        return emptyNode;
    }

    @Override
    public boolean equals(HalNode node) {
        if (!(node instanceof HalEmptyNode treeNode)) {
            return false;
        }

        return this.getPos() == treeNode.getPos()
                && this.getLength() == treeNode.getLength();
    }

    @Override
    protected HalTreeNode copyMe() {
        return of(this);
    }

    @Override
    public String toHashString(int depth) {
        return "EMPTY_NODE" + super.toHashString(depth);
    }
}
