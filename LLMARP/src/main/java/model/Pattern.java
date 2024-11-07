package model;

import com.github.gumtreediff.tree.Tree;
import lombok.AllArgsConstructor;
import lombok.Value;

@Value
@AllArgsConstructor(staticName="of")
public class Pattern {
    Tree oldTree;
    Tree newTree;
}