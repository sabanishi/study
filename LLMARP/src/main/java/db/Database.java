package db;

import lombok.extern.slf4j.Slf4j;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.sqlobject.SqlObjectPlugin;

import java.nio.file.Path;

@Slf4j
public class Database {
    public static Jdbi open(Path path){
        String uri = "jdbc:sqlite:" + path.toString();
        Jdbi jdbi = Jdbi.create(uri);
        jdbi.installPlugin(new SqlObjectPlugin());

        return jdbi;
    }

    public static void initialize(Handle handle){
        handle.execute("CREATE TABLE repositories (id INTEGER PRIMARY KEY AUTOINCREMENT,url TEXT UNIQUE)");
        log.info("Table created");
    }
}
