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
        dao.computeSupportC();
        dao.computeSupportH();
        dao.computeConfidenceC();
        dao.computeConfidenceH();

        //scoreを計算し、テーブルを作成する
        handle.execute("DROP TABLE IF EXISTS scores;");
        handle.execute("""
                CREATE TABLE scores AS
                SELECT
                hash,
                confidenceH+supportH*0.001 AS score
                FROM patterns
                ORDER BY score DESC
                """);
        handle.execute("DROP INDEX IF EXISTS scores_hash;");
        handle.execute("CREATE INDEX scores_hash ON scores(hash);");
        log.info("made SCORE table");
    }
}
