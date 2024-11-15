package db;

import gson.GsonLocator;
import model.Chunk;
import model.Pattern;
import model.tree.HalNode;
import model.tree.NormalizationInfo;
import org.jdbi.v3.core.result.ResultIterable;
import org.jdbi.v3.core.statement.StatementContext;
import org.jdbi.v3.sqlobject.config.RegisterRowMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindBean;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.jdbi.v3.core.mapper.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

public interface Dao {
    @SqlQuery("INSERT INTO repositories (url) VALUES (?) RETURNING id")
    long insertRepository(final String url);

    @SqlQuery("INSERT INTO commits (repository_id, hash, message) VALUES (?, ?, ?) RETURNING id")
    long insertCommit(final long repositoryId,final String hash,final String message);

    @SqlQuery("INSERT INTO chunks (commit_id, file, old_begin, old_end, new_begin, new_end) VALUES (:commitId, :c.fileName, :c.oldStatement.lines.begin, :c.oldStatement.lines.end, :c.newStatement.lines.begin, :c.newStatement.lines.end) RETURNING id")
    long insertChunk(@Bind("commitId")final long commitId, @BindBean("c") final Chunk chunk);

    @SqlUpdate("INSERT OR IGNORE INTO patterns (hash,old_tree_hash, new_tree_hash,is_normalized) VALUES (:p.hash.name,:p.oldTreeRoot.hash.name,:p.newTreeRoot.hash.name,:isNormalized)")
    void insertPattern(@Bind("isNormalized")boolean isNormalized,@BindBean("p")final Pattern pattern);

    @SqlUpdate("INSERT OR IGNORE INTO trees (hash, structure) VALUES (:t.hash.name,:structure)")
    void insertTree(@BindBean("t")final HalNode tree,@Bind("structure")String structure);

    @SqlUpdate("INSERT OR IGNORE INTO normalization_info (hash, type, target_id) VALUES (:i.hash.name, :i.type,:i.targetId)")
    void insertNormalizationInfo(@BindBean("i")NormalizationInfo info);

    @SqlQuery("INSERT OR IGNORE INTO chunk_patterns (chunk_hash, pattern_hash) VALUES (:chunkId, :p.hash.name) RETURNING id")
    long insertChunkPatternRelationship(@Bind("chunkId")final long chunkId,final @BindBean("p")Pattern pattern);

    @SqlQuery("INSERT OR IGNORE INTO pattern_normalization_info (pattern_hash, info_hash) VALUES (:p.hash.name, :i.hash.name) RETURNING id")
    long insertPatternInfoRelationship(@BindBean("p")Pattern pattern, @BindBean("i")NormalizationInfo info);

    @SqlQuery("SELECT * FROM trees WHERE hash = :hash")
    @RegisterRowMapper(TreeJsonRawMapper.class)
    ResultIterable<HalNode> searchTree(@Bind("hash")final String hash);

    class TreeJsonRawMapper implements RowMapper<HalNode> {
        @Override
        public HalNode map(ResultSet rs, StatementContext ctx) throws SQLException {
            String structure = rs.getString("structure");
            return GsonLocator.getGson().fromJson(structure, HalNode.class);
        }
    }
}
