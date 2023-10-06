package discord.worldbosses;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;

public class BossManager {
    private Map<String, TimerData> mapTimers = new HashMap<>();
    private final Gson gson = new Gson();
    private Set<String> skippedAndForgottenBosses = new HashSet<>();
    private static final Logger logger = LoggerFactory.getLogger(BossManager.class);

    public Set<String> getSkippedAndForgottenBosses() {
        return new HashSet<>(skippedAndForgottenBosses);
    }

    private String FILE_NAME;

    public BossManager(String serverId) {
        this.FILE_NAME = "data/" + serverId + "/timers.json";
        loadTimers();
        System.out.println("Server ID: " + serverId); // Add this line

    }

    public List<Map.Entry<String, TimerData>> getSortedTimers() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss d/MM/yyyy");
        return mapTimers.entrySet().stream()
                .sorted(Comparator.comparing(e -> LocalDateTime.parse(e.getValue().getBossSpawnTime(), formatter)))
                .collect(Collectors.toList());
    }

    public void addTimer(String mapName, String time) {
        logger.info("Added timer for {} at {}", mapName, time);
        mapTimers.put(mapName, new TimerData(time));
        skippedAndForgottenBosses.remove(mapName);
        saveTimers();
    }

    public String getBossSpawnTime(String mapName) {
        TimerData data = mapTimers.get(mapName);
        return data == null ? null : data.getBossSpawnTime();
    }

    public Map<String, TimerData> getAllTimers() {
        return new HashMap<>(mapTimers);
    }

    public void editTimer(String mapName, String newTime) {
        logger.info("Edited timer for {} to {}", mapName, newTime);
        mapTimers.put(mapName, new TimerData(newTime));
        saveTimers();
    }

    public void deleteTimer(String mapName) {
        mapTimers.remove(mapName);
        logger.info("Deleted timer for {}", mapName);
        saveTimers();
    }

    private void loadTimers() {
        try (InputStream is = new FileInputStream(FILE_NAME);
                Reader reader = new InputStreamReader(is)) {
            Type type = new TypeToken<Map<String, TimerData>>() {
            }.getType();
            mapTimers = gson.fromJson(reader, type);
            if (mapTimers == null) {
                mapTimers = new HashMap<>();
            } else {
                // Populate the skippedAndForgottenBosses set
                for (Map.Entry<String, TimerData> entry : mapTimers.entrySet()) {
                    if ("Skipped".equals(entry.getValue().getStatus())
                            || "Forgotten".equals(entry.getValue().getStatus())) {
                        skippedAndForgottenBosses.add(entry.getKey());
                    }
                }
            }
            logger.info("Loaded timers: {}", mapTimers);
        } catch (IOException e) {
            logger.error("Error loading timers", e);
        }
    }

    public void saveTimers() {
        logger.info("Saving timers to JSON file...");

        // Ensure the directory exists
        File directory = new File(FILE_NAME).getParentFile();
        if (!directory.exists()) {
            directory.mkdirs();
        }

        try (OutputStream os = new FileOutputStream(FILE_NAME);
                Writer writer = new OutputStreamWriter(os)) {
            gson.toJson(mapTimers, writer);
            logger.info("Timers saved successfully.");
        } catch (IOException e) {
            logger.error("Error saving timers to file", e);
        }
    }

    public void markBossAsKilled(String mapName, String killedDateTime) {
        TimerData data = mapTimers.get(mapName);
        if (data != null) {
            // Try to parse the provided killedDateTime
            DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss d/MM/yyyy");
            LocalDateTime newSpawnDateTime;
            try {
                newSpawnDateTime = LocalDateTime.parse(killedDateTime, dateTimeFormatter);
            } catch (DateTimeParseException e) {
                logger.error("Error parsing killed time: {}", killedDateTime, e);
                return;
            }

            // Set the new spawn time
            String formattedDateTime = newSpawnDateTime.format(dateTimeFormatter);
            data.setBossSpawnTime(formattedDateTime);
            logger.info("Updated spawn time for {} to {}", mapName, formattedDateTime);

            // Reset the status
            data.setStatus(null);
            data.setStatusTime(null);
            saveTimers();
        } else {
            logger.error("No timer data found for {}", mapName);
        }
    }

    public void markBossAsSkipped(String mapName) {
        TimerData data = mapTimers.get(mapName);
        if (data != null) {
            data.setStatus("Skipped");
            data.setStatusTime(
                    LocalDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ofPattern("HH:mm:ss d/MM/yyyy")));
            skippedAndForgottenBosses.add(mapName);
            saveTimers();
        }
    }

    public void markBossAsForgotten(String mapName) {
        TimerData data = mapTimers.get(mapName);
        if (data != null) {
            data.setStatus("Forgotten");
            skippedAndForgottenBosses.add(mapName);
            data.setStatusTime(
                    LocalDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ofPattern("HH:mm:ss d/MM/yyyy")));
            saveTimers();
        }
    }

    public boolean isSkippedOrForgotten(String mapName) {
        TimerData data = getAllTimers().get(mapName);
        if (data != null) {
            return "Skipped".equals(data.getStatus()) || "Forgotten".equals(data.getStatus());
        }
        return false;
    }

    public static class TimerData {
        private String bossSpawnTime;
        private String status;
        private String statusTime;

        public TimerData(String bossSpawnTime) {
            this.bossSpawnTime = bossSpawnTime;
        }

        public String getBossSpawnTime() {
            return bossSpawnTime;
        }

        public void setBossSpawnTime(String bossSpawnTime) {
            this.bossSpawnTime = bossSpawnTime;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public String getStatusTime() {
            return statusTime;
        }

        public void setStatusTime(String statusTime) {
            this.statusTime = statusTime;
        }
    }
}
