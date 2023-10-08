package discord.worldbosses;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import com.google.gson.Gson;

public class ConfigManager {
    private static final Gson gson = new Gson();
    private static final Path DATA_DIRECTORY = Paths.get("data");

    public static String getDesignatedChannelId(String serverId) {
        try {
            String content = readConfigFile(serverId);
            if (content != null) {
                ServerConfig config = gson.fromJson(content, ServerConfig.class);
                if (config != null) {
                    return config.designatedChannelId;
                }
            }
        } catch (IOException e) {
            e.printStackTrace(); // Handle or log the exception
        }
        return null;
    }

    public static void setDesignatedChannelId(String serverId, String channelId) {
        ServerConfig config = new ServerConfig();
        config.designatedChannelId = channelId;
        String json = gson.toJson(config);
        try {
            writeConfigFile(serverId, json);
        } catch (IOException e) {
            e.printStackTrace(); // Handle or log the exception
        }
    }

    public static String getBotToken() {
        try (InputStream is = AlbionBot.class.getResourceAsStream("/global.json")) {
            if (is != null) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                    StringBuilder content = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        content.append(line);
                    }
                    GlobalConfig config = gson.fromJson(content.toString(), GlobalConfig.class);
                    String token = config.botToken;
                    System.out.println("Bot Token: " + token); // Add this line for debugging
                    return token;
                }
            }
        } catch (IOException e) {
            e.printStackTrace(); // Handle or log the exception
        }
        return null;
    }

    public static void setBotToken(String token) {
        GlobalConfig config = new GlobalConfig();
        config.botToken = token;
        String json = gson.toJson(config);
        try {
            writeGlobalConfigFile(json);
        } catch (IOException e) {
            e.printStackTrace(); // Handle or log the exception
        }
    }

    private static String readConfigFile(String serverId) throws IOException {
        Path filePath = DATA_DIRECTORY.resolve(serverId).resolve("config.json");
        if (Files.exists(filePath)) {
            return new String(Files.readAllBytes(filePath));
        }
        return null;
    }

    private static void writeConfigFile(String serverId, String json) throws IOException {
        Path directoryPath = DATA_DIRECTORY.resolve(serverId);
        if (!Files.exists(directoryPath)) {
            Files.createDirectories(directoryPath);
        }
        Path filePath = directoryPath.resolve("config.json");
        Files.write(filePath, json.getBytes());
    }

    private static void writeGlobalConfigFile(String json) throws IOException {
        Path filePath = DATA_DIRECTORY.resolve("global.json");
        Files.write(filePath, json.getBytes());
    }

    private static class ServerConfig {
        public String designatedChannelId;
    }

    private static class GlobalConfig {
        public String botToken;
    }
}
