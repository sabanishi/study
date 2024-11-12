package model.tree;

import com.github.gumtreediff.tree.Tree;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

@Getter
public class HalTreeNode extends HalNode{
    private String type;
    private String label;
    private Tree original;

    public static HalTreeNode of(Tree tree){
        HalTreeNode node = new HalTreeNode();
        node.type = tree.getType().toString();
        node.label = tree.getLabel();
        node.original = tree;
        node.pos = tree.getPos();
        node.length = tree.getLength();

        for(Tree child:tree.getChildren()){
            node.addChild(HalTreeNode.of(child));
        }

        return node;
    }

    public static HalTreeNode of(HalTreeNode tree){
        HalTreeNode node = of(tree.original);
        node.id = tree.id;
        return node;
    }

    public void addChild(HalNode child){
        children.add(child);
        child.setParent(this);
    }

    @Override
    public String toString(int depth) {
        String text = String.format("%s%d %s \"%s\"\n", StringUtils.repeat("  ", depth), id, type, label);
        for(HalNode child:children){
            text = text.concat(child.toString(depth+1));
        }
        return text;
    }

    @Override
    public HalNode deepCopy() {
        HalTreeNode node = of(this);
        for(HalNode child:children){
            node.addChild(child.deepCopy());
        }
        return node;
    }

    @Override
    public boolean equals(HalNode tree) {
        return false;
    }

    @Override
    protected boolean isSameTree(Tree tree) {
        return this.original.equals(tree);
    }
}
