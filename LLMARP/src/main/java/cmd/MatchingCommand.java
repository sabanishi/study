package cmd;

import com.github.difflib.DiffUtils;
import com.github.difflib.patch.AbstractDelta;
import com.github.difflib.patch.Patch;
import com.github.gumtreediff.tree.Tree;
import db.Dao;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import model.Statement;
import model.db.PatternDbInfo;
import model.tree.HalNode;
import model.tree.HalRootNode;
import model.tree.HalTreeNode;
import model.tree.ReplaceNode;
import org.jdbi.v3.core.result.ResultIterable;
import parse.ISplitter;
import parse.Splitter;
import picocli.CommandLine;
import picocli.CommandLine.Option;
import picocli.CommandLine.Mixin;
import util.FileUtil;
import util.Pair;
import util.TreeUtil;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

@Slf4j
@CommandLine.Command(name = "matching", description = "Matching Change Opportunities")
public class MatchingCommand extends BaseCommand{
    public static class Config{
        @Option(names = {"-r","--repository"},paramLabel = "<repo>",description = "repository path")
        Path repository = Path.of("");

        @Option(names = "-n",paramLabel = "<num>",description = "number of pattern list")
        int nPattern = 10000;
    }

    @Mixin
    Config config = new Config();

    @Override
    protected void process(){
        log.info("Matching: {}",config.repository);
        List<Path> filePaths = searchFiles(config.repository,".java");

        ResultIterable<String> patternHashList = dao.fetchHighScorePatternHash(config.nPattern);
        List<PatternDbInfo> patterns = patternHashList.stream().flatMap(h-> dao.searchPattern(h).stream()).toList();
        List<PatternInfo> patternInfoList = new ArrayList<>();

        //PatternDbInfoからPatternInfoを作成
        //DBInfoに含まれるハッシュ値からHalNodeの参照を作成する
        for(PatternDbInfo dbInfo : patterns){
            HalNode oldTree = searchTree(dbInfo.getOldTreeHash());
            HalNode newTree = searchTree(dbInfo.getNewTreeHash());
            PatternInfo info = PatternInfo.of(dbInfo.getHash(),oldTree,newTree);
            patternInfoList.add(info);
        }

        int matchCount = 0;
        for(Path filePath : filePaths){
            log.debug("Matching: {}",filePath);
            try{
                String targetSource = FileUtil.read(filePath);
                Tree targetTree = TreeUtil.createTree(targetSource);
                HalTreeNode targetRoot = HalTreeNode.of(targetTree,targetSource);

                File logFile = new File("match/match_log.csv");

                for(PatternInfo info : patternInfoList){
                    HalRootNode oldRoot = (HalRootNode)info.getOldTree();
                    HalRootNode newRoot = (HalRootNode)info.getNewTree();
                    HalNode copyRoot = targetRoot.deepCopy();
                    apply(copyRoot,oldRoot,newRoot);

                    String result = copyRoot.makeNormalizeText();

                    if(!result.equals(targetSource)){
                        log.info("match: {} with {}",filePath,info.getHash());
                        //結果をファイルに書き込む
                        Path beforePath = Path.of("match/"+matchCount+"_before.java");
                        Path afterPath = Path.of("match/"+matchCount+"_after.java");
                        Path patternOldPath = Path.of("match/"+matchCount+"_pattern_old.txt");
                        Path patternNewPath = Path.of("match/"+matchCount+"_pattern_new.txt");
                        Path diffPath = Path.of("match/"+matchCount+"_diff.txt");

                        FileUtil.write(beforePath,targetSource);
                        FileUtil.write(afterPath,result);
                        FileUtil.write(patternOldPath,info.getOldTree().toHashString(0));
                        FileUtil.write(patternNewPath,info.getNewTree().toHashString(0));

                        String diff = makeDiff(targetSource,result);
                        diff = info.getHash()+"\n"+diff;
                        FileUtil.write(diffPath,diff);

                        FileWriter logFw = new FileWriter(logFile,true);
                        logFw.write(matchCount+","+filePath+","+info.getHash()+"\n");
                        logFw.close();

                        matchCount++;
                    }
                }
            } catch (IOException e) {
                log.error(e.getMessage());
            }
        }
    }

    /**
     * targetとresultの差分を生成する
     */
    private String makeDiff(String target,String result){
        ISplitter splitter = new Splitter();
        List<Statement> targetStatements = splitter.split(target);
        List<Statement> resultStatements = splitter.split(result);
        Patch<Statement> diff = DiffUtils.diff(targetStatements,resultStatements);

        StringBuilder sb = new StringBuilder();
        for(AbstractDelta<Statement> delta : diff.getDeltas()){
            sb.append(delta.getSource().toString());
            sb.append("\n");
            sb.append(delta.getTarget().toString());
            sb.append("\n\n");
        }

        return sb.toString();
    }

