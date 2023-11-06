package discord.worldbosses;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

public class ConfigManager {
    private static final Logger logger = LoggerFactory.getLogger(ConfigManager.class);
    private static final Gson gson = new Gson();
    private static final Path DATA_DIRECTORY = Paths.get("data");

    static {
        // Load the .env file only if it exists
        if (Files.exists(Paths.get(".env"))) {
            DotEnv.load(".env");
        }
    }

    public static String getDesignatedChannelId(String serverId) {
        try {
            String content = readConfigFile(serverId);
            if (content != null) {
                ServerConfig config = gson.fromJson(content, ServerConfig.class);
                return config.designatedChannelId;
            }
        } catch (IOException e) {
            logger.error("Error reading configuration for server: {}", serverId, e);
        } catch (JsonSyntaxException e) {
            logger.error("Malformed JSON in configuration for server: {}", serverId, e);
        }
        return null;
    }

    public static void setDesignatedChannelId(String serverId, String channelId) {
        ServerConfig config = new ServerConfig(channelId);
        String json = gson.toJson(config);
        try {
            writeConfigFile(serverId, json);
        } catch (IOException e) {
            logger.error("Error writing configuration for server: {}", serverId, e);
        }
    }

    public static String getBotToken() {
        String token = System.getProperty("BOT_TOKEN", System.getenv("BOT_TOKEN"));
        if (token == null || token.isEmpty()) {
            logger.error("BOT_TOKEN is not set in .env file or environment variables.");
            throw new IllegalStateException("BOT_TOKEN is not set in .env file or environment variables.");
        }
        return token;
    }

    private static String readConfigFile(String serverId) throws IOException {
        Path filePath = DATA_DIRECTORY.resolve(serverId).resolve("config.json");
        if (Files.exists(filePath)) {
            return Files.readString(filePath);
        }
        return null;
    }

    private static void writeConfigFile(String serverId, String json) throws IOException {
        Path directoryPath = DATA_DIRECTORY.resolve(serverId);
        Files.createDirectories(directoryPath);
        Path filePath = directoryPath.resolve("config.json");
        Files.writeString(filePath, json);
    }

    private static class ServerConfig {
        String designatedChannelId;

        ServerConfig(String designatedChannelId) {
            this.designatedChannelId = designatedChannelId;
        }
    }
}
