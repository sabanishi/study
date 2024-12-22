package cmd;

import com.google.common.base.Stopwatch;
import lombok.extern.slf4j.Slf4j;
import picocli.CommandLine.ParentCommand;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

@Slf4j
public abstract class BaseCommand implements Callable<Integer> {
    @ParentCommand
    protected AppCommand app;

    @Override
    public Integer call() {
        final Stopwatch w = Stopwatch.createStarted();
        try{
            setUp();
            process();
        }catch (Exception e){
            log.error(e.getMessage(),e);
        }finally {
            log.debug("Finished - {} ms",w.elapsed(TimeUnit.MICROSECONDS));
        }
        return 0;
    }

    protected void setUp() throws Exception{}
    protected abstract void process() throws Exception;
}
