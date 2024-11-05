package model;

import com.github.gumtreediff.tree.Tree;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import org.eclipse.jgit.diff.Edit;

import java.util.List;
import java.util.Stack;

@ToString(of = {"fileName","oldStatement","newStatement"})
@RequiredArgsConstructor
public class Chunk {
    @Getter
    private final String fileName;

    @Getter
    private final Statement oldStatement;
    @Getter
    private final Tree oldAllTree;
    @Getter(lazy = true)
    private final Tree oldTree = extractSubTree(oldAllTree,oldStatement);

    @Getter
    private final Statement newStatement;
    @Getter
    private final Tree newAllTree;
    @Getter(lazy = true)
    private final Tree newTree = extractSubTree(newAllTree,newStatement);

    public static Chunk of(final String fileName, final List<Statement> oldStatements, Tree oldAllTree,final List<Statement> newStatements, Tree newAllTree,Edit e){
        final List<Statement> oldSlice = oldStatements.subList(e.getBeginA(), e.getEndA());
        final List<Statement> newSlice = newStatements.subList(e.getBeginB(), e.getEndB());

        int oldLineBegin,oldLineEnd,newLineBegin,newLineEnd;
        int oldCharBegin,oldCharEnd,newCharBegin,newCharEnd;

        if(oldSlice.isEmpty()){
            oldLineBegin = e.getBeginA() == oldStatements.size()
                    ? oldStatements.get(oldStatements.size()-1).getLines().getEnd()
                    : oldStatements.get(e.getBeginA()).getLines().getBegin();
            oldLineEnd = oldLineBegin;

            oldCharBegin = e.getBeginA() == oldStatements.size()
                    ? oldStatements.get(oldStatements.size()-1).getChars().getEnd()
                    : oldStatements.get(e.getBeginA()).getChars().getBegin();
            oldCharEnd = oldCharBegin;
        }else{
            oldLineBegin = oldSlice.get(0).getLines().getBegin();
            oldLineEnd = oldSlice.get(oldSlice.size()-1).getLines().getEnd();

            oldCharBegin = oldSlice.get(0).getChars().getBegin();
            oldCharEnd = oldSlice.get(oldSlice.size()-1).getChars().getEnd();
        }

        if(newSlice.isEmpty()) {
            newLineBegin = e.getBeginB() == newStatements.size()
                    ? newStatements.get(newStatements.size() - 1).getLines().getEnd()
                    : newStatements.get(e.getBeginB()).getLines().getBegin();
            newLineEnd = newLineBegin;

            newCharBegin = e.getBeginB() == newStatements.size()
                    ? newStatements.get(newStatements.size() - 1).getChars().getEnd()
                    : newStatements.get(e.getBeginB()).getChars().getBegin();
            newCharEnd = newCharBegin;
        }else{
            newLineBegin = newSlice.get(0).getLines().getBegin();
            newLineEnd = newSlice.get(newSlice.size()-1).getLines().getEnd();

            newCharBegin = newSlice.get(0).getChars().getBegin();
            newCharEnd = newSlice.get(newSlice.size()-1).getChars().getEnd();
        }

        Statement oldStatement = Statement.joint(oldSlice, Range.of(oldLineBegin, oldLineEnd),Range.of(oldCharBegin,oldCharEnd));
        Statement newStatement = Statement.joint(newSlice, Range.of(newLineBegin, newLineEnd),Range.of(newCharBegin,newCharEnd));

        return new Chunk(fileName, oldStatement,oldAllTree, newStatement,newAllTree);
    }

    private Tree extractSubTree(Tree allTree,Statement statement){
        Range range = statement.getChars();
        final int begin = range.getBegin();
        final int end = range.getEnd();

        Stack<Tree> stack = new Stack<>();
        stack.push(allTree);

        while(!stack.empty()){
            Tree node = stack.pop();
            if(begin <= node.getPos() && node.getEndPos() <= end){
                return node.deepCopy();
            }

            if(node.getPos() <= begin && end <= node.getEndPos()){
                for(Tree child: node.getChildren()){
                    stack.push(child);
                }
            }
        }

        return null;
    }
}