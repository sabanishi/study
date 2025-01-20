package cmd;

import db.PatternConnectionDbInfo;
import lombok.extern.slf4j.Slf4j;
import model.Pattern;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import model.db.ChunkDbInfo;
import model.db.PatternDbInfo;
import model.tree.HalNode;
import org.jdbi.v3.core.result.ResultIterable;
import util.LLMUser;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Command(name="check",description="Check whether the pattern is useful or not")
public class CheckCommand extends BaseCommand{
    public static class Config{
        @CommandLine.Option(names = "-n",paramLabel = "<num>",description = "number of pattern list")
        int nPattern = 1000;
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
        ResultIterable<String> patternHashes = dao.fetchHighScorePattern();
        int i = 0;
        List<String> checkedPattern = new ArrayList<>();
        for(String patternHash : patternHashes){
            checkedPattern.add(patternHash);
            PatternDbInfo info = dao.searchPattern(patternHash).first();
            if(info==null){
                log.error("Pattern not found");
                continue;
            }

            //子パターンが有用である時、自身は有用とはしない
            if(!info.getIsChildUseful()){
                log.info("Pattern {} is not useful because children are useful",info.getHash());
                continue;
            }

            //パターンにリテラルが含まれていない時
            if(!info.getIsCandidate()){
                log.info("Pattern {} is not candidate",info.getHash());
                dao.updatePatternIsUseful(info.getHash(),true);
                i++;
                continue;
            }


            if(judgeIsUseful(info)){
                //有用なパターンの場合
                i++;
                log.info("{}/Pattern {} is useful",i,info.getHash());
                //自身の親パターンを取得する
                ResultIterable<PatternConnectionDbInfo> parentPatterns = dao.searchParentPattern(info.getHash());
                for(PatternConnectionDbInfo parentPatternInfo : parentPatterns){
                    String parentHash = parentPatternInfo.getParentHash();
                    //親パターンの子パターンを全て取得する
                    boolean isAllUseful = true;
                    ResultIterable<PatternConnectionDbInfo> childrenPatterns = dao.searchChildPattern(parentHash);
                    for(PatternConnectionDbInfo childPatternInfo : childrenPatterns){
                        //子パターンが有用かどうかを判定する
                        PatternDbInfo childPattern = dao.searchPattern(childPatternInfo.getChildHash()).first();
                        if(!childPattern.getIsUseful()){
                            isAllUseful = false;
                            break;
                        }
                    }
                    if(isAllUseful){
                        //全ての子パターンが有用な場合、親パターンは有用でないとする
                        dao.updatePatternIsUseful(parentHash,false);
                        dao.updatePatternIsChildUseful(parentHash,false);
                        log.info("Parent Pattern {} is not useful",parentHash);
                        if(checkedPattern.contains(parentHash)){
                            i--;
                        }
                    }
                }
            }
            if(i>=config.nPattern){
                break;
            }
        }
    }

    private boolean judgeIsUseful(PatternDbInfo info){
        log.info(info.getIsCandidate()+"");
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
        if(lower.contains("true")) {
            dao.updatePatternIsUseful(info.getHash(), true);
            return true;
        }else{
            dao.updatePatternIsUseful(info.getHash(),false);
            return false;
        }
    }
}
