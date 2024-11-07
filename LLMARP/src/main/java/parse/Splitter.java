package parse;

import model.Range;
import model.Statement;

import java.util.ArrayList;
import java.util.List;

public class Splitter implements ISplitter{

    @Override
    public List<Statement> split(String source) {
        final List<Statement> result = new ArrayList<>();
        final int lfLength = System.getProperty("line.separator").length();
        List<String> lines = List.of(source.split("\\r\\n|\\r|\\n"));

        int charStart = 0;
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            Range lineRange = Range.of(i, i + 1);
            Range charRange = Range.of(charStart, charStart + line.length());
            result.add(Statement.of(line,lineRange,charRange));
            //改行も文字数に含める
            charStart += line.length() + lfLength;
        }
        return result;
    }
}
