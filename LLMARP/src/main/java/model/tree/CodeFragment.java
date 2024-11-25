package model.tree;

import lombok.Getter;
import lombok.Setter;

@Getter
public class CodeFragment {
    private final int nodeId;
    private final int begin;
    private final int end;
    @Setter
    private String body;

    public CodeFragment(int nodeId, int begin, int end, String body) {
        this.nodeId = nodeId;
        this.begin = begin;
        this.end = end;
        this.body = body;
    }

    public static CodeFragment of(HalNode node,String text){
        int nodeId = node.getId();
        int begin = node.getPos();
        int end = node.getPos() + node.getLength();
        String body = text.substring(begin, end);
        return new CodeFragment(nodeId, begin, end, body);
    }
}
