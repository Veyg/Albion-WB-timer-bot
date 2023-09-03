package discord.worldbosses;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.*;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

public class BossManager {
    private static final String FILE_NAME = "timers.json";
    private Map<String, String> mapTimers = new HashMap<>();
    private final Gson gson = new Gson();

    public BossManager() {
        loadTimers();
    }

    public void addTimer(String mapName, String time) {
        mapTimers.put(mapName, time);
        saveTimers();
    }

    public String getTimer(String mapName) {
        return mapTimers.get(mapName);
    }

    public Map<String, String> getAllTimers() {
        return new HashMap<>(mapTimers);
    }

    public void editTimer(String mapName, String newTime) {
        mapTimers.put(mapName, newTime);
        saveTimers();
    }

    public void deleteTimer(String mapName) {
        mapTimers.remove(mapName);
        saveTimers();
    }

    private void saveTimers() {
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
                Type type = new TypeToken<Map<String, String>>() {}.getType();
                mapTimers = gson.fromJson(reader, type);
                if (mapTimers == null) {
                    mapTimers = new HashMap<>();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
