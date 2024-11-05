package diff;

import model.Range;
import model.Statement;

import java.util.ArrayList;
import java.util.List;

public class Splitter implements ISplitter{

    @Override
    public List<Statement> split(String source) {
        final List<Statement> result = new ArrayList<>();
        List<String> lines = List.of(source.split("\\r\\n|\\r|\\n"));
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            Range range = Range.of(i, i + 1);
            result.add(Statement.of(line, range));
        }
        System.out.println(result);
        return result;
    }
}
