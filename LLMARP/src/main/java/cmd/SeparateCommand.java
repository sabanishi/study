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
        Path dbPath = app.config.database;
        //ファイルをコピーする
        Files.copy(dbPath,config.nonePath);
        Files.copy(dbPath,config.allPath);

        final Jdbi allJdbi = Database.open(config.allPath);
        try(final Handle h = allJdbi.open()){
            h.execute("""
                    UPDATE patterns AS p
                    SET is_useful = 1
                    WHERE hash NOT IN
                        (SELECT parent_hash
                        FROM pattern_connections
                       )
                    AND confidenceC = 1
                    AND supportC >= 2
                    """);
            h.execute("""
                    UPDATE patterns AS p
                    SET is_useful = 0
                    WHERE p.is_useful = 1
                        AND p.old_tree_hash = p.new_tree_hash
                    """);
        }catch(Exception e){
            log.error(e.getMessage(),e);
        }

        log.info("Separate the pattern");

        final Jdbi noneJdbi = Database.open(config.nonePath);
        try(final Handle h = noneJdbi.open()) {
            h.execute("""
                     CREATE TEMPORARY TABLE candidate_children AS
                     SELECT DISTINCT pc.parent_hash
                     FROM pattern_connections pc
                     INNER JOIN patterns cp
                         ON pc.child_hash = cp.hash
                     WHERE cp.is_candidate = 0
                    """);
            h.execute("""
                    UPDATE patterns AS p
                    SET is_useful = 1
                    WHERE p.is_candidate = 0
                        AND p.hash NOT IN (
                            SELECT parent_hash
                            FROM candidate_children
                        )
                        AND confidenceC = 1
                        AND supportC >= 2
                    """);
            h.execute("""
                    UPDATE patterns AS p
                    SET is_useful = 0
                    WHERE p.is_useful = 1
                        AND p.old_tree_hash = p.new_tree_hash
                    """);
        }catch(Exception e){
            log.error(e.getMessage(),e);
        }
    }
}
