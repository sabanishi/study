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
        handle.execute("CREATE TABLE repositories (id INTEGER PRIMARY KEY AUTOINCREMENT,url TEXT UNIQUE)");
        handle.execute("CREATE TABLE commits (id INTEGER PRIMARY KEY AUTOINCREMENT,repository_id INTEGER,hash TEXT,message TEXT)");
        handle.execute("CREATE TABLE chunks (id INTEGER PRIMARY KEY AUTOINCREMENT,commit_id INTEGER,file TEXT,old_begin INTEGER,old_end INTEGER,new_begin INTEGER,new_end INTEGER,old_raw TEXT,new_raw TEXT)");
        handle.execute("CREATE TABLE patterns (hash TEXT PRIMARY KEY,old_tree_hash TEXT,new_tree_hash TEXT,is_normalized INTEGER)");
        handle.execute("CREATE TABLE normalization_info (hash TEXT PRIMARY KEY, type INTEGER,target_id INTEGER,order_index INTEGER)");
        handle.execute("CREATE TABLE trees (hash TEXT PRIMARY KEY,structure TEXT, text TEXT)");
        handle.execute("CREATE TABLE chunk_patterns (id INTEGER PRIMARY KEY AUTOINCREMENT,chunk_id INTEGER,pattern_hash TEXT)");
        handle.execute("CREATE TABLE chunk_normalization_info (id INTEGER PRIMARY KEY AUTOINCREMENT,chunk_patterns_id INTEGER,info_hash TEXT)");
        handle.execute("CREATE TABLE pattern_connections (id INTEGER PRIMARY KEY AUTOINCREMENT, parent_hash TEXT, child_hash TEXT)");

        log.info("Table created");
    }
}
