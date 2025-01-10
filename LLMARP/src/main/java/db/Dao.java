package db;

import gson.GsonLocator;
import model.Chunk;
import model.Pattern;
import model.db.ChunkDbInfo;
import model.db.PatternDbInfo;
import model.tree.HalNode;
import model.tree.NormalizationInfo;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.result.ResultIterable;
import org.jdbi.v3.core.statement.StatementContext;
import org.jdbi.v3.sqlobject.config.RegisterRowMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindBean;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

import java.sql.ResultSet;
import java.sql.SQLException;

public interface Dao {
    @SqlQuery("INSERT INTO repositories (url) VALUES (?) RETURNING id")
    long insertRepository(final String url);

    @SqlQuery("INSERT INTO commits (repository_id, hash, message) VALUES (?, ?, ?) RETURNING id")
    long insertCommit(final long repositoryId, final String hash, final String message);

    @SqlQuery("INSERT INTO chunks (commit_id, file, old_begin, old_end, new_begin, new_end,old_raw,new_raw) VALUES (:commitId, :c.fileName, :c.oldStatement.lines.begin, :c.oldStatement.lines.end, :c.newStatement.lines.begin, :c.newStatement.lines.end,:c.oldStatement.raw,:c.newStatement.raw) RETURNING id")
    long insertChunk(@Bind("commitId") final long commitId, @BindBean("c") final Chunk chunk);

    @SqlUpdate("INSERT OR IGNORE INTO patterns (hash,old_tree_hash, new_tree_hash,is_candidate,is_normalized,is_useful) VALUES (:p.hash.name,:p.oldTreeRoot.hash.name,:p.newTreeRoot.hash.name,:p.isCandidate,:isNormalized,:isUseful)")
    void insertPattern(@BindBean("p") final Pattern pattern,@Bind("isNormalized") boolean isNormalized, @Bind("isUseful") boolean isUseful);

    @SqlUpdate("INSERT OR IGNORE INTO trees (hash, structure,text) VALUES (:t.hash.name,:structure,:t.normalizeText)")
    void insertTree(@BindBean("t") final HalNode tree, @Bind("structure") String structure);

    @SqlUpdate("INSERT OR IGNORE INTO normalization_info (hash, type, target_id,order_index) VALUES (:i.hash.name, :i.type,:i.targetId,:i.order)")
    void insertNormalizationInfo(@BindBean("i") NormalizationInfo info);

    @SqlQuery("INSERT OR IGNORE INTO chunk_patterns (chunk_id, pattern_hash) VALUES (:chunkId, :p.hash.name) RETURNING id")
    long insertChunkPatternRelationship(@Bind("chunkId") final long chunkId, final @BindBean("p") Pattern pattern);

    @SqlQuery("INSERT OR IGNORE INTO chunk_normalization_info (chunk_patterns_id, info_hash) VALUES (:chunkPatternsId, :i.hash.name) RETURNING id")
    long insertChunkInfoRelationship(@Bind("chunkPatternsId") final long chunkPatternsId, @BindBean("i") NormalizationInfo info);

    @SqlUpdate("INSERT OR IGNORE INTO pattern_connections (parent_hash, child_hash) VALUES (:parent.hash.name, :child.hash.name)")
    void insertPatternConnection(@BindBean("parent") final Pattern parent, @BindBean("child") final Pattern child);

    @SqlQuery("SELECT chunk_id FROM chunk_patterns WHERE pattern_hash = :hash")
    ResultIterable<String> searchChunkHashByPatternHash(@Bind("hash") String hash);

    @SqlQuery("SELECT * FROM trees WHERE hash = :hash")
    @RegisterRowMapper(TreeJsonRawMapper.class)
    ResultIterable<HalNode> searchTree(@Bind("hash") final String hash);

    @SqlQuery("SELECT * FROM chunks WHERE id = :chunkId")
    @RegisterRowMapper(ChunkInfoMapper.class)
    ResultIterable<ChunkDbInfo> searchChunkById(@Bind("chunkId") long chunkId);

    @SqlQuery("SELECT * FROM patterns WHERE hash = :hash")
    @RegisterRowMapper(PatternMapper.class)
    ResultIterable<PatternDbInfo> searchPattern(@Bind("hash") String hash);

    @SqlUpdate("UPDATE patterns SET is_useful = :isUseful WHERE hash = :hash")
    void updatePatternIsUseful(@Bind("hash") String hash, @Bind("isUseful") boolean isUseful);

    @SqlQuery("SELECT DISTINCT hash FROM scores ORDER BY score DESC LIMIT :limit")
    ResultIterable<String> fetchHighScorePatternHash(@Bind("limit")int limit);

    @SqlQuery("SELECT * FROM patterns WHERE is_candidate = 1")
    @RegisterRowMapper(PatternMapper.class)
    ResultIterable<PatternDbInfo> fetchCandidatePatterns();

    @SqlUpdate("UPDATE patterns AS p SET supportH = (SELECT count(*) FROM chunk_patterns AS cp WHERE cp.pattern_hash = p.hash)")
    void computeSupportH();

    @SqlUpdate("UPDATE patterns AS p SET supportC = (SELECT count(DISTINCT cp.chunk_id) FROM chunk_patterns AS cp WHERE cp.pattern_hash = p.hash)")
    void computeSupportC();

    @SqlUpdate("UPDATE patterns AS p SET confidenceH = CAST(p.supportH AS REAL) / (SELECT sum(p2.supportH) FROM patterns AS p2 WHERE p2.old_tree_hash = p.old_tree_hash)")
    void computeConfidenceH();

    @SqlUpdate("UPDATE patterns AS p SET confidenceC = CAST(p.supportC AS REAL) / (SELECT sum(p2.supportC) FROM patterns AS p2 WHERE p2.old_tree_hash = p.old_tree_hash)")
    void computeConfidenceC();

    class TreeJsonRawMapper implements RowMapper<HalNode> {
        @Override
        public HalNode map(ResultSet rs, StatementContext ctx) throws SQLException {
            String structure = rs.getString("structure");
            return GsonLocator.getGson().fromJson(structure, HalNode.class);
        }
    }

    class ChunkInfoMapper implements RowMapper<ChunkDbInfo> {
        @Override
        public ChunkDbInfo map(ResultSet rs, StatementContext ctx) throws SQLException {
            String fileName = rs.getString("file");
            int oldBegin = rs.getInt("old_begin");
            int oldEnd = rs.getInt("old_end");
            int newBegin = rs.getInt("new_begin");
            int newEnd = rs.getInt("new_end");
            String oldRaw = rs.getString("old_raw");
            String newRaw = rs.getString("new_raw");

            return ChunkDbInfo.of(fileName, oldBegin, oldEnd, newBegin, newEnd, oldRaw, newRaw);
        }
    }

    class PatternMapper implements RowMapper<PatternDbInfo> {
        @Override
        public PatternDbInfo map(ResultSet rs, StatementContext ctx) throws SQLException {
            String hash = rs.getString("hash");
            String oldTreeHash = rs.getString("old_tree_hash");
            String newTreeHash = rs.getString("new_tree_hash");
            boolean isNormalized = rs.getBoolean("is_normalized");
            boolean isCandidate = rs.getBoolean("is_candidate");
            boolean isUseful = rs.getBoolean("is_useful");
            return PatternDbInfo.of(hash, oldTreeHash, newTreeHash, isNormalized, isCandidate, isUseful);
        }
    }
}
