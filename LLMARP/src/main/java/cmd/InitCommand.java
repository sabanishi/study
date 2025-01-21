package cmd;

import db.Database;
import picocli.CommandLine;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@CommandLine.Command(name="init",description="Initialize database")
public class InitCommand extends BaseCommand{

    @Override
    protected void setUp() throws Exception{
        Files.deleteIfExists(app.config.database);

        //「match」フォルダがある場合、その中身を全て削除
        //無い場合は作成する
        if(!Files.exists(Paths.get("match"))){
            Files.createDirectory(Paths.get("match"));
        }
        for(Path deletePath : Files.list(Paths.get("match")).toList()){
            Files.delete(deletePath);
        }
    }

    @Override
    protected void process() throws Exception {
        Database.initialize(handle);
    }
}
