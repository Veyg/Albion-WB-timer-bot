package discord.worldbosses;

import java.io.File;
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
            e.printStackTrace(); 
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

    static {
        // Load the .env file only if it exists
        if (new File(".env").exists()) {
            DotEnv.load(".env");
        }
    }


    public static String getBotToken() {
        // Try to get the BOT_TOKEN from the system properties first (local .env file)
        String token = System.getProperty("BOT_TOKEN");
        
        // If not found, try to get it from the system environment variables (production)
        if (token == null || token.isEmpty()) {
            token = System.getenv("BOT_TOKEN");
        }
        
        // If still not found, handle the error
        if (token == null || token.isEmpty()) {
            System.out.println("Token not found. Please check your .env file or environment variables.");
            throw new IllegalStateException("BOT_TOKEN is not set in .env file or environment variables.");
        }
        
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
