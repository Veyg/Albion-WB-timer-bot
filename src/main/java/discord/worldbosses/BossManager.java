package discord.worldbosses;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.*;
import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.HashSet;
import java.util.List;

public class BossManager {
    private static final String FILE_NAME = "timers.json";
    private Map<String, TimerData> mapTimers = new HashMap<>();
    private final Gson gson = new Gson();
    private Set<String> skippedAndForgottenBosses = new HashSet<>();

    public Set<String> getSkippedAndForgottenBosses() {
        return new HashSet<>(skippedAndForgottenBosses);
    }

    public BossManager() {
        loadTimers();
    }

    public List<Map.Entry<String, TimerData>> getSortedTimers() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss d/MM/yyyy");
        return mapTimers.entrySet().stream()
                .sorted(Comparator.comparing(e -> LocalDateTime.parse(e.getValue().getBossSpawnTime(), formatter)))
                .collect(Collectors.toList());
    }

    public void addTimer(String mapName, String time) {
        System.out.println("Added timer for " + mapName + " at " + time);
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
        System.out.println(
                "Edited timer for " + mapName + " to " + newTime);
        mapTimers.put(mapName, new TimerData(newTime));
        saveTimers();
    }

    public void deleteTimer(String mapName) {
        mapTimers.remove(mapName);
        System.out.println("Deleted timer for " + mapName);
        saveTimers();
    }

    public void saveTimers() {
        System.out.println("Saving timers to JSON file...");
        try (Writer writer = new FileWriter(FILE_NAME)) {
            gson.toJson(mapTimers, writer);
            System.out.println("Timers saved successfully.");
        } catch (IOException e) {
            System.err.println("Error saving timers to file: " + e.getMessage());
            e.printStackTrace();
        }
    }
    

    private void loadTimers() {
        File file = new File(FILE_NAME);
        if (file.exists()) {
            try (Reader reader = new FileReader(file)) {
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
                System.out.println("Loaded timers: " + mapTimers);
            } catch (IOException e) {
                e.printStackTrace();
            }
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
                System.err.println("Error parsing killed time: " + killedDateTime);
                return;
            }
            
            // Set the new spawn time
            String formattedDateTime = newSpawnDateTime.format(dateTimeFormatter);
            data.setBossSpawnTime(formattedDateTime);
            System.out.println("Updated spawn time for " + mapName + " to " + formattedDateTime);
            
            // Reset the status
            data.setStatus(null);
            data.setStatusTime(null);
            saveTimers();
        } else {
            System.err.println("No timer data found for " + mapName);
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
            data.setStatus("forgotten");
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
