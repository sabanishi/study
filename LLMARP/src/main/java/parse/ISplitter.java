package parse;

import model.Statement;

import java.util.List;

public interface ISplitter {
    List<Statement> split(final String source);
}
