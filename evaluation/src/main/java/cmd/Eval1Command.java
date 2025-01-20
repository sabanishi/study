package cmd;

import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Hashtable;

@Slf4j
@Command(name = "eval1", description = "Evaluate 1st task")
public class Eval1Command extends BaseCommand{
    public static class Config{
        @Option(names = "--folder", paramLabel = "<path>", description = "folder path (default: ${DEFAULT-VALUE})")
        String folderPath = "match";
        @Option(names = "--output", paramLabel = "<file>", description = "output file name (default: ${DEFAULT-VALUE})")
        String outputFileName = "match.csv";
    }

    private Config config = new Config();

    @Override
    protected void process() throws Exception {
        Path folderPath = Path.of(config.folderPath);

        int beforeNotBroken = 0;
        int afterBroken = 0;
        StringBuilder broken = new StringBuilder();
        for(int i=0;i<=172;i++){
            Path beforePath = folderPath.resolve(i+"_before.java");
            Path afterPath = folderPath.resolve(i+"_after.java");

            //ファイルの内容を読み込む
            String before = Files.readString(beforePath);
            String after = Files.readString(afterPath);

            boolean isBrokenBefore = isBroken(before);
            boolean isBrokenAfter = isBroken(after);

            log.info("count: {}",i);
            if(!isBrokenBefore){
                beforeNotBroken++;
                //Beforeが壊れていないものに対して、Afterが壊れている場合は出力する
                if(isBrokenAfter){
                    broken.append(i).append("\n");
                    afterBroken++;
                }
            }
        }

        //数を出力
        log.info("{} / {}",afterBroken,beforeNotBroken);

        //CSVファイルに出力
        broken.append("\n").append(afterBroken).append(",").append(beforeNotBroken);
        Path outputPath = folderPath.resolve(config.outputFileName);
        Files.writeString(outputPath,broken.toString());
    }

    private boolean isBroken(String source){
        ASTParser parser = ASTParser.newParser(AST.JLS16);
        parser.setResolveBindings(true);
        Hashtable<String, String> options = JavaCore.getDefaultOptions();
        options.put(JavaCore.COMPILER_SOURCE, JavaCore.VERSION_16);
        options.put(JavaCore.COMPILER_COMPLIANCE, JavaCore.VERSION_16);
        options.put(JavaCore.COMPILER_CODEGEN_TARGET_PLATFORM, JavaCore.VERSION_16);
        parser.setCompilerOptions(options);

        parser.setSource(source.toCharArray());
        CompilationUnit unit = (CompilationUnit) parser.createAST(null);
        IProblem[] problems = unit.getProblems();
        if(problems == null || problems.length > 0){
            if(problems == null) {
                log.error("Error!!!!!!: problems is null");
            }else{
                for(IProblem problem : problems){
                    log.error("Error!!!: {}", problem.getMessage());
                }
            }
            return true;
        }else{
            return false;
        }
    }
}
