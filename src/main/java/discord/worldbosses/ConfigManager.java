package discord.worldbosses;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import com.google.gson.Gson;

public class ConfigManager {
    private static final Gson gson = new Gson();

    public static String getDesignatedChannelId(String serverId) {
        try {
            String content = readConfigFile(serverId);
            if (content != null) {
                Config config = gson.fromJson(content, Config.class);
                return config.designatedChannelId;
            }
        } catch (IOException e) {
            // Handle the exception
        }
        return null;
    }

    public static void setDesignatedChannelId(String serverId, String channelId) {
        Config config = new Config();
        config.designatedChannelId = channelId;
        String json = gson.toJson(config);
        try {
            writeConfigFile(serverId, json);
        } catch (IOException e) {
            // Handle the exception
        }
    }

    public static String getBotToken() {
        try {
            InputStream inputStream = ConfigManager.class.getResourceAsStream("/global.json");
            if (inputStream != null) {
                byte[] bytes = inputStream.readAllBytes();
                String content = new String(bytes, StandardCharsets.UTF_8);
                Config config = gson.fromJson(content, Config.class);
                return config.botToken;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void setBotToken(String token) {
        Config config = new Config();
        config.botToken = token;
        String json = gson.toJson(config);
        try {
            writeConfigFile("global", json);
        } catch (IOException e) {
            // Handle the exception
        }
    }

    private static String readConfigFile(String fileName) throws IOException {
        Path filePath = Paths.get("data", fileName, "config.json");
        if (Files.exists(filePath)) {
            return new String(Files.readAllBytes(filePath));
        }
        return null;
    }

    private static void writeConfigFile(String fileName, String json) throws IOException {
        Path directoryPath = Paths.get("data", fileName);
        if (!Files.exists(directoryPath)) {
            Files.createDirectories(directoryPath);
        }
        Path filePath = directoryPath.resolve("config.json");
        Files.write(filePath, json.getBytes());
    }

    private static class Config {
        public String designatedChannelId;
        public String botToken;
    }
}
