package cmd;

import lombok.extern.slf4j.Slf4j;
import picocli.CommandLine.Command;
import model.db.ChunkDbInfo;
import model.db.PatternDbInfo;
import model.tree.HalNode;
import org.jdbi.v3.core.result.ResultIterable;
import util.LLMUser;

@Slf4j
@Command(name="check",description="Check whether the pattern is useful or not")
public class CheckCommand extends BaseCommand{
    private LLMUser user;

    final String systemMessage = "You are an excellent programmer.\n" +
            "Please determine whether normalizing the given change history to the specified change pattern is useful.\n" +
            "\"Useful\" refers to a change pattern that meets all the following conditions:\n" +
            "For any code fragment that matches the \"before\" section of the change pattern, the \"after\" code fragment can be universally generated.\n" +
            "The transformation can be applied to all matched code fragments without breaking their behavior.\n" +
            "Normalized nodes present in the \"after\" section of the change pattern must also exist in the \"before\" section.\n" +
            "Additionally, if normalized nodes exist in the \"after\" section of the change pattern but not in the \"before\" section, the change pattern cannot be automatically applied and is therefore not useful.\n" +
            "\n" +
            "Output only \"True\" or \"False.\"\n" +
            "The normalization description format is as follows:\n" +
            "\n" +
            "1. \"$V1\" matches any variable or literal.\n" +
            "2. \"[$V1]\" matches any code fragment.\n" +
            "3. \"<$V1>\" matches any number of arguments.\n";

    @Override
    protected void setUp(){
        user = new LLMUser();
    }

    @Override
    protected void process(){
        //DB上からis_candidateがtrueのPatternを取得
        ResultIterable<PatternDbInfo> candidatePatternInfo = dao.fetchCandidatePatterns();
        for(PatternDbInfo info : candidatePatternInfo){
            judgeIsUseful(info);
        }
    }

    private void judgeIsUseful(PatternDbInfo info){
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
        }else{
            dao.updatePatternIsUseful(info.getHash(),false);
        }
    }
}
