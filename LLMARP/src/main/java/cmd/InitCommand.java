package cmd;

import db.Database;
import picocli.CommandLine;

import java.nio.file.Files;

@CommandLine.Command(name="init",description="Initialize database")
public class InitCommand extends BaseCommand{
    @Override
    protected void setUp() throws Exception{
        Files.deleteIfExists(app.config.database);
    }

    @Override
    protected void process() throws Exception {
        Database.initialize(handle);
    }
}
