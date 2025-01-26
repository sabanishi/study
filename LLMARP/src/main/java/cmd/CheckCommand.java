package cmd;

import db.PatternConnectionDbInfo;
import lombok.extern.slf4j.Slf4j;
import model.Pattern;
import model.tree.*;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import model.db.ChunkDbInfo;
import model.db.PatternDbInfo;
import org.jdbi.v3.core.result.ResultIterable;
import util.LLMUser;
import util.Pair;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Command(name="check",description="Check whether the pattern is useful or not")
public class CheckCommand extends BaseCommand{
    public static class Config{
        @CommandLine.Option(names = "-n",paramLabel = "<num>",description = "number of pattern list")
        int nPattern = 10000;
    }

    private Config config = new Config();

    private LLMUser user;

    final String systemMessage = "You are an excellent programmer.\n" +
            "Please determine if the following literal normalization is useful.\n" +
            "\"Useful\" refers to a change pattern that meets all the following conditions:\n" +
            "For any code fragment that matches the \"before\" section of the change pattern, the \"after\" code fragment can be universally generated.\n" +
            "The transformation can be applied to all matched code fragments without breaking their behavior.\n" +
            "Normalized nodes present in the \"after\" section of the change pattern must also exist in the \"before\" section.\n" +
            "Additionally, if normalized nodes exist in the \"after\" section of the change pattern but not in the \"before\" section, the change pattern cannot be automatically applied and is therefore not useful.\n" +
            "Also, [$V1],[$V2], etc. indicate that the variables are normalized, which does not affect their usefulness.\n" +
            "And $V1, $V2, etc. indicate that the literal is normalized, and you should only determine if this is the correct normalization.\n" +
            "\n" +
            "Output only \"True\" or \"False.\"\n" +
            "Example1\n" +
            "Input:\n" +
            "History\n" +
            "//before\n" +
            "assertEquals($V4,\"Hoge\");\n" +
            "//after\n" +
            "assertThat($V4, is(\"Hoge\"));\n" +
            "\n" +
            "Normalized Pattern\n" +
            "//before\n" +
            "assertEquals($V4,$V5);\n" +
            "//after\n" +
            "assertThat($V4, is($V5));\n" +
            "\n" +
            "Output:\n" +
            "True\n" +
            "\n" +
            "\n" +
            "Example2\n" +
            "Input:\n" +
            "History\n" +
            "//before\n" +
            "assertEquals($V4,\"\");\n" +
            "//after\n" +
            "assertThat($V4).isEmpty();\n" +
            "\n" +
            "Normalized Pattern\n" +
            "//before\n" +
            "assertEquals($V4,$V5);\n" +
            "//after\n" +
            "assertThat($V4).isEmpty();\n" +
            "\n" +
            "Output:\n" +
            "False";

    @Override
    protected void setUp(){
        user = new LLMUser();
    }

    @Override
    protected void process(){
        //DB上からスコアが高い順にパターンを取得
        ResultIterable<PatternDbInfo> patterns = dao.fetchHighScorePattern(2,1);
        log.info("Check {} patterns",patterns.stream().count());
        long max = patterns.stream().count();

        int i = 0;
        for(PatternDbInfo info : patterns){
            i++;
            //子パターンが有用である時、自身は有用とはしない
            if(info.getIsChildUseful()){
                log.info("{}/Pattern {} is not useful because children are useful",i,info.getHash());
                continue;
            }

            //前後の木が同じ時、有用とはしない
            if(info.getOldTreeHash().equals(info.getNewTreeHash())){
                log.info("{}/Pattern {} is not useful because old tree and new tree are same",i,info.getHash());
                continue;
            }

            //変数を正規化できる時、有用とはしない
            HalNode before = dao.searchTree(info.getOldTreeHash()).first();
            if(before!=null){
                if(canNormalizeVariables(before)){
                    log.info("{}/Pattern {} is not useful because variables can be normalized",i,info.getHash());
                    continue;
                }
            }

            if(judgeIsUseful(info)){
                log.info("{}/Pattern {} is useful",i,info.getHash());
                //自身の親パターンを取得する
                ResultIterable<PatternConnectionDbInfo> parentPatterns = dao.searchParentPattern(info.getHash());
                for(PatternConnectionDbInfo parentPatternInfo : parentPatterns){
                    String parentHash = parentPatternInfo.getParentHash();
                    //親パターンは有用でないとする
                    dao.updatePatternIsUseful(parentHash,false);
                    dao.updatePatternIsChildUseful(parentHash,true);
                }
            }
        }
    }

    private boolean judgeIsUseful(PatternDbInfo info){
        //パターンにリテラルが含まれていない時
        if(!info.getIsCandidate()){
            log.info("Pattern {} is not candidate",info.getHash());
            dao.updatePatternIsUseful(info.getHash(),true);
            return true;
        }

        log.info("Check Pattern {}",info.getHash());

        StringBuilder sb = new StringBuilder();
        //Patternを取得
        HalNode oldTree = dao.searchTree(info.getOldTreeHash()).first();
        HalNode newTree = dao.searchTree(info.getNewTreeHash()).first();

        ResultIterable<String> chunkIds = dao.searchChunkHashByPatternHash(info.getHash());

        //そのチャンクの情報を取得
        for (String chunkId : chunkIds) {
            ResultIterable<ChunkDbInfo> chunks = dao.searchChunkById(Long.parseLong(chunkId));
            int j = 0;
            for (ChunkDbInfo chunkInfo : chunks) {
                sb.append("Code Change History").append(j).append("\n");
                j++;
                sb.append("//before\n");
                String before = chunkInfo.getOldStatement().replace("\t", "");
                sb.append(before).append("\n");
                String after = chunkInfo.getNewStatement().replace("\t", "");
                sb.append("//after\n");
                sb.append(after).append("\n");
                sb.append("\n");
            }
        }

        sb.append("Normalized Pattern\n");
        sb.append("//before\n");
        sb.append(oldTree.makeNormalizeText()).append("\n");
        sb.append("//after\n");
        sb.append(newTree.makeNormalizeText()).append("\n");

        log.debug(oldTree.makeNormalizeText() +" -> "+ newTree.makeNormalizeText());
        String result = user.send(systemMessage, sb.toString(),1.0f);

        if(result.isEmpty()){
            log.debug("No answer");
        }else{
            log.debug(result);
        }

        //結果を小文字にして、「ture」を含むかを確認
        String lower = result.toLowerCase();
        if(lower.contains("false")) {
            dao.updatePatternIsUseful(info.getHash(), false);
            return false;
        }else{
            dao.updatePatternIsUseful(info.getHash(),true);
            return true;
        }
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
