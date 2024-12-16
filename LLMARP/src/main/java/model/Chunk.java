package model;

import com.github.gumtreediff.matchers.MappingStore;
import com.github.gumtreediff.matchers.Matchers;
import com.github.gumtreediff.tree.Tree;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.Value;
import model.tree.HalNode;
import model.tree.HalRootNode;
import model.tree.HalTreeNode;
import model.tree.NormalizationInfo;
import org.eclipse.jgit.diff.Edit;
import util.Pair;

import java.util.*;
import java.util.stream.Collectors;

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

        Stack<Tree> oldTreeStack = extractSubTree(oldAllTree, oldStatement);
        Stack<Tree> newTreeStack = extractSubTree(newAllTree, newStatement);

        //oldTreeまたはnewTreeがnullの場合、正規化を行わない
        if (oldTreeStack.empty()|| newTreeStack.empty()) {
            return new Chunk(fileName, oldStatement, newStatement, null, new ArrayList<Pattern>());
        }

        List<Pair<Tree,String>> oldInfo = new ArrayList<>();
        while(!oldTreeStack.isEmpty()){
            Tree tree = oldTreeStack.pop();
            //treeの範囲に対応したStatementを切り出す
            String rawText = oldSource.substring(tree.getPos(),tree.getEndPos());
            oldInfo.add(Pair.of(tree,rawText));
        }

        List<Pair<Tree,String>> newInfo = new ArrayList<>();
        while(!newTreeStack.isEmpty()){
            Tree tree = newTreeStack.pop();
            //treeの範囲に対応したStatementを切り出す
            String rawText = newSource.substring(tree.getPos(),tree.getEndPos());
            newInfo.add(Pair.of(tree,rawText));
        }

        HalRootNode oldTreeRoot = HalRootNode.of(oldInfo,oldSource);
        HalRootNode newTreeRoot = HalRootNode.of(newInfo,newSource);

        //originalPatternのNodeにIDを付与する
        int id = 1;
        oldTreeRoot.setId(0);
        newTreeRoot.setId(0);
        MappingStore mapping = Matchers.getInstance().getMatcher().match(oldAllTree, newAllTree);
        for (HalNode oldNode : oldTreeRoot.preOrder()) {
            if(oldNode instanceof HalRootNode)continue;
            if (oldNode instanceof HalTreeNode oldTreeNode) {
                Tree newOriginalTree = mapping.getDstForSrc(oldTreeNode.getOriginal());
                if (newOriginalTree != null) {
                    //ノードがSimpleNameの時、同じLabelでなければ同じIDとして扱わない
                    if (!oldTreeNode.getType().equals("SimpleName")
                            || oldTreeNode.getLabel().equals(newOriginalTree.getLabel())) {
                        HalNode newNode = newTreeRoot.searchByGumTree(newOriginalTree);
                        if(newNode!=null){
                            newNode.setId(id);
                        }
                    }
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

        Pattern originalPattern = Pattern.of(oldTreeRoot, newTreeRoot, new ArrayList<NormalizationInfo>(),false);

        List<Pattern> normalizedPatterns = new ArrayList<>();

        Chunk chunk = new Chunk(fileName, oldStatement, newStatement, originalPattern, normalizedPatterns);

        //beforeとafterのASTが一致する時、正規化を行わない
        if(oldTreeRoot.getChildren().size()==newTreeRoot.getChildren().size()){
            boolean isSame = true;
            for(int i=0;i<oldTreeRoot.getChildren().size();i++){
                Tree oldTree = ((HalTreeNode)(oldTreeRoot.getChildren().get(i))).getOriginal();
                Tree newTree = ((HalTreeNode)(newTreeRoot.getChildren().get(i))).getOriginal();
                if(oldTree == null || !oldTree.isIsomorphicTo(newTree)){
                    isSame = false;
                    break;
                }
            }
            if(isSame){
                return chunk;
            }
        }

        chunk.normalize();
        return chunk;
    }

    private static Stack<Tree> extractSubTree(Tree root, Statement statement) {
        Range range = statement.getChars();
        final int begin = range.getBegin();
        final int end = range.getEnd();

        Stack<Tree> stack = new Stack<>();
        stack.push(root);
        Stack<Tree> result = new Stack<>();

        while (!stack.empty()) {
            Tree node = stack.pop();
            if (begin <= node.getPos() && node.getEndPos() <= end) {
                result.push(node);
            }else if(node.getPos() <= begin && end <= node.getEndPos()){
                for (Tree child : node.getChildren()) {
                    stack.push(child);
                }
            }
        }

        return result;
    }

    private void normalize() {
        //徐々に正規化則を適用していく
        Set<Pattern> normalized = new HashSet<Pattern>();
        originalPattern.normalize(normalized);
        normalizedPatterns.addAll(normalized);
    }

    @Override
    public String toString(){
        return """
                Chunk(
                fileName=%s,
                oldBegin=%d,oldEnd=%d,
                newBegin=%d,newEnd=%d,
                olsStatement=
                %s,
                newStatement=
                %s
                """.formatted(fileName,oldStatement.getLines().getBegin(),oldStatement.getLines().getEnd(),newStatement.getLines().getBegin(),newStatement.getLines().getEnd(),oldStatement,newStatement);
    }
}