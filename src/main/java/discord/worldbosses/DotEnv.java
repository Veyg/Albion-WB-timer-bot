package discord.worldbosses;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class DotEnv {
    private static final Logger logger = LoggerFactory.getLogger(DotEnv.class);

    public static void load(String filename) {
        Path path = Path.of(filename);
        if (Files.exists(path)) {
            try {
                List<String> lines = Files.readAllLines(path);
                for (String line : lines) {
                    // Skip comments and empty lines
                    if (line.startsWith("#") || line.trim().isEmpty()) {
                        continue;
                    }
                    String[] parts = line.split("=", 2);
                    if (parts.length == 2) {
                        String key = parts[0].trim();
                        String value = parts[1].trim().replaceAll("^\"|\"$", ""); 
                        System.setProperty(key, value);
                    } else {
                        logger.warn("Ignoring malformed line in .env file: {}", line);
                    }
                }
            } catch (IOException e) {
                logger.error("Failed to load .env file", e);
                throw new IllegalStateException("Failed to load .env file", e);
            }
        } else {
            logger.warn(".env file does not exist at path: {}", path);
        }
    }
}
