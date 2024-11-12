package parse;

import com.github.gumtreediff.tree.Tree;
import model.Chunk;
import model.Statement;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.Edit;
import org.eclipse.jgit.revwalk.RevCommit;
import utils.RepositoryAccess;
import utils.TreeUtils;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ChunkCreator {
    private final ISplitter splitter;
    private final IDifferencer<Statement> differencer;

    public ChunkCreator(final ISplitter splitter, final IDifferencer<Statement> differencer) {
        this.splitter = splitter;
        this.differencer = differencer;
    }

    /**
     * コミットに含まれるChunkをリストにして返す
     */
    public List<Chunk> calculate(final RevCommit commit, final RepositoryAccess repositoryAccess) {
        final List<DiffEntry> entries = repositoryAccess.getChanges(commit);
        return entries.stream().filter(this::isSupportedFileChange)
                .flatMap(e -> extractChunks(e, repositoryAccess))
                .collect(Collectors.toList());
    }

    private boolean isSupportedFileChange(final DiffEntry entry) {
        return entry.getChangeType() == DiffEntry.ChangeType.MODIFY
                && entry.getOldPath().endsWith(".java")
                && entry.getNewPath().endsWith(".java");
    }

    private Stream<Chunk> extractChunks(final DiffEntry entry, final RepositoryAccess repositoryAccess) {
        String oldSource = repositoryAccess.readBlob(entry.getOldId().toObjectId());
        String newSource = repositoryAccess.readBlob(entry.getNewId().toObjectId());

        List<Statement> oldStatements = splitter.split(oldSource);
        List<Statement> newStatements = splitter.split(newSource);

        Tree oldAllTree = TreeUtils.createTree(oldSource);
        Tree newAllTree = TreeUtils.createTree(newSource);

        return differencer.compute(oldStatements, newStatements)
                .stream()
                .filter(e -> e.getType() == Edit.Type.REPLACE)//単純な追加、削除は除外する
                .map(e -> Chunk.of(entry.getNewPath(), oldStatements, oldAllTree, newStatements, newAllTree, e));
    }
}
