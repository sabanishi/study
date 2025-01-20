package cmd;

import lombok.extern.slf4j.Slf4j;
import picocli.CommandLine.Command;

@Slf4j
@Command(name="measure",description="Measure Change Opportunities")
public class MeasureCommand extends BaseCommand{

    @Override
    protected void process(){
        log.info("Measure {}",app.config.database);

        //support,confidenceを計算
        log.info("computing supportC");
        dao.computeSupportC();

        log.info("computing confidenceC");
        dao.computeConfidenceC();
        log.info("computing score");
        //スコアの列を追加する
        handle.execute("ALTER TABLE patterns ADD COLUMN score REAL;");
        handle.execute("UPDATE patterns SET score = confidenceC+supportC*0.001;");

        //scoreを計算し、テーブルを作成する
        handle.execute("DROP TABLE IF EXISTS scores;");
        handle.execute("""
                CREATE TABLE scores AS
                SELECT
                hash,
                confidenceC+supportC*0.001 AS score
                FROM patterns
                ORDER BY score DESC
                """);
        handle.execute("DROP INDEX IF EXISTS scores_hash;");
        handle.execute("CREATE INDEX scores_hash ON scores(hash);");
        log.info("made SCORE table");
    }
}
