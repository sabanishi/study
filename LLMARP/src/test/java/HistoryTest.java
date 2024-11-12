import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.revwalk.RevCommit;
import org.junit.jupiter.api.Test;
import util.RepositoryAccess;

import java.nio.file.Path;

public class HistoryTest {

    @Test
    void test() {
        Path path = Path.of("Test/.git");
        //絶対パスを出力
        System.out.println("Path: " + path.toAbsolutePath().toString());

        //パスに対応するGitリポジトリを取得
        try (RepositoryAccess repositoryAccess = new RepositoryAccess(path)) {
            for (RevCommit commit : repositoryAccess.walk()) {
                System.out.println(commit.getName());
                System.out.println(commit.getFullMessage());

                //変更差分を取得する
                for (DiffEntry diff : repositoryAccess.getChanges(commit)) {
                    //変更差分であり、拡張子が.javaのファイルのみを対象とする
                    if (diff.getChangeType() == DiffEntry.ChangeType.MODIFY
                            && diff.getOldPath().endsWith(".java")
                            && diff.getNewPath().endsWith(".java")) {
                        String oldSource = repositoryAccess.readBlob(diff.getOldId().toObjectId());
                        String newSource = repositoryAccess.readBlob(diff.getNewId().toObjectId());

                        //変更前後のファイルを出力する
                        System.out.println("oldSource:\n" + oldSource);
                        System.out.println("newSource:\n" + newSource);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
