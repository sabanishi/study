package cmd;

import db.Dao;
import db.Database;
import lombok.extern.slf4j.Slf4j;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Jdbi;
import picocli.CommandLine;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Slf4j
@CommandLine.Command(name="separate",description="Separate the pattern")
public class SeparateCommand extends BaseCommand{
    public static class Config{
        @CommandLine.Option(names = "--none_name",paramLabel = "<none_normalize_name>",description = "none name")
        Path nonePath = Path.of("none_normalize.db");

        @CommandLine.Option(names="--all_name",paramLabel = "<all_normalize_name>",description = "all name")
        Path allPath = Path.of("all_normalize.db");
    }

    private Config config = new Config();

    @Override
    protected void process() throws IOException {
        //Path dbPath = app.config.database;
        //ファイルをコピーする
        //Files.copy(dbPath,config.nonePath);
        //Files.copy(dbPath,config.allPath);

        /*final Jdbi noneJdbi = Database.open(config.nonePath);
        try(final Handle h = noneJdbi.open()){
            h.execute("UPDATE patterns AS p SET is_useful = 1 WHERE hash NOT IN (SELECT child_hash FROM pattern_connections)");
        }catch(Exception e){
            log.error(e.getMessage(),e);
        }*/

        log.info("Separate the pattern");

        final Jdbi allJdbi = Database.open(config.allPath);
        try(final Handle h = allJdbi.open()) {
            h.execute("""
                    UPDATE patterns AS p SET is_useful = 1
                    WHERE NOT EXISTS (
                        SELECT 1
                        FROM patterns AS cp
                        WHERE cp.hash IN
                        (SELECT child_hash
                            FROM pattern_connections
                            WHERE parent_hash = p.hash
                                AND cp.is_candidate = 1
                        )
                    )
                    """);
        }catch(Exception e){
            log.error(e.getMessage(),e);
        }
    }
}
