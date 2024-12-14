package cmd;

import com.github.gumtreediff.tree.Tree;
import lombok.AllArgsConstructor;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import model.db.PatternDbInfo;
import model.tree.*;
import org.jdbi.v3.core.result.ResultIterable;
import picocli.CommandLine;
import picocli.CommandLine.Option;
import picocli.CommandLine.Mixin;
import util.FileUtil;
import util.Pair;
import util.TreeUtil;

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

        @Option(names = {"-a","--at"},paramLabel = "<revision>",description = "revision to retrieve (default: ${DEFAULT-VALUE})")
        Path dbPath = Path.of("halfix.db");
    }

    @Mixin
    Config config = new Config();

    @Override
    protected void process(){
        log.info("Matching: {}",config.repository);
        List<Path> filePaths = searchFiles(config.repository,".java");

        ResultIterable<PatternDbInfo> patterns = dao.fetchUsefulPatterns();
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
                for(PatternInfo info : patternInfoList){
                    HalNode oldRoot = info.getOldTree();
                    HalNode newRoot = info.getNewTree();
                    HalNode copyRoot = targetRoot.deepCopy();
                    apply(copyRoot,oldRoot,newRoot);

                    String result = copyRoot.makeNormalizeText();

                    if(result.equals(targetSource)){
                        //log.debug("no match: {} with {}",filePath,info.getHash());
                    }else{
                        log.info("match: {} with {}",filePath,info.getHash());
                        //結果をファイルに書き込む
                        Path beforePath = Path.of("match/"+matchCount+"_before.java");
                        Path afterPath = Path.of("match/"+matchCount+"_after.java");
                        Path patternOldPath = Path.of("match/"+matchCount+"_pattern_old.txt");
                        Path patternNewPath = Path.of("match/"+matchCount+"_pattern_new.txt");

                        FileUtil.write(beforePath,targetSource);
                        FileUtil.write(afterPath,result);
                        FileUtil.write(patternOldPath,info.getOldTree().toHashString(0));
                        FileUtil.write(patternNewPath,info.getNewTree().toHashString(0));

                        matchCount++;
                    }
                }
            } catch (IOException e) {
                log.error(e.getMessage());
            }
        }
    }

    /**
     * targetRootに対して推薦を行い、木の内容を書き換える
     * @param targetRoot 推薦対象
     * @param patternOldRoot 変更パターンの変更前木
     * @param patternNewRoot 変更パターンの変更後木
     */
    private void apply(HalNode targetRoot, HalNode patternOldRoot,HalNode patternNewRoot){
        List<HalNode> matchedNodes = match(targetRoot, patternOldRoot);

        //マッチングした各部分木に対して、適用を行う
        for(HalNode matchedNode : matchedNodes){
            HalNode newMatchedNode = exchange(matchedNode, patternOldRoot, patternNewRoot);
            ReplaceNode replaceNode = ReplaceNode.of((HalTreeNode)matchedNode,newMatchedNode.makeNormalizeText());
            targetRoot.replace(matchedNode,replaceNode);
        }
    }

    private HalNode exchange(HalNode target, HalNode patternOld, HalNode patternNew){
        HalNode result = patternNew.deepCopy();
        result.setRawText(patternNew.getRawText());

        List<Pair<HalNode,HalNode>> replacedNodes = new ArrayList<>();
        for(HalNode resultChild : result.preOrder()){
            if(!(resultChild.getClass().equals(HalTreeNode.class))){
                HalNode originalNode = target.searchById(resultChild.getId());
                if(originalNode==null){
                    log.error("Node not found: {}",resultChild.getId());
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

    private List<HalNode> match(HalNode target, HalNode pattern){
        List<HalNode> result = new ArrayList<>();

        for(HalNode targetChild : target.preOrder()){
            if(isMatch(targetChild, pattern)){
                result.add(targetChild);
            }
        }

        return result;
    }

    private boolean isMatch(HalNode target, HalNode source){
        if(!equals(target,source)) return false;

        target.setId(source.getId());

        List<HalNode> targetChildren = target.getChildren();
        List<HalNode> sourceChildren = source.getChildren();

        if(source instanceof HalNormalizeInvocationNode || source instanceof HalEmptyNode){
            return true;
        }

        if(targetChildren.size() != sourceChildren.size()) return false;

        for(int index=0; index < targetChildren.size(); index++){
            if(!isMatch(targetChildren.get(index), sourceChildren.get(index))) return false;
        }

        return true;
    }

    private boolean equals(HalNode target,HalNode source){
        if(source instanceof HalEmptyNode) return true;

        if(source instanceof HalNormalizeInvocationNode){
            if(target instanceof HalNormalizeInvocationNode) return true;
            if(target instanceof HalTreeNode targetNode){
                return targetNode.getType().equals("MethodInvocation");
            }
            return false;
        }

        if(source instanceof HalNormalizeNode sourceNode){
            if(target instanceof HalTreeNode targetNode){
                return targetNode.getType().equals(sourceNode.getType());
            }
            return false;
        }

        if(source instanceof HalTreeNode sourceNode){
            if(target instanceof HalTreeNode targetNode){
                return targetNode.getType().equals(sourceNode.getType()) &&
                        targetNode.getLabel().equals(sourceNode.getLabel());
            }
            return false;
        }

        return false;
    }

    private HalNode searchTree(String hash){
        ResultIterable<HalNode> result = dao.searchTree(hash);
        if(result.iterator().hasNext()){
            return result.iterator().next();
        }

        log.error("Tree not found: {}",hash);
        return null;
    }

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

        public static PatternInfo of(String hash, HalNode oldTree, HalNode newTree){
            return new PatternInfo(hash,oldTree,newTree);
        }
    }
}
