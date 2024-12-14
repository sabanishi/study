package util;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Collectors;

@Slf4j
public class FileUtil {
    /**
     * 引数のファイルをresourcesフォルダから読み込み、その内容を返す
     */
    public static String read(Path path) throws IOException {
        return Files.lines(path, StandardCharsets.UTF_8)
                .collect(Collectors.joining(System.getProperty("line.separator")));
    }

    public static String write(Path path, String content) throws IOException{
        log.debug("Writing to {}", path);
        Files.write(path, content.getBytes());
        return content;
    }
}
