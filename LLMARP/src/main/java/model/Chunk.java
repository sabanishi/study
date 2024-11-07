package model;

import com.github.gumtreediff.tree.Tree;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.Value;
import org.eclipse.jgit.diff.Edit;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

@ToString(of = {"fileName","oldStatement","newStatement"})
@RequiredArgsConstructor
@Value
public class Chunk {
    String fileName;

    Statement oldStatement;
    Statement newStatement;

    Pattern originalPattern;

    List<Pattern> normalizedPatterns;

    public static Chunk of(final String fileName, final List<Statement> oldStatements, Tree oldAllTree,final List<Statement> newStatements, Tree newAllTree,Edit e){
        final List<Statement> oldSlice = oldStatements.subList(e.getBeginA(), e.getEndA());
        final List<Statement> newSlice = newStatements.subList(e.getBeginB(), e.getEndB());

        int oldLineBegin = oldSlice.get(0).getLines().getBegin();
        int oldLineEnd = oldSlice.get(oldSlice.size() - 1).getLines().getEnd();
        int newLineBegin = newSlice.get(0).getLines().getBegin();
        int newLineEnd = newSlice.get(newSlice.size() - 1).getLines().getEnd();

        int oldCharBegin = oldSlice.get(0).getChars().getBegin();
        int oldCharEnd = oldSlice.get(oldSlice.size() - 1).getChars().getEnd();
        int newCharBegin = newSlice.get(0).getChars().getBegin();
        int newCharEnd = newSlice.get(newSlice.size() - 1).getChars().getEnd();

        Statement oldStatement = Statement.joint(oldSlice, Range.of(oldLineBegin, oldLineEnd),Range.of(oldCharBegin,oldCharEnd));
        Statement newStatement = Statement.joint(newSlice, Range.of(newLineBegin, newLineEnd),Range.of(newCharBegin,newCharEnd));

        Pattern originalPattern = Pattern.of(extractSubTree(oldAllTree,oldStatement),extractSubTree(newAllTree,newStatement));

        List<Pattern> normalizedPatterns = new ArrayList<>();
        normalizedPatterns.add(originalPattern);

        return new Chunk(fileName, oldStatement, newStatement,originalPattern,normalizedPatterns);
    }

    private static Tree extractSubTree(Tree root,Statement statement){
        Range range = statement.getChars();
        final int begin = range.getBegin();
        final int end = range.getEnd();

        Stack<Tree> stack = new Stack<>();
        stack.push(root);

        while(!stack.empty()){
            Tree node = stack.pop();
            if(begin <= node.getPos() && node.getEndPos() <= end){
                return node.deepCopy();
            }

            for(Tree child: node.getChildren()){
                stack.push(child);
            }
        }

        return null;
    }
}