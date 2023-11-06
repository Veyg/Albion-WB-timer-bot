package discord.worldbosses;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class DotEnv {
    public static void load(String filename) {
        Path path = Path.of(filename);
        if (Files.exists(path)) {
            try {
                List<String> lines = Files.readAllLines(path);
                for (String line : lines) {
                    String[] parts = line.split("=", 2);
                    if (parts.length == 2) {
                        String key = parts[0].trim();
                        String value = parts[1].trim();
                        System.setProperty(key, value); // Sets the system property
                    }
                }
            } catch (IOException e) {
                throw new RuntimeException("Failed to load .env file", e);
            }
        }
    }
}
