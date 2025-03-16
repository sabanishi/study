package cmd;

import db.Dao;
import lombok.extern.slf4j.Slf4j;
import model.db.ChunkDbInfo;
import model.db.PatternDbInfo;
import model.tree.*;
import picocli.CommandLine.Command;
import org.jdbi.v3.core.result.ResultIterable;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

@Slf4j
@Command(name="rq1",description="Execute RQ1")
public class Rq1Command extends BaseCommand{
    @Override
    protected void process() throws Exception {
        int max = 100;
        int i = 0;

        StringBuilder sb = new StringBuilder();

        ResultIterable<PatternDbInfo> highScorePatterns = dao.fetchHighScoreCandidatePatterns();
        for (PatternDbInfo info : highScorePatterns) {
            //子パターンが有用である時、自身は有用とはしない
            if (info.getIsChildUseful()) {
                continue;
            }

            //前後の木が同じ時、有用とはしない
            if (info.getOldTreeHash().equals(info.getNewTreeHash())) {
                continue;
            }

            //変数を正規化できる時、有用とはしない
            HalNode before = dao.searchTree(info.getOldTreeHash()).first();
            if (before != null) {
                if (canNormalizeVariables(before)) {
                    continue;
                }
            }

            watch(info, dao,sb);
            i++;
            if (i >= max) {
                break;
            }
        }

        //ファイルに出力
        File file = new File("rq1.txt");
        //ファイルに書き込む
        try (FileWriter filewriter = new FileWriter(file)) {
            filewriter.write(sb.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void watch(PatternDbInfo patternDbInfo, Dao dao,StringBuilder sb){
        HalNode oldTree = searchTree(dao, patternDbInfo.getOldTreeHash());
        HalNode newTree = searchTree(dao, patternDbInfo.getNewTreeHash());
        assert oldTree != null;
        assert newTree != null;

        sb.append("Hash:").append(patternDbInfo.getHash()).append("\n");
        sb.append("OldTree:").append(oldTree.makeNormalizeText()).append("\n");
        sb.append("NewTree:").append(newTree.makeNormalizeText()).append("\n");
        sb.append("LLM :").append(patternDbInfo.getIsUseful()).append("\n");
        sb.append("Ans1:").append("\n");
        sb.append("Ans2:").append("\n\n");

        //Patternの基になったcommitを取得
        ResultIterable<String> commitHashes = dao.searchChunkHashByPatternHash(patternDbInfo.getHash());
        for (String chunkHash : commitHashes) {
            ResultIterable<ChunkDbInfo> chunkDbInfos = dao.searchChunkById(Long.parseLong(chunkHash));
            for (ChunkDbInfo chunkDbInfo : chunkDbInfos) {
                sb.append("Chunk:").append(chunkDbInfo.getFileName()).append("\n");
                sb.append(chunkDbInfo.getOldBegin()).append("-").append(chunkDbInfo.getOldEnd()).append("\n");
                sb.append(chunkDbInfo.getOldStatement()).append("\n");
                sb.append(chunkDbInfo.getNewBegin()).append("-").append(chunkDbInfo.getNewEnd()).append("\n");
                sb.append(chunkDbInfo.getNewStatement()).append("\n\n");
            }
        }
        sb.append("====================================\n\n");
    }

    private HalNode searchTree(Dao dao,String hash){
        ResultIterable<HalNode> result = dao.searchTree(hash);
        if(result.iterator().hasNext()){
            return result.iterator().next();
        }
        return null;
    }

    private boolean canNormalizeVariables(HalNode oldTree){
        for(HalNode oldNode : oldTree.preOrder()){
            if (oldNode instanceof HalTreeNode oldTargetNode) {
                NormalizeNameInfo info = canNormalizeVariable(oldTargetNode);
                if (info.canNormalize) {
                    return true;
                }
            }
        }

        return false;
    }

    private static NormalizeNameInfo canNormalizeVariable(HalNode targetNode) {
        if (targetNode instanceof HalTreeNode targetTreeNode
                && !(targetNode instanceof HalNormalizeNode)
                && !(targetNode instanceof HalNormalizeInvocationNode)
                && !(targetNode instanceof HalEmptyNode)) {

            //一番外側の変数は正規化しない
            if(targetTreeNode.getId()==0)return new NormalizeNameInfo(false, null);

            if((targetTreeNode.getType().equals("SimpleName"))){
                if (targetNode.getParent() != null && targetNode.getParent() instanceof HalTreeNode parentTreeNode) {
                    String parentType = parentTreeNode.getType();
                    //メソッドの引数または変数宣言の右辺の時のみ、正規化を行う
                    switch(parentType){
                        case "METHOD_INVOCATION_RECEIVER":
                        case "METHOD_INVOCATION_ARGUMENTS":
                        case "SingleVariableDeclaration":
                        case "VariableDeclarationFragment":
                        case "ReturnStatement":
                        case "Assignment":
                            //1文字目が大文字の時,メソッド名と判断して正規化を行わない
                            char firstChar = targetTreeNode.getLabel().charAt(0);
                            if(Character.isUpperCase(firstChar)){
                                return new NormalizeNameInfo(false, null);
                            }
                            return new NormalizeNameInfo(true, targetTreeNode);
                        default:
                            break;
                    }
                }
            }
        }
        return new NormalizeNameInfo(false, null);
    }

    public record NormalizeNameInfo(boolean canNormalize, HalTreeNode targetNode) {
    }
}
