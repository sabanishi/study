package util;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.diff.RawText;
import org.eclipse.jgit.diff.RawTextComparator;
import org.eclipse.jgit.lib.*;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.revwalk.filter.RevFilter;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.util.io.DisabledOutputStream;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

/**
 * Gitリポジトリを操作するためのクラス
 */
@Slf4j
public class RepositoryAccess implements AutoCloseable {
    @Getter
    private final Repository repository;
    private RevWalk cachedWalk;
    private ObjectReader cachedReader;

    public RepositoryAccess(Path path) {
        this.repository = createRepository(path);
    }

    public RepositoryAccess(Repository repository) {
        this.repository = repository;
    }

    /**
     * PathからJGitのRepositoryを生成する
     */
    private Repository createRepository(final Path path) {
        try {
            FileRepositoryBuilder builder = new FileRepositoryBuilder();
            return builder.setGitDir(path.toFile()).readEnvironment().findGitDir().build();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private DiffFormatter createFormatter(final Repository repo) {
        DiffFormatter formatter = new DiffFormatter(DisabledOutputStream.INSTANCE);
        formatter.setRepository(repo);
        formatter.setDiffComparator(RawTextComparator.DEFAULT);
        formatter.setDetectRenames(true);
        return formatter;
    }

    private RevWalk getWalk() {
        if (cachedWalk == null) {
            cachedWalk = new RevWalk(repository);
        }
        return cachedWalk;
    }

    private ObjectReader getReader() {
        if (cachedReader == null) {
            cachedReader = repository.newObjectReader();
        }
        return cachedReader;
    }

    /**
     * 指定したコミット範囲を回す
     */
    public Iterable<RevCommit> walk(final String commitFrom,final String commitTo){
        final RevWalk walk = getWalk();
        // from: exclusive (from, to]
        if (commitFrom != null) {
            try {
                final RevCommit c = walk.parseCommit(repository.resolve(commitFrom));
                log.info("Range from [exclusive]: {} ({})", commitFrom, c.getId().name());
                walk.markUninteresting(c);
            } catch (final IOException e) {
                log.error("Invalid rev: {} ({})", commitFrom, e);
            }
        }

        // end: inclusive (from, to]
        if(commitTo != null){
            try {
                final RevCommit c = walk.parseCommit(repository.resolve(commitTo));
                log.info("Range to (inclusive): {} ({})", commitTo, c.getId().name());
                walk.markStart(c);
            } catch (final IOException e) {
                log.error("Invalid rev: {} ({})", commitTo, e);
            }
        }

        walk.setRevFilter(RevFilter.NO_MERGES);
        return walk;
    }

    /**
     * 全コミットを回す
     */
    public Iterable<RevCommit> walk() {
        return walk(null,"HEAD");
    }

    /**
     * 指定したコミットと、その1つ前のコミットとの変更差分を取得する
     */
    public List<DiffEntry> getChanges(final RevCommit commit) {
        try (DiffFormatter formatter = createFormatter(repository)) {
            ObjectId parentId = commit.getParentCount() == 1 ? commit.getParent(0).getId() : null;
            return formatter.scan(parentId, commit.getId());
        } catch (Exception e) {
            e.printStackTrace();
            return Collections.emptyList();
        }
    }

    /**
     * 指定したコミットの変更差分を取得する
     */
    public String readBlob(final ObjectId blobId) {
        try {
            ObjectLoader loader = getReader().open(blobId, Constants.OBJ_BLOB);
            final RawText text = new RawText(loader.getCachedBytes());

            return text.getString(0, text.size(), false);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    public int countCommit(){
        try{
            Git git = Git.open(repository.getDirectory());
            Iterable<RevCommit> commits = git.log().call();
            int count = 0;
            for(RevCommit c : commits){
                count++;
            }
            return count;
        }catch(Exception e){
            log.error("Failed to open git repository",e);
            return 0;
        }
    }

    @Override
    public void close() {
        if (cachedWalk != null) {
            cachedWalk.close();
        }

        if (cachedReader != null) {
            cachedReader.close();
        }

        repository.close();
    }

}
