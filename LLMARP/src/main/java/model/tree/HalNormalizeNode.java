package model.tree;

import com.github.gumtreediff.tree.Tree;
import org.apache.commons.lang3.StringUtils;

public class HalNormalizeNode extends HalTreeNode{
    protected HalNormalizeNode(String type, String label, Tree original, int pos, int length){
        super(type, label, original, pos, length);
    }

    protected HalNormalizeNode(){
        super();
    }

    public static HalNormalizeNode of(HalTreeNode node){
        String label = "$V" + node.getId();
        HalNormalizeNode normalizeNode = new HalNormalizeNode(node.getType(), label, node.getOriginal(), node.getPos(), node.getLength());
        normalizeNode.setId(node.getId());
        for(HalNode child: node.getChildren()){
            normalizeNode.addChild(child);
        }
        return normalizeNode;
    }

    @Override
    public boolean equals(HalNode node) {
        if (!(node instanceof HalNormalizeNode treeNode)) {
            return false;
        }

        return this.getLabel().equals(treeNode.getLabel())
                && this.getType().equals(treeNode.getType())
                && this.getPos() == treeNode.getPos()
                && this.getLength() == treeNode.getLength();
    }

    @Override
    protected HalTreeNode copyMe(){
        return of(this);
    }

    @Override
    public String toHashString(int depth){
        return "NORMALIZED_NODE"+super.toHashString(depth);
    }
}
