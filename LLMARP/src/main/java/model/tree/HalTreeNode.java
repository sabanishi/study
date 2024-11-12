package model.tree;

import com.github.gumtreediff.tree.Tree;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;

@Getter
public class HalTreeNode extends HalNode {
    protected String type;
    @Setter(AccessLevel.PUBLIC)
    protected String label;
    protected Tree original;

    protected HalTreeNode(String type, String label, Tree original,int pos,int length) {
        this.type = type;
        this.label = label;
        this.original = original;
        this.pos = pos;
        this.length = length;
    }

    public static HalTreeNode of(Tree tree) {
        HalTreeNode node = new HalTreeNode(tree.getType().toString(), tree.getLabel(), tree,tree.getPos(),tree.getLength());
        for (Tree child : tree.getChildren()) {
            node.addChild(HalTreeNode.of(child));
        }

        return node;
    }

    public static HalTreeNode of(HalTreeNode tree) {
        HalTreeNode node = new HalTreeNode(tree.getType(), tree.getLabel(), tree.original,tree.getPos(),tree.getLength());
        node.id = tree.id;
        return node;
    }

    public void addChild(HalNode child) {
        children.add(child);
        child.setParent(this);
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
    public HalNode deepCopy() {
        HalTreeNode node = of(this);
        for (HalNode child : children) {
            node.addChild(child.deepCopy());
        }
        return node;
    }

    @Override
    public boolean equals(HalNode node) {
        if (!(node instanceof HalTreeNode treeNode)) {
            return false;
        }

        return this.getLabel().equals(treeNode.getLabel())
                && this.getType().equals(treeNode.getType())
                && this.getPos() == treeNode.getPos()
                && this.getLength() == treeNode.getLength();
    }

    @Override
    public int hashCode() {
        int hash = calcMyHashCode();
        for (HalNode child : children) {
            hash += child.hashCode();
        }
        return hash;
    }

    private int calcMyHashCode(){
        return this.label.hashCode() + this.type.hashCode() + this.pos + this.length;
    }

    @Override
    protected boolean isSameTree(Tree tree) {
        return this.original.equals(tree);
    }
}
