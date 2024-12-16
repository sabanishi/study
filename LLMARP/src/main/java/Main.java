import cmd.AppCommand;
import picocli.CommandLine;
import picocli.CommandLine.RunAll;

public class Main {
    public static void main(String[] args) {
        final AppCommand app = new AppCommand();
        final CommandLine cmdline = new CommandLine(app);
        cmdline.setExecutionStrategy(new RunAll());
        cmdline.setExpandAtFiles(false);

        final int status = cmdline.execute(args);
        System.exit(status);
    }
}
