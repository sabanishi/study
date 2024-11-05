package utils;

import lombok.Getter;
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
public class RepositoryAccess implements AutoCloseable{
    @Getter
    private final Repository repository;
    private RevWalk cachedWalk;
    private ObjectReader cachedReader;

    public RepositoryAccess(Path path){
        this.repository = createRepository(path);
    }

    /**
     * PathからJGitのRepositoryを生成する
     */
    private Repository createRepository(final Path path){
        try{
            FileRepositoryBuilder builder = new FileRepositoryBuilder();
            return builder.setGitDir(path.toFile()).readEnvironment().findGitDir().build();
        }catch (Exception e){
            e.printStackTrace();
            return null;
        }
    }

    private DiffFormatter createFormatter(final Repository repo){
        DiffFormatter formatter = new DiffFormatter(DisabledOutputStream.INSTANCE);
        formatter.setRepository(repo);
        formatter.setDiffComparator(RawTextComparator.DEFAULT);
        formatter.setDetectRenames(true);
        return formatter;
    }

    private RevWalk getWalk(){
        if(cachedWalk == null){
            cachedWalk = new RevWalk(repository);
        }
        return cachedWalk;
    }

    private ObjectReader getReader(){
        if(cachedReader == null){
            cachedReader = repository.newObjectReader();
        }
        return cachedReader;
    }

    /**
     * 全コミットを回す
     */
    public Iterable<RevCommit> walk(){
        RevWalk walk = getWalk();
        try{
            Ref head = repository.findRef("HEAD");
            if(head == null){
                throw new RuntimeException("HEAD not found");
            }

            RevCommit commit = walk.parseCommit(head.getObjectId());
            walk.markStart(commit);

        }catch(IOException e){
            e.printStackTrace();
        }

        walk.setRevFilter(RevFilter.NO_MERGES);
        return walk;
    }

    /**
     * 指定したコミットと、その1つ前のコミットとの変更差分を取得する
     */
    public List<DiffEntry> getChanges(final RevCommit commit){
        try(DiffFormatter formatter = createFormatter(repository)){
            ObjectId parentId = commit.getParentCount() == 1 ?  commit.getParent(0).getId() : null;
            return formatter.scan(parentId, commit.getId());
        }catch(IOException e) {
            e.printStackTrace();
            return Collections.emptyList();
        }
    }

    /**
     * 指定したコミットの変更差分を取得する
     */
    public String readBlob(final ObjectId blobId){
        try {
            ObjectLoader loader = getReader().open(blobId,Constants.OBJ_BLOB);
            final RawText text = new RawText(loader.getCachedBytes());

            return text.getString(0, text.size(), false);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "";
    }

    @Override
    public void close(){
        if(cachedWalk != null){
            cachedWalk.close();
        }

        if(cachedReader != null){
            cachedReader.close();
        }

        repository.close();
    }

}
