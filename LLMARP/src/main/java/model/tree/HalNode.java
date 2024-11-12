package model.tree;

import com.github.gumtreediff.tree.Tree;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
public abstract class HalNode {
    @Setter(AccessLevel.PROTECTED)
    private HalTreeNode parent;
    protected List<HalNode> children = new ArrayList<>();
    protected int pos;
    protected int length;
    @Setter(AccessLevel.PUBLIC)
    protected int id = -1;

    public List<HalNode> preOrder() {
        List<HalNode> list = new ArrayList<>();
        return this.preOrder(list);
    }

    protected List<HalNode> preOrder(List<HalNode> list) {
        list.add(this);
        for (HalNode child : children) {
            list = child.preOrder(list);
        }
        return list;
    }

    public HalNode searchByGumTree(Tree target) {
        if (isSameTree(target)) {
            return this;
        }
        for (HalNode child : children) {
            HalNode result = child.searchByGumTree(target);
            if (result != null) {
                return result;
            }
        }
        return null;
    }

    public HalNode searchById(int id){
        if(this.id == id){
            return this;
        }
        for(HalNode child:children){
            HalNode result = child.searchById(id);
            if(result != null){
                return result;
            }
        }
        return null;
    }

    public boolean replace(HalNode oldNode,HalNode newNode){
        for(int i=0;i<children.size();i++){
            HalNode child = children.get(i);
            if(child.equals(oldNode)){
                children.set(i,newNode);
                return true;
            }
            if(child.replace(oldNode,newNode)){
                return true;
            }
        }
        return false;
    }

    @Override
    public String toString(){
        return toString(0);
    }

    public abstract String toString(int depth);

    public abstract HalNode deepCopy();

    public abstract boolean equals(HalNode tree);

    public abstract int hashCode();

    protected abstract boolean isSameTree(Tree tree);
}
