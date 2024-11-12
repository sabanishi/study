import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.stream.Collectors;

public class TestUtils {

    /**
     * 引数のファイルをresourcesフォルダから読み込み、その内容を返す
     *
     * @param filePath
     * @return
     */
    public static String read(String filePath) throws IOException {
        ClassLoader classLoader = TestUtils.class.getClassLoader();
        URL resource = classLoader.getResource(filePath);

        return Files.lines(Paths.get(resource.getPath()), StandardCharsets.UTF_8)
                .collect(Collectors.joining(System.getProperty("line.separator")));
    }
}
