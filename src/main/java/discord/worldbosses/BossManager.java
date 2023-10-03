package discord.worldbosses;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.*;
import java.lang.reflect.Type;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;

public class BossManager {
    private static final String FILE_NAME = "timers.json";
    private Map<String, TimerData> mapTimers = new HashMap<>();
    private final Gson gson = new Gson();
    private Set<String> skippedAndForgottenBosses = new HashSet<>();
    private static final int NOTIFICATION_OFFSET_MINUTES = 20;
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss d/MM/yyyy");

    public Set<String> getSkippedAndForgottenBosses() {
        return new HashSet<>(skippedAndForgottenBosses);
    }

    public BossManager() {
        loadTimers();
    }

    public void addTimer(String mapName, String time) {
        String notificationTime = calculateNotificationTime(time);
        System.out.println("Added timer for " + mapName + " at " + time + " with notification at " + notificationTime);
        mapTimers.put(mapName, new TimerData(time, notificationTime));
        skippedAndForgottenBosses.remove(mapName);
        saveTimers();
    }

    public String getBossSpawnTime(String mapName) {
        TimerData data = mapTimers.get(mapName);
        return data == null ? null : data.getBossSpawnTime();
    }

    public String getNotificationTime(String mapName) {
        TimerData data = mapTimers.get(mapName);
        return data == null ? null : data.getNotificationTime();
    }

    public Map<String, TimerData> getAllTimers() {
        return new HashMap<>(mapTimers);
    }

    public void editTimer(String mapName, String newTime) {
        String notificationTime = calculateNotificationTime(newTime);
        System.out.println(
                "Edited timer for " + mapName + " to " + newTime + " with notification at " + notificationTime);
        mapTimers.put(mapName, new TimerData(newTime, notificationTime));
        saveTimers();
    }

    public void deleteTimer(String mapName) {
        mapTimers.remove(mapName);
        System.out.println("Deleted timer for " + mapName);
        saveTimers();
    }

    private String calculateNotificationTime(String bossSpawnTime) {
        
        LocalDateTime bossTime = LocalDateTime.parse(bossSpawnTime, DATE_TIME_FORMATTER);
        LocalDateTime notificationTime = bossTime.minusMinutes(NOTIFICATION_OFFSET_MINUTES);
        return notificationTime.format(DATE_TIME_FORMATTER);
    }

    public void saveTimers() {
        System.out.println("Saving timers to JSON file...");
        try (Writer writer = new FileWriter(FILE_NAME)) {
            gson.toJson(mapTimers, writer);
        } catch (IOException e) {
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

    public void markBossAsKilled(String mapName, String killedTime) {
        TimerData data = mapTimers.get(mapName);
        if (data != null) {
            // Get the current date and add 2 days
            LocalDate currentDatePlusTwoDays = LocalDate.now(ZoneOffset.UTC).plusDays(2);

            // Combine the provided killedTime with the new date
            DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");
            LocalTime killedLocalTime = LocalTime.parse(killedTime, timeFormatter);
            LocalDateTime newSpawnDateTime = LocalDateTime.of(currentDatePlusTwoDays, killedLocalTime);

            // Set the new spawn time and notification time
            DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss d/MM/yyyy");
            data.setBossSpawnTime(newSpawnDateTime.format(dateTimeFormatter));
            data.setNotificationTime(calculateNotificationTime(newSpawnDateTime.format(dateTimeFormatter)));

            // Reset the status
            data.setStatus(null);
            data.setStatusTime(null);
            saveTimers();
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
        private String notificationTime;
        private String status;
        private String statusTime;

        public TimerData(String bossSpawnTime, String notificationTime) {
            this.bossSpawnTime = bossSpawnTime;
            this.notificationTime = notificationTime;
        }

        public String getBossSpawnTime() {
            return bossSpawnTime;
        }

        public void setBossSpawnTime(String bossSpawnTime) {
            this.bossSpawnTime = bossSpawnTime;
        }

        public String getNotificationTime() {
            return notificationTime;
        }

        public void setNotificationTime(String notificationTime) {
            this.notificationTime = notificationTime;
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
