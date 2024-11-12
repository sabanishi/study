package model;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Value;

import java.util.List;
import java.util.stream.Collectors;

@Value
@EqualsAndHashCode(of = {"raw"})
@AllArgsConstructor(staticName = "of")
public class Statement {
    String raw;
    Range lines;
    Range chars;

    @Override
    public String toString() {
        return raw;
    }

    public static Statement joint(List<Statement> statements, Range lines, Range chars) {
        return new Statement(statements.stream().map(Statement::getRaw).collect(Collectors.joining("\n")), lines, chars);
    }
}
