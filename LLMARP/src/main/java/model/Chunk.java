package model;

import com.github.gumtreediff.matchers.MappingStore;
import com.github.gumtreediff.matchers.Matchers;
import com.github.gumtreediff.tree.Tree;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.Value;
import model.tree.HalNode;
import model.tree.HalTreeNode;
import model.tree.NormalizationInfo;
import org.eclipse.jgit.diff.Edit;

import java.util.*;
import java.util.stream.Collectors;

@ToString(of = {"fileName", "oldStatement", "newStatement"})
@RequiredArgsConstructor
@Value
public class Chunk {
    String fileName;

    Statement oldStatement;
    Statement newStatement;

    Pattern originalPattern;

    List<Pattern> normalizedPatterns;

    public static Chunk of(final String fileName, final List<Statement> oldStatements, Tree oldAllTree, final List<Statement> newStatements, Tree newAllTree, Edit e) {
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

        Statement oldStatement = Statement.joint(oldSlice, Range.of(oldLineBegin, oldLineEnd), Range.of(oldCharBegin, oldCharEnd));
        Statement newStatement = Statement.joint(newSlice, Range.of(newLineBegin, newLineEnd), Range.of(newCharBegin, newCharEnd));

        String oldSource = oldStatements.stream().map(Statement::getRaw).collect(Collectors.joining("\n"));
        String newSource = newStatements.stream().map(Statement::getRaw).collect(Collectors.joining("\n"));

        Tree oldTree = extractSubTree(oldAllTree, oldStatement);
        Tree newTree = extractSubTree(newAllTree, newStatement);

        HalTreeNode oldTreeRoot = HalTreeNode.of(oldTree, oldSource);
        HalTreeNode newTreeRoot = HalTreeNode.of(newTree, newSource);

        Pattern originalPattern = Pattern.of(oldTreeRoot, newTreeRoot, new ArrayList<NormalizationInfo>());

        //originalPatternのNodeにIDを付与する
        int id = 0;
        MappingStore mapping = Matchers.getInstance().getMatcher().match(oldTree, newTree);
        for (HalNode oldNode : oldTreeRoot.preOrder()) {
            if (oldNode instanceof HalTreeNode oldTreeNode) {
                Tree newOriginalTree = mapping.getDstForSrc(oldTreeNode.getOriginal());
                if (newOriginalTree != null) {
                    HalNode newNode = newTreeRoot.searchByGumTree(newOriginalTree);
                    newNode.setId(id);
                }
            }

            oldNode.setId(id);
            id++;
        }

        for (HalNode newNode : newTreeRoot.preOrder()) {
            if (newNode.getId() >= 0) continue;
            newNode.setId(id);
            id++;
        }

        List<Pattern> normalizedPatterns = new ArrayList<>();

        Chunk chunk = new Chunk(fileName, oldStatement, newStatement, originalPattern, normalizedPatterns);

        //beforeとafterのASTが一致する時、正規化を行わない
        if(oldTree != null && oldTree.isIsomorphicTo(newTree)){
            return chunk;
        }

        chunk.normalize();
        return chunk;
    }

    private static Tree extractSubTree(Tree root, Statement statement) {
        Range range = statement.getChars();
        final int begin = range.getBegin();
        final int end = range.getEnd();

        Stack<Tree> stack = new Stack<>();
        stack.push(root);

        while (!stack.empty()) {
            Tree node = stack.pop();
            if (begin <= node.getPos() && node.getEndPos() <= end) {
                return node.deepCopy();
            }

            for (Tree child : node.getChildren()) {
                stack.push(child);
            }
        }

        return null;
    }

    private void normalize() {
        //徐々に正規化則を適用していく
        Set<Pattern> normalized = new HashSet<Pattern>();
        originalPattern.normalize(normalized);
        normalizedPatterns.addAll(normalized);
    }
}