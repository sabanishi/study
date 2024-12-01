import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;

@Slf4j
public class LLMUser {
    /**
     * LLMにテキストを送信してその返信結果を返す
     */
    public String send(String systemMessage, String userMessage,float temperature) {
        try {
            String reformattedUserMessage = reformatText(userMessage);
            String reformattedSystemMessage = reformatText(systemMessage);

            URL url = new URL("http://127.0.0.1:5000/generate");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);

            String json =
                    "{" +
                            "\"system\": \"" + reformattedSystemMessage + "\"," +
                            "\"user\": \"" + reformattedUserMessage + "\"," +
                            "\"temperature\": " + temperature +
                            "}";

            //log.info("Request: {}", json);

            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = json.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            int responseCode = conn.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                //結果を取得
                return new String(conn.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            }

        } catch (MalformedURLException e) {
            log.error(e.getMessage());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return "";
    }

    private String reformatText(String text) {
        return text.replace("\"", "\\\"").replace("\n", "\\n");
    }
}