    /**
     * targetRootに対して推薦を行い、木の内容を書き換える
     * @param targetRoot 推薦対象
     * @param patternOldRoot 変更パターンの変更前木
     * @param patternNewRoot 変更パターンの変更後木
     */
    private void apply(HalNode targetRoot, HalRootNode patternOldRoot,HalRootNode patternNewRoot){
        List<List<HalTreeNode>> matchedNodes = match(targetRoot, patternOldRoot);

        //マッチングした各部分木に対して、適用を行う
        for(List<HalTreeNode> matchedNode : matchedNodes){
            HalNode newMatchedNode = exchange(matchedNode, patternNewRoot);
            ReplaceNode replaceNode = ReplaceNode.of(matchedNode,newMatchedNode.makeNormalizeText());
            targetRoot.replace(matchedNode.get(0),replaceNode);
            for(int i=1;i<matchedNode.size();i++) {
                targetRoot.removeChild(matchedNode.get(i));
            }
        }
    }

    /**
     * targetに対してpatternNewを適用した木を生成する
     */
    private HalNode exchange(List<HalTreeNode> target, HalRootNode patternNew){
        HalNode result = patternNew.deepCopy();
        result.setRawText(patternNew.getRawText());

        List<Pair<HalNode,HalNode>> replacedNodes = new ArrayList<>();
        for(HalNode resultChild : result.preOrder()){
            if(!(resultChild.getClass().equals(HalTreeNode.class))){
                HalNode originalNode = null;
                for(HalNode targetChild:target){
                    originalNode = targetChild.searchById(resultChild.getId());
                    if(originalNode!=null){
                        break;
                    }
                }
                if(originalNode==null){
                    log.error("originalNode is null");
                    continue;
                }
                ReplaceNode replaceNode = ReplaceNode.of((HalTreeNode)resultChild,originalNode.makeNormalizeText());
                replacedNodes.add(Pair.of(resultChild,replaceNode));
            }
        }

        for(Pair<HalNode,HalNode> pair : replacedNodes){
            HalNode originalNode = pair.getFirst();
            HalNode resultNode = pair.getSecond();
            result.replace(originalNode,resultNode);
        }

        return result;
    }

    /**
     * targetの子Nodeを舐めて、patternとマッチするNodeを返す
     */
    private List<List<HalTreeNode>> match(HalNode target,HalRootNode pattern){
        List<List<HalTreeNode>> result = new ArrayList<>();
        List<List<HalTreeNode>> myResult = matchLoop(target,pattern);
        if(myResult!=null){
            result.addAll(myResult);
        }

        for(HalNode child : target.getChildren()){
            List<List<HalTreeNode>> childResult = match(child,pattern);
            result.addAll(childResult);
        }
        return result;
    }

    /**
     * targetの子Node中にpatternとマッチするものがあるかを探す
     */
    private List<List<HalTreeNode>> matchLoop(HalNode target,HalRootNode pattern){
        if(target.getChildren().size() < pattern.getChildren().size())return null;

        List<List<HalTreeNode>> result = new ArrayList<>();
        for(int i=0;i<target.getChildren().size();i++){
            HalNode targetChild = target.getChildren().get(i);
            if(targetChild.match(pattern.getChildren().get(0))){
                List<HalTreeNode> matchedNodes = new ArrayList<>();
                matchedNodes.add((HalTreeNode)targetChild);
                if(i == target.getChildren().size()-1 && pattern.getChildren().size()==1){
                    result.add(matchedNodes);
                    break;
                }
                for(int j=i+1;j<target.getChildren().size();j++){
                    int patternCount = j-i;
                    if(patternCount >= pattern.getChildren().size()){
                        result.add(matchedNodes);
                        break;
                    }
                    if(j >= pattern.getChildren().size())break;
                    if(target.getChildren().get(j).match(pattern.getChildren().get(j-i))){
                        matchedNodes.add((HalTreeNode)target.getChildren().get(i));
                    }else{
                        break;
                    }
                }
            }
        }

        return result;
    }

    /**
     * 引数のハッシュ値を持つ木をDBから検索する
     */
    private HalNode searchTree(String hash){
        ResultIterable<HalNode> result = dao.searchTree(hash);
        if(result.iterator().hasNext()){
            return result.iterator().next();
        }

        return null;
    }

    /**
     * 指定されたディレクトリ内から指定された拡張子のファイルを検索する
     */
    private List<Path> searchFiles(Path rootPath, String extension){
        try(Stream<Path> stream = Files.walk(rootPath)){
            return stream.filter(p->p.toString().endsWith(extension)).toList();
        }
        catch(IOException e){
            log.error(e.getMessage());
            return List.of();
        }
    }

    @Value
    public static class PatternInfo{
        public String hash;
        public HalNode oldTree;
        public HalNode newTree;

        public PatternInfo(String hash, HalNode oldTree, HalNode newTree){
            this.hash = hash;
            this.oldTree = oldTree;
            this.newTree = newTree;
        }

        public static MatchingCommand.PatternInfo of(String hash, HalNode oldTree, HalNode newTree){
            return new MatchingCommand.PatternInfo(hash,oldTree,newTree);
        }
    }
}
