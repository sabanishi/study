package model.tree;

import lombok.Value;
import model.Range;

@Value
public class Change {
    Range oldRange;
    String oldText;
    String newText;
}
