package cmd;

import db.Dao;
import gson.GsonLocator;
import lombok.extern.slf4j.Slf4j;
import model.Chunk;
import model.Pattern;
import model.tree.HalNode;
import model.tree.NormalizationInfo;
import org.eclipse.jgit.revwalk.RevCommit;
import parse.*;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Mixin;
import model.Statement;
import util.RepositoryAccess;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@Slf4j
@Command(name="extract",description="Extract commit from a  repository")
public class ExtractCommand extends BaseCommand{
    public static class Config{
        @Option(names={"-r","--repository"},paramLabel="<repo>",description="repository path")
        Path repository = Path.of(".git");

        @Option(names={"--from","-f"},paramLabel="<rev>",description="Revision to skip go further (exclusive)")
        String from;

        @Option(names={"--end","-e"},paramLabel="<rev>",description="Revision to start traversing (default: ${DEFAULT-VALUE})")
        String to = "HEAD";
    }

    @Mixin
    Config config = new Config();

    ChunkCreator creator;

    @Override
    protected void setUp(){
        ISplitter splitter = new Splitter();
        IDifferencer<Statement> differencer = new JGitDifferencer<>();
        creator = new ChunkCreator(splitter,differencer);
    }

    @Override
    protected void process(){
        log.info(app.config.database.toString());
        if(!Files.isDirectory(config.repository)){
            log.error("Repository not found: {}",config.repository);
            return;
        }

        try(final RepositoryAccess ra = new RepositoryAccess(config.repository)){
            log.info("Process {}",config.repository);
            final long repoId = dao.insertRepository(config.repository.toString());

            int totalNum = ra.countCommit();
            int now = 0;
            for(final RevCommit c : ra.walk(config.from,config.to)){
                now++;
                try{
                    log.info("Processing ({}/{}): {}",now,totalNum,c.getId().getName());
                    processCommit(c,repoId,ra);
                }catch(Exception e){
                    log.error("Error ({}/{}): {}",now,totalNum,c.getId().getName(),e);
                }
            }
        }
    }

    private void processCommit(final RevCommit commit,final long repoId,final RepositoryAccess ra){
        final List<Chunk> chunks = creator.calculate(commit,ra);

        final long commitId = dao.insertCommit(repoId, commit.getId().getName(), commit.getFullMessage());
        for(final Chunk chunk : chunks){
            InsertChunk(dao,commitId,chunk);
        }
    }

    private void InsertChunk(Dao dao, long commitId, Chunk chunk) {
        long chunkId = dao.insertChunk(commitId, chunk);
        Pattern original = chunk.getOriginalPattern();
        for(Pattern parent : original.getParents()){
            dao.insertPatternConnection(parent, original);
        }
        InsertPattern(dao, chunkId, original, false);
        for (Pattern pattern : chunk.getNormalizedPatterns()) {
            InsertPattern(dao, chunkId, pattern, true);
        }
    }

    private void InsertPattern(Dao dao, long chunkId, Pattern pattern, boolean isNormalized) {
        dao.insertPattern(pattern,isNormalized);
        long chunkPatternsId = dao.insertChunkPatternRelationship(chunkId, pattern);
        InsertTree(dao, pattern.getOldTreeRoot());
        InsertTree(dao, pattern.getNewTreeRoot());
        for (NormalizationInfo info : pattern.getAppliedNormalizations()) {
            InsertNormalizationInfo(dao, chunkPatternsId, info);
        }

        for(Pattern parent : pattern.getParents()){
            dao.insertPatternConnection(parent, pattern);
        }
    }

    private void InsertTree(Dao dao, HalNode tree) {
        String structure = GsonLocator.getGson().toJson(tree);
        dao.insertTree(tree, structure);
    }

    private void InsertNormalizationInfo(Dao dao, long chunkPatternsId, NormalizationInfo info) {
        dao.insertNormalizationInfo(info);
        dao.insertChunkInfoRelationship(chunkPatternsId, info);
    }
}
