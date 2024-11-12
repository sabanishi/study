import com.github.gumtreediff.client.Run;
import com.github.gumtreediff.gen.jdt.JdtTreeGenerator;
import com.github.gumtreediff.tree.Tree;
import org.junit.jupiter.api.Test;

import java.io.IOException;

public class SandboxTest {

    @Test
    void test() {
        String oldSource = "", newSource = "";
        try {
            oldSource = TestUtils.read("Before.java");
            newSource = TestUtils.read("After.java");
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        System.out.println("Before.java:");
        System.out.println(oldSource);
        System.out.println("\n\nAfter.java:");
        System.out.println(newSource);

        //2つのファイルをGumTreeを用いて抽象構文木に変換する
        Run.initGenerators();
        Tree beforeTree;
        Tree afterTree;
        try {
            beforeTree = createTree(oldSource);
            afterTree = createTree(newSource);
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        //結果を出力
        System.out.println("\n\nBefore.java AST:");
        System.out.println(beforeTree.toTreeString());
        System.out.println("\n\nAfter.java AST:");
        System.out.println(afterTree.toTreeString());
    }

    private Tree createTree(String source) throws Exception {
        return new JdtTreeGenerator().generateFrom().string(source).getRoot();
    }
}
