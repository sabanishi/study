package cmd;

import lombok.extern.slf4j.Slf4j;
import ch.qos.logback.classic.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.concurrent.Callable;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Slf4j
@Command(version ="hal_repair",sortOptions = false,subcommandsRepeatable = true,
        subcommands = { InitCommand.class, ExtractCommand.class,CheckCommand.class, MeasureCommand.class, MatchingCommand.class,
                        MoveCommand.class, SeparateCommand.class})
public class AppCommand implements Callable<Integer> {
    public static class Config{
        @Option(names = "--log", paramLabel = "<level>", description = "log level (default: ${DEFAULT-VALUE})")
        Level logLevel = Level.DEBUG;

        @Option(names = {"-d","--database"}, paramLabel = "<db>", description = "database file path")
        Path database = Path.of("all_normalize.db");
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
