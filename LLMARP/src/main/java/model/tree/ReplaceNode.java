package model.tree;

import com.github.gumtreediff.tree.Tree;

public class ReplaceNode extends HalTreeNode{
    private String replaceText;

    private ReplaceNode(String type, String label, Tree original, int pos, int length, String rawText) {
        super(type, label, original, pos, length, rawText);
    }

    public static ReplaceNode of(HalTreeNode node,String replaceText) {
        String type = "REPLACE_NODE[" + node.getType() + "]";
        ReplaceNode replaceNode = new ReplaceNode(type, replaceText, node.getOriginal(), node.getPos(), node.getLength(), node.getRawText());
        replaceNode.setId(node.getId());
        replaceNode.replaceText = replaceText;
        return replaceNode;
    }

    @Override
    public boolean equalsInternal(Object obj) {
        if (!(obj instanceof ReplaceNode treeNode)) {
            return false;
        }

        return this.getLabel().equals(treeNode.getLabel())
                && this.getType().equals(treeNode.getType());
    }


    @Override
    protected String makeNormalizeTextInternal(String baseText) {
        return replaceText;
    }
}
