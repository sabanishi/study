package db;

import lombok.extern.slf4j.Slf4j;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.sqlobject.SqlObjectPlugin;

import java.nio.file.Path;

@Slf4j
public class Database {
    public static Jdbi open(Path path) {
        String uri = "jdbc:sqlite:" + path.toString();
        Jdbi jdbi = Jdbi.create(uri);
        jdbi.installPlugin(new SqlObjectPlugin());

        return jdbi;
    }

    public static void initialize(Handle handle) {
        handle.execute("""
                CREATE TABLE repositories (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    url TEXT UNIQUE
                )""");
        handle.execute("CREATE INDEX repositories_url ON repositories(url)");

        handle.execute("""
                CREATE TABLE commits(
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    repository_id INTEGER,
                    hash TEXT,
                    message TEXT
                    )""");
        handle.execute("CREATE INDEX commits_repository_id ON commits(repository_id)");
        handle.execute("CREATE INDEX commits_hash ON commits(hash)");

        handle.execute("""
                CREATE TABLE chunks (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    commit_id INTEGER,
                    file TEXT,
                    old_begin INTEGER,
                    old_end INTEGER,
                    new_begin INTEGER,
                    new_end INTEGER,
                    old_raw TEXT,
                    new_raw TEXT
                )""");
        handle.execute("CREATE INDEX chunks_commit_id ON chunks(commit_id)");

        handle.execute("""
                CREATE TABLE patterns (
                    hash TEXT PRIMARY KEY,
                    old_tree_hash TEXT,
                    new_tree_hash TEXT,
                    supportH INTEGER,
                    supportC INTEGER,
                    confidenceH REAL,
                    confidenceC REAL,
                    is_candidate BIT,
                    is_normalized BIT,
                    is_useful BIT,
                    is_child_useful BIT,
                    UNIQUE(old_tree_hash, new_tree_hash)
                )""");
        handle.execute("CREATE INDEX patterns_hash ON patterns(hash)");
        handle.execute("CREATE INDEX patterns_old_tree_hash ON patterns(old_tree_hash)");
        handle.execute("CREATE INDEX patterns_new_tree_hash ON patterns(new_tree_hash)");
        handle.execute("CREATE INDEX patterns_supportH ON patterns(supportH)");
        handle.execute("CREATE INDEX patterns_supportC ON patterns(supportC)");
        handle.execute("CREATE INDEX patterns_confidenceH ON patterns(confidenceH)");
        handle.execute("CREATE INDEX patterns_confidenceC ON patterns(confidenceC)");
        handle.execute("CREATE INDEX patterns_is_candidate ON patterns(is_candidate)");
        handle.execute("CREATE INDEX patterns_is_normalized ON patterns(is_normalized)");
        handle.execute("CREATE INDEX patterns_is_useful ON patterns(is_useful)");

        handle.execute("""
                CREATE TABLE normalization_info (
                    hash TEXT PRIMARY KEY,
                    type INTEGER,
                    target_id INTEGER,
                    order_index INTEGER
                )""");
        handle.execute("CREATE INDEX normalization_info_hash ON normalization_info(hash)");

        handle.execute("""
                CREATE TABLE trees (
                    hash TEXT PRIMARY KEY,
                    structure TEXT,
                    text TEXT
                )""");
        handle.execute("CREATE INDEX trees_hash ON trees(hash)");

        handle.execute("""
                CREATE TABLE chunk_patterns (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    chunk_id INTEGER,
                    pattern_hash TEXT
                )""");
        handle.execute("CREATE INDEX chunk_patterns_chunk_id ON chunk_patterns(chunk_id)");
        handle.execute("CREATE INDEX chunk_patterns_pattern_hash ON chunk_patterns(pattern_hash)");


        handle.execute("""
                CREATE TABLE chunk_normalization_info (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    chunk_patterns_id INTEGER,
                    info_hash TEXT
                )""");
        handle.execute("CREATE INDEX chunk_normalization_info_chunk_patterns_id ON chunk_normalization_info(chunk_patterns_id)");

        handle.execute("""
                CREATE TABLE pattern_connections (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    parent_hash TEXT,
                    child_hash TEXT
                )""");
        handle.execute("CREATE INDEX pattern_connections_parent_hash ON pattern_connections(parent_hash)");
        handle.execute("CREATE INDEX pattern_connections_child_hash ON pattern_connections(child_hash)");
        handle.execute("CREATE INDEX pattern_connections_parent_child ON pattern_connections(parent_hash, child_hash)");

        log.info("Table created");
    }
}
