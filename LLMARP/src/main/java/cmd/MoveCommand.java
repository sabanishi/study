package cmd;

import db.*;
import lombok.extern.slf4j.Slf4j;
import model.db.ChunkDbInfo;
import model.db.PatternDbInfo;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.result.ResultIterable;
import picocli.CommandLine;

import java.nio.file.Path;

@CommandLine.Command(name = "move",description = "Move database")
@Slf4j
public class MoveCommand extends BaseCommand{
    public static class Config{
        @CommandLine.Option(names = {"-f","--from"}, paramLabel = "<from>", description = "from database file path")
        Path from = Path.of("from.db");

        @CommandLine.Option(names = {"-t","--to"}, paramLabel = "<to>", description = "to database file path")
        Path to = Path.of("to.db");
    }

    @CommandLine.Mixin
    private Config config;

    @Override
    protected void setUp(){
    }

    @Override
    protected void process(){
        //toファイルが存在しない時,初期化する
        boolean toExists = config.to.toFile().exists();
        //2つのDBを開く
        final Jdbi fromDb = Database.open(config.from);
        final Jdbi toDb = Database.open(config.to);
        fromDb.open().useTransaction(fromHandle -> {
            toDb.open().useTransaction(toHandle -> {
                if(!toExists){
                    Database.initialize(toHandle);
                }
                Dao fromDao = fromHandle.attach(Dao.class);
                Dao toDao = toHandle.attach(Dao.class);
                //fromのDBからtoのDBにコピー
                copy(fromDao,toDao);
            });
        });
    }

    private void copy(Dao from,Dao to){
        //Repositoryの情報をコピー
        ResultIterable<UrlDbInfo> urlDbInfos = from.fetchAllRepositories();
        for(UrlDbInfo urlDbInfo : urlDbInfos){
            long repoId = to.insertRepository(urlDbInfo.url);
            int i = 0;
            //Commitの情報をコピー
            ResultIterable<CommitDbInfo> commitDbInfos = from.fetchCommitsByRepositoryId(urlDbInfo.id);
            for(CommitDbInfo commitDbInfo : commitDbInfos){
                i++;
                log.debug("copying commit {}",i);
                long commitId = to.insertCommit(repoId,commitDbInfo.hash,commitDbInfo.message);
                //Chunkの情報をコピー
                ResultIterable<ChunkDbInfo> chunkDbInfos = from.fetchChunksByCommitId(commitDbInfo.id);
                for(ChunkDbInfo chunkDbInfo : chunkDbInfos){
                    long chunkId = to.insertChunk(commitId,chunkDbInfo.getFileName(),chunkDbInfo.getOldBegin(),chunkDbInfo.getOldEnd(),chunkDbInfo.getNewBegin(),chunkDbInfo.getNewEnd(),chunkDbInfo.getOldStatement(),chunkDbInfo.getNewStatement());
                    //ChunkとPatternの関係をコピー
                    ResultIterable<ChunkPatternDbInfo> chunkPatternDbInfos = from.fetchPatternDbInfoByChunkId(chunkDbInfo.getId());
                    for(ChunkPatternDbInfo chunkPatternDbInfo : chunkPatternDbInfos){
                        String patternHash = chunkPatternDbInfo.patternHash;
                        long chunkPatternId = to.insertChunkPatternRelationship(chunkId,patternHash);
                        //Patternの情報をコピー
                        ResultIterable<PatternDbInfo> patternDbInfos = from.searchPattern(patternHash);
                        for(PatternDbInfo patternDbInfo : patternDbInfos){
                            to.insertPattern(patternDbInfo);
                            //Treeの情報をコピー
                            ResultIterable<TreeDbInfo> treeDbInfos = from.fetchTreeByHash(patternDbInfo.getOldTreeHash());
                            for(TreeDbInfo treeDbInfo : treeDbInfos){
                                to.insertTree(treeDbInfo);
                            }
                            ResultIterable<TreeDbInfo> treeDbInfos2 = from.fetchTreeByHash(patternDbInfo.getNewTreeHash());
                            for(TreeDbInfo treeDbInfo : treeDbInfos2){
                                to.insertTree(treeDbInfo);
                            }
                        }

                        //ChunkNormalizedInfoの情報をコピー
                        ResultIterable<ChunkNormalizedDbInfo> chunkNormalizedDbInfos = from.fetchChunkNormalizedInfoByChunkPatternsId(chunkPatternDbInfo.id);
                        for(ChunkNormalizedDbInfo chunkNormalizedDbInfo : chunkNormalizedDbInfos){
                            ChunkNormalizedDbInfo newInfo = new ChunkNormalizedDbInfo();
                            newInfo.chunkPatternsId = chunkPatternId;
                            newInfo.hash = chunkNormalizedDbInfo.hash;
                            to.insertChunkInfoRelationship(newInfo);
                        }
                    }
                }
            }
        }

        //PatternConnectionの情報をコピー
        ResultIterable<PatternConnectionDbInfo> patternConnectionDbInfos = from.fetchPatternConnections();
        for(PatternConnectionDbInfo patternConnectionDbInfo : patternConnectionDbInfos){
            to.insertPatternConnection(patternConnectionDbInfo);
        }

        //NormalizedInfoの情報をコピー
        ResultIterable<NormalizedDbInfo> normalizedDbInfos = from.fetchNormalizationInfo();
        for(NormalizedDbInfo normalizedDbInfo : normalizedDbInfos){
            to.insertNormalizationInfo(normalizedDbInfo);
        }
    }
}
