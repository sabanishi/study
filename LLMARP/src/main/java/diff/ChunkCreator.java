package diff;

import model.Chunk;
import model.Statement;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.revwalk.RevCommit;
import utils.RepositoryAccess;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ChunkCreator {
    private final ISplitter splitter;
    private final IDifferencer<Statement> differencer;

    public ChunkCreator(final ISplitter splitter,final IDifferencer<Statement> differencer){
        this.splitter = splitter;
        this.differencer = differencer;
    }

    /**
     * コミットに含まれるChunkをリストにして返す
     */
    public List<Chunk> extract(final RevCommit commit,final RepositoryAccess repositoryAccess){
        final List<DiffEntry> entries = repositoryAccess.getChanges(commit);
        return entries.stream().filter(this::isSupportedFileChange)
                .flatMap(e->extractChunks(e,repositoryAccess))
                .collect(Collectors.toList());
    }

    private boolean isSupportedFileChange(final DiffEntry entry){
        return entry.getChangeType() == DiffEntry.ChangeType.MODIFY
                && entry.getOldPath().endsWith(".java")
                && entry.getNewPath().endsWith(".java");
    }

    private Stream<Chunk> extractChunks(final DiffEntry entry, final RepositoryAccess repositoryAccess) {
        String oldSource = repositoryAccess.readBlob(entry.getOldId().toObjectId());
        String newSource = repositoryAccess.readBlob(entry.getNewId().toObjectId());

        List<Statement> oldStatements = splitter.split(oldSource);
        List<Statement> newStatements = splitter.split(newSource);

        return differencer.compute(oldStatements,newStatements)
                .stream()
                .map(e->Chunk.of(entry.getNewPath(),oldStatements,newStatements,e));
    }
}
