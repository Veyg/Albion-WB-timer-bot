package discord.worldbosses;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.io.IOException;
import com.google.gson.Gson;

public class ConfigManager {
    private static final String CONFIG_PATH = "config.json";
    private static final Gson gson = new Gson();

    public static String getDesignatedChannelId() {
        try {
            String content = new String(Files.readAllBytes(Paths.get(CONFIG_PATH)));
            Config config = gson.fromJson(content, Config.class);
            return config.designatedChannelId;
        } catch (IOException e) {
            return null;
        }
    }

    public static void setDesignatedChannelId(String channelId) {
        Config config = new Config();
        config.designatedChannelId = channelId;
        String json = gson.toJson(config);
        try {
            Files.write(Paths.get(CONFIG_PATH), json.getBytes());
        } catch (IOException e) {
            // Handle the exception
        }
    }

    public static String getBotToken() {
        try {
            String content = new String(Files.readAllBytes(Paths.get(CONFIG_PATH)));
            Config config = gson.fromJson(content, Config.class);
            return config.botToken;
        } catch (IOException e) {
            return null;
        }
    }

    public static void setBotToken(String token) {
        Config config = new Config();
        config.botToken = token;
        String json = gson.toJson(config);
        try {
            Files.write(Paths.get(CONFIG_PATH), json.getBytes());
        } catch (IOException e) {
            // Handle the exception
        }
    }

    private static class Config {
        public String designatedChannelId;
        public String botToken;
    }
}
