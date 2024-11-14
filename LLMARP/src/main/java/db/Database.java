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
        handle.execute("CREATE TABLE chunks (id INTEGER PRIMARY KEY AUTOINCREMENT,commit_id INTEGER,file TEXT,old_begin INTEGER,old_end INTEGER,new_begin INTEGER,new_end INTEGER)");
        handle.execute("CREATE TABLE patterns (hash TEXT PRIMARY KEY,old TEXT,new TEXT,is_normalized INTEGER)");
        handle.execute("CREATE TABLE normalization_info (hash TEXT PRIMARY KEY, type INTEGER,target_id INTEGER)");
        handle.execute("CREATE TABLE tree (hash TEXT PRIMARY KEY,structure TEXT)");
        handle.execute("CREATE TABLE chunk_normalized_pattern (id INTEGER PRIMARY KEY AUTOINCREMENT,chunk_hash TEXT,pattern_hash TEXT)");
        handle.execute("CREATE TABLE pattern_info (id INTEGER PRIMARY KEY AUTOINCREMENT,pattern_hash TEXT,info_hash TEXT)");

        log.info("Table created");
    }
}
