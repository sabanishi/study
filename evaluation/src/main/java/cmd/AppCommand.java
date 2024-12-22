package cmd;

import ch.qos.logback.classic.Level;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine.Option;
import picocli.CommandLine.Command;

import java.util.concurrent.Callable;

@Slf4j
@Command(version = "hal_repair_eval",sortOptions = false,subcommands = {Eval1Command.class})
public class AppCommand implements Callable<Integer> {
    public static class Config{
        @Option(names = "--log", paramLabel = "<level>", description = "log level (default: ${DEFAULT-VALUE})")
        Level logLevel = Level.DEBUG;
    }

    protected Config config = new Config();


    @Override
    public Integer call(){
        setLoggerLevel(Logger.ROOT_LOGGER_NAME,config.logLevel);
        if(config.logLevel == Level.DEBUG || config.logLevel == Level.TRACE){
            setLoggerLevel("org.eclipse.jgit", Level.INFO);
            setLoggerLevel("org.springframework", Level.INFO);
        }

        log.debug("Set log level to {}",config.logLevel);
        return 0;
    }

    private void setLoggerLevel(final String name,final Level level){
        final ch.qos.logback.classic.Logger logger = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(name);
        logger.setLevel(level);
        log.debug("Set log level of {} to {}", name,level);
    }
}
