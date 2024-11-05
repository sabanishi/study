package utils;

import com.github.gumtreediff.gen.jdt.JdtTreeGenerator;
import com.github.gumtreediff.tree.Tree;

import java.io.IOException;

public class TreeUtils {
    public static Tree createTree(String source){
        try {
            return new JdtTreeGenerator().generateFrom().string(source).getRoot();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
