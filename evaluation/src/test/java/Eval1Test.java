import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.junit.jupiter.api.Test;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

@Slf4j
public class Eval1Test {
    @Test
    void test() throws Exception{
        Path folderPath = Path.of("../../higuchi/chaa/match");
        Path matchPath = folderPath.resolve("match_log.csv");
        //csvファイルを読み込む
        CsvMapper mapper = new CsvMapper();
        CsvSchema schema = CsvSchema.emptySchema().withHeader();
        MappingIterator<Object> iterator = mapper.readerFor(Object.class)
                .with(schema)
                .readValues(new File(matchPath.toString()));

        int count = 0;
        while (iterator.hasNext()) {
            iterator.next();
            count++;
        }

        log.info("count: {}",count);

        for(int i=1;i<count;i++){
            Path beforePath = folderPath.resolve(i+"-before.java");
            Path afterPath = folderPath.resolve(i+"-after.java");

            //ファイルの内容を読み込む
            String before = Files.readString(beforePath);
            String after = Files.readString(afterPath);

            boolean isBrokenBefore = isBroken(before);
            boolean isBrokenAfter = isBroken(after);

            if(!isBrokenBefore || !isBrokenAfter){
                log.info("before: {}",before);
                log.info("after: {}",after);
            }
        }
    }

    private boolean isBroken(String source){
        ASTParser parser = ASTParser.newParser(AST.JLS16);
        parser.setResolveBindings(true);
        parser.setSource(source.toCharArray());
        CompilationUnit unit = (CompilationUnit) parser.createAST(null);
        IProblem[] problems = unit.getProblems();
        if(problems == null || problems.length > 0){
            return true;
        }else{
            return false;
        }
    }
}