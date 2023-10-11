package discord.worldbosses;

import java.io.IOException;
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
        String token = System.getenv("BOT_TOKEN");
        System.out.println("Token fetched: " + (token != null && !token.isEmpty()));
        return token;
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


 private static class ServerConfig {
        public String designatedChannelId;
    }
}
