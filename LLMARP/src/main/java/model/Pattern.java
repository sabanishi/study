package model;

import lombok.AllArgsConstructor;
import lombok.Value;
import model.tree.HalNode;

@Value
@AllArgsConstructor(staticName = "of")
public class Pattern {
    HalNode oldTreeRoot;
    HalNode newTreeRoot;
}
