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

    @SqlQuery("SELECT * FROM repositories")
    @RegisterRowMapper(UrlMapper.class)
    ResultIterable<UrlDbInfo> fetchAllRepositories();

    @SqlQuery("SELECT * FROM commits WHERE repository_id = :repositoryId")
    @RegisterRowMapper(CommitMapper.class)
    ResultIterable<CommitDbInfo> fetchCommitsByRepositoryId(@Bind("repositoryId") long repositoryId);

    @SqlQuery("SELECT * FROM chunks WHERE commit_id = :commitId")
    @RegisterRowMapper(ChunkInfoMapper.class)
    ResultIterable<ChunkDbInfo> fetchChunksByCommitId(@Bind("commitId")long commitId);

    @SqlQuery("SELECT * FROM chunk_patterns WHERE chunk_id = :chunkId")
    @RegisterRowMapper(ChunkPatternMapper.class)
    ResultIterable<ChunkPatternDbInfo> fetchPatternDbInfoByChunkId(@Bind("chunkId") long chunkId);

    @SqlQuery("SELECT * FROM trees WHERE hash = :hash")
    @RegisterRowMapper(TreeMapper.class)
    ResultIterable<TreeDbInfo> fetchTreeByHash(@Bind("hash") String hash);

    @SqlQuery("SELECT * FROM pattern_connections")
    @RegisterRowMapper(PatternConnectionMapper.class)
    ResultIterable<PatternConnectionDbInfo> fetchPatternConnections();

    @SqlQuery("SELECT * FROM chunk_normalization_info WHERE chunk_patterns_id = :chunkPatternsId")
    @RegisterRowMapper(ChunkNormalizedInfoMapper.class)
    ResultIterable<ChunkNormalizedDbInfo> fetchChunkNormalizedInfoByChunkPatternsId(@Bind("chunkPatternsId") long chunkPatternsId);

    @SqlQuery("SELECT * FROM normalization_info")
    @RegisterRowMapper(NormalizationInfoMapper.class)
    ResultIterable<NormalizedDbInfo> fetchNormalizationInfo();

    @SqlQuery("INSERT INTO commits (repository_id, hash, message) VALUES (?, ?, ?) RETURNING id")
    long insertCommit(final long repositoryId, final String hash, final String message);

    @SqlQuery("INSERT INTO chunks (commit_id, file, old_begin, old_end, new_begin, new_end,old_raw,new_raw) VALUES (:commitId, :c.fileName, :c.oldStatement.lines.begin, :c.oldStatement.lines.end, :c.newStatement.lines.begin, :c.newStatement.lines.end,:c.oldStatement.raw,:c.newStatement.raw) RETURNING id")
    long insertChunk(@Bind("commitId") final long commitId, @BindBean("c") final Chunk chunk);

    @SqlQuery("INSERT INTO chunks (commit_id, file, old_begin, old_end, new_begin, new_end,old_raw,new_raw) VALUES (:commitId, :fileName, :oldBegin, :oldEnd, :newBegin, :newEnd,:oldRaw,:newRaw) RETURNING id")
    long insertChunk(@Bind("commitId") final long commitId, @Bind("fileName") final String fileName, @Bind("oldBegin") final int oldBegin, @Bind("oldEnd") final int oldEnd, @Bind("newBegin") final int newBegin, @Bind("newEnd") final int newEnd, @Bind("oldRaw") final String oldRaw, @Bind("newRaw") final String newRaw);

    @SqlUpdate("INSERT OR IGNORE INTO patterns (hash,old_tree_hash, new_tree_hash,is_candidate,is_normalized,is_useful,is_child_useful) VALUES (:p.hash.name,:p.oldTreeRoot.hash.name,:p.newTreeRoot.hash.name,:p.isCandidate,:isNormalized,:isUseful, false)")
    void insertPattern(@BindBean("p") final Pattern pattern,@Bind("isNormalized") boolean isNormalized, @Bind("isUseful") boolean isUseful);

    @SqlUpdate("INSERT OR IGNORE INTO patterns (hash,old_tree_hash, new_tree_hash,is_candidate,is_normalized,is_useful,is_child_useful) VALUES (:p.hash,:p.oldTreeHash,:p.newTreeHash,:p.isCandidate,:p.isNormalized,:p.isUseful,:p.isChildUseful)")
    void insertPattern(@BindBean("p") final PatternDbInfo pattern);

    @SqlUpdate("INSERT OR IGNORE INTO trees (hash, structure,text) VALUES (:t.hash.name,:structure,:t.normalizeText)")
    void insertTree(@BindBean("t") final HalNode tree, @Bind("structure") String structure);

    @SqlUpdate("INSERT OR IGNORE INTO trees (hash, structure,text) VALUES (:info.hash,:info.structure,:info.text)")
    void insertTree(@BindBean("info") final TreeDbInfo info);

    @SqlUpdate("INSERT OR IGNORE INTO normalization_info (hash, type, target_id,order_index) VALUES (:i.hash.name, :i.type,:i.targetId,:i.order)")
    void insertNormalizationInfo(@BindBean("i") NormalizationInfo info);

    @SqlUpdate("INSERT OR IGNORE INTO normalization_info (hash, type, target_id,order_index) VALUES (:info.hash, :info.type,:info.targetId,:info.orderIndex)")
    void insertNormalizationInfo(@BindBean("info") final NormalizedDbInfo info);

    @SqlQuery("INSERT OR IGNORE INTO chunk_patterns (chunk_id, pattern_hash) VALUES (:chunkId, :p.hash.name) RETURNING id")
    long insertChunkPatternRelationship(@Bind("chunkId") final long chunkId, final @BindBean("p") Pattern pattern);

    @SqlQuery("INSERT OR IGNORE INTO chunk_patterns (chunk_id, pattern_hash) VALUES (:chunkId, :patternHash) RETURNING id")
    long insertChunkPatternRelationship(@Bind("chunkId") final long chunkId, @Bind("patternHash") final String patternHash);

    @SqlQuery("INSERT OR IGNORE INTO chunk_normalization_info (chunk_patterns_id, info_hash) VALUES (:chunkPatternsId, :i.hash.name) RETURNING id")
    long insertChunkInfoRelationship(@Bind("chunkPatternsId") final long chunkPatternsId, @BindBean("i") NormalizationInfo info);

    @SqlQuery("INSERT OR IGNORE INTO chunk_normalization_info (chunk_patterns_id, info_hash) VALUES (:info.chunkPatternsId, :info.hash) RETURNING id")
    long insertChunkInfoRelationship(@BindBean("info")final ChunkNormalizedDbInfo info);

    @SqlUpdate("INSERT OR IGNORE INTO pattern_connections (parent_hash, child_hash) VALUES (:parent.hash.name, :child.hash.name)")
    void insertPatternConnection(@BindBean("parent") final Pattern parent, @BindBean("child") final Pattern child);

    @SqlUpdate("INSERT OR IGNORE INTO pattern_connections (parent_hash, child_hash) VALUES (:info.parentHash,:info.childHash)")
    void insertPatternConnection(@BindBean("info") final PatternConnectionDbInfo info);

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

    @SqlUpdate("UPDATE patterns SET is_child_useful = :isUseful WHERE hash = :hash")
    void updatePatternIsChildUseful(@Bind("hash") String hash, @Bind("isUseful") boolean isUseful);

    @SqlQuery("SELECT DISTINCT hash FROM scores ORDER BY score DESC LIMIT :limit")
    ResultIterable<String> fetchHighScorePatternHash(@Bind("limit")int limit);

    @SqlQuery("SELECT * FROM pattern_connections WHERE child_hash = :hash")
    @RegisterRowMapper(PatternConnectionMapper.class)
    ResultIterable<PatternConnectionDbInfo> searchParentPattern(@Bind("hash") String hash);

    @SqlQuery("SELECT * FROM pattern_connections WHERE parent_hash = :hash")
    @RegisterRowMapper(PatternConnectionMapper.class)
    ResultIterable<PatternConnectionDbInfo> searchChildPattern(@Bind("hash") String hash);

    @SqlQuery("SELECT * FROM scores ORDER BY score DESC")
    ResultIterable<String> fetchHighScorePattern();

    @SqlQuery("SELECT * FROM patterns WHERE is_candidate = 1")
    @RegisterRowMapper(PatternMapper.class)
    ResultIterable<PatternDbInfo> fetchCandidatePatterns();

    @SqlQuery("SELECT * FROM patterns WHERE is_useful=1")
    @RegisterRowMapper(PatternMapper.class)
    ResultIterable<PatternDbInfo> fetchUsefulPatterns();

    @SqlUpdate("UPDATE patterns AS p SET supportH = (SELECT count(*) FROM chunk_patterns AS cp WHERE cp.pattern_hash = p.hash) WHERE p.is_useful = 1")
    void computeSupportH();

    @SqlUpdate("UPDATE patterns AS p SET supportC = (SELECT count(DISTINCT cp.chunk_id) FROM chunk_patterns AS cp WHERE cp.pattern_hash = p.hash) WHERE p.is_useful = 1")
    void computeSupportC();

    @SqlUpdate("UPDATE patterns AS p SET confidenceH = CAST(p.supportH AS REAL) / (SELECT sum(p2.supportH) FROM patterns AS p2 WHERE (p2.is_useful = 1 AND p2.old_tree_hash = p.old_tree_hash)) WHERE p.is_useful = 1")
    void computeConfidenceH();

    @SqlUpdate("UPDATE patterns AS p SET confidenceC = CAST(p.supportC AS REAL) / (SELECT sum(p2.supportC) FROM patterns AS p2 WHERE p2.old_tree_hash = p.old_tree_hash) WHERE p.is_useful = 1")
    void computeConfidenceC();

    @SqlUpdate("UPDATE patterns AS p SET is_useful = 0")
    void resetUsefulFlag();

    @SqlUpdate("UPDATE patterns AS p SET is_useful = 1 WHERE hash NOT IN (SELECT parent_hash FROM pattern_connections)")
    void updateAllNormalizedPatternUsefulFlag();

    @SqlUpdate("UPDATE scores SET score = 0 WHERE hash = :hash")
    void resetScore(@Bind("hash") String hash);

    @SqlQuery("SELECT score FROM scores ORDER BY score DESC LIMIT :limit")
    ResultIterable<Float> fetchHighScorePatternNumber(@Bind("limit")int limit);

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
            long id = rs.getLong("id");
            long commitId = rs.getLong("commit_id");
            String fileName = rs.getString("file");
            int oldBegin = rs.getInt("old_begin");
            int oldEnd = rs.getInt("old_end");
            int newBegin = rs.getInt("new_begin");
            int newEnd = rs.getInt("new_end");
            String oldRaw = rs.getString("old_raw");
            String newRaw = rs.getString("new_raw");

            return ChunkDbInfo.of(id,commitId,fileName, oldBegin, oldEnd, newBegin, newEnd, oldRaw, newRaw);
        }
    }

    class PatternMapper implements RowMapper<PatternDbInfo> {
        @Override
        public PatternDbInfo map(ResultSet rs, StatementContext ctx) throws SQLException {
            String hash = rs.getString("hash");
            String oldTreeHash = rs.getString("old_tree_hash");
            String newTreeHash = rs.getString("new_tree_hash");
            boolean isCandidate = rs.getBoolean("is_candidate");
            boolean isNormalized = rs.getBoolean("is_normalized");
            boolean isUseful = rs.getBoolean("is_useful");
            boolean isChildUseful = rs.getBoolean("is_child_useful");
            return PatternDbInfo.of(hash, oldTreeHash, newTreeHash, isCandidate, isNormalized,isUseful,isChildUseful);
        }
    }

    class UrlMapper implements RowMapper<UrlDbInfo>{
        @Override
        public UrlDbInfo map(ResultSet rs, StatementContext ctx) throws SQLException {
            UrlDbInfo info = new UrlDbInfo();
            info.id = rs.getLong("id");
            info.url = rs.getString("url");
            return info;
        }
    }

    class CommitMapper implements RowMapper<CommitDbInfo>{
        @Override
        public CommitDbInfo map(ResultSet rs, StatementContext ctx) throws SQLException {
            CommitDbInfo info = new CommitDbInfo();
            info.id = rs.getLong("id");
            info.repositoryId = rs.getLong("repository_id");
            info.hash = rs.getString("hash");
            info.message = rs.getString("message");
            return info;
        }
    }

    class TreeMapper implements RowMapper<TreeDbInfo> {
        @Override
        public TreeDbInfo map(ResultSet rs, StatementContext ctx) throws SQLException {
            TreeDbInfo info = new TreeDbInfo();
            info.hash = rs.getString("hash");
            info.structure = rs.getString("structure");
            info.text = rs.getString("text");
            return info;
        }
    }

    class ChunkNormalizedInfoMapper implements RowMapper<ChunkNormalizedDbInfo>{
        @Override
        public ChunkNormalizedDbInfo map(ResultSet rs, StatementContext ctx) throws SQLException {
            ChunkNormalizedDbInfo info = new ChunkNormalizedDbInfo();
            info.id = rs.getLong("id");
            info.chunkPatternsId = rs.getLong("chunk_patterns_id");
            info.hash = rs.getString("info_hash");
            return info;
        }
    }

    class ChunkPatternMapper implements RowMapper<ChunkPatternDbInfo>{
        @Override
        public ChunkPatternDbInfo map(ResultSet rs, StatementContext ctx) throws SQLException {
            ChunkPatternDbInfo info = new ChunkPatternDbInfo();
            info.id = rs.getLong("id");
            info.chunkId = rs.getLong("chunk_id");
            info.patternHash = rs.getString("pattern_hash");
            return info;
        }
    }

    class PatternConnectionMapper implements RowMapper<PatternConnectionDbInfo> {
        @Override
        public PatternConnectionDbInfo map(ResultSet rs, StatementContext ctx) throws SQLException {
            PatternConnectionDbInfo info = new PatternConnectionDbInfo();
            info.id = rs.getLong("id");
            info.parentHash = rs.getString("parent_hash");
            info.childHash = rs.getString("child_hash");
            return info;
        }
    }

    class NormalizationInfoMapper implements RowMapper<NormalizedDbInfo>{
        @Override
        public NormalizedDbInfo map(ResultSet rs, StatementContext ctx) throws SQLException {
            NormalizedDbInfo info = new NormalizedDbInfo();
            info.hash = rs.getString("hash");
            info.type = rs.getString("type");
            info.targetId = rs.getLong("target_id");
            info.orderIndex = rs.getLong("order_index");
            return info;
        }
    }
}
