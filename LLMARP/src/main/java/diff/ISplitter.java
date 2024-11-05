package diff;

import model.Statement;

import java.util.List;

public interface ISplitter {
    public List<Statement> split(final String source);
}
