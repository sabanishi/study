package db;

import model.Chunk;
import model.Pattern;
import model.tree.HalNode;
import model.tree.NormalizationInfo;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindBean;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

public interface Dao {
    @SqlQuery("INSERT INTO repositories (url) VALUES (?) RETURNING id")
    long insertRepository(final String url);

    @SqlQuery("INSERT INTO commits (repository_id, hash, message) VALUES (?, ?, ?) RETURNING id")
    long insertCommit(final long repositoryId,final String hash,final String message);

    @SqlQuery("INSERT INTO chunks (commit_id, file, old_begin, old_end, new_begin, new_end) VALUES (:commitId, :c.fileName, :c.oldStatement.lines.begin, :c.oldStatement.lines.end, :c.newStatement.lines.begin, :c.newStatement.lines.end) RETURNING id")
    long insertChunk(@Bind("commitId")final long commitId, @BindBean("c") final Chunk chunk);

    @SqlUpdate("INSERT OR IGNORE INTO patterns (hash,old, new,is_normalized) VALUES (:p.hash.name,:p.oldTreeRoot.hash.name,:p.newTreeRoot.hash.name,:isNormalized)")
    void insertPattern(@Bind("isNormalized")boolean isNormalized,@BindBean("p")final Pattern pattern);

    @SqlQuery("INSERT OR IGNORE INTO chunk_normalized_pattern (chunk_hash, pattern_hash) VALUES (:chunkId, :p.hash.name) RETURNING id")
    long insertChunkPatternRelationship(@Bind("chunkId")final long chunkId,final @BindBean("p")Pattern pattern);

    @SqlQuery("INSERT OR IGNORE INTO pattern_info (pattern_hash, info_hash) VALUES (:p.hash.name, :i.hash.name) RETURNING id")
    long insertPatternInfoRelationship(@BindBean("p")Pattern pattern, @BindBean("i")NormalizationInfo info);

    @SqlUpdate("INSERT OR IGNORE INTO normalization_info (hash, type, target_id) VALUES (:i.hash.name, :i.type,:i.targetId)")
    void insertNormalizationInfo(@BindBean("i")NormalizationInfo info);

    @SqlUpdate("INSERT OR IGNORE INTO tree (hash, structure) VALUES (:t.hash.name,:structure)")
    void insertTree(@BindBean("t")final HalNode tree,@Bind("structure")String structure);
}
