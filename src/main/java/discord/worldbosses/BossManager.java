package discord.worldbosses;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

public class BossManager {
    private static final String FILE_NAME = "timers.dat";
    private Map<String, String> mapTimers = new HashMap<>();

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

    private void saveTimers() {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(FILE_NAME))) {
            oos.writeObject(mapTimers);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @SuppressWarnings("unchecked")
    private void loadTimers() {
        File file = new File(FILE_NAME);
        if (file.exists()) {
            try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
                mapTimers = (Map<String, String>) ois.readObject();
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
    }
}
