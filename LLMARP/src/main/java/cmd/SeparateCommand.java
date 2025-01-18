package cmd;

import db.Dao;
import db.Database;
import lombok.extern.slf4j.Slf4j;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Jdbi;
import picocli.CommandLine;

import java.nio.file.Path;

@Slf4j
@CommandLine.Command(name="separate",description="Separate the pattern")
public class SeparateCommand extends BaseCommand{

    @Override
    protected void process(){
        noneProcess();
    }

    private void noneProcess(){
        Path nonePath = Path.of("hal_repair.db");
        final Jdbi noneJdbi = Database.open(nonePath);
        try(final Handle h = noneJdbi.open()){
            h.useTransaction(h0 ->{
                Dao newDao = h0.attach(Dao.class);
                try{
                    newDao.resetUsefulFlag();
                }catch(final Exception e){
                    log.error(e.getMessage(),e);
                }
            });
        }
    }
}
