package discord.worldbosses;

import java.util.HashMap;
import java.util.Map;

public class BossManager {
    // Map of map name to spawn time
    private Map<String, String> mapTimers = new HashMap<>();

    public void addTimer(String mapName, String time) {
        mapTimers.put(mapName, time);
    }

    public String getTimer(String mapName) {
        return mapTimers.get(mapName);
    }

    // ... Additional methods for managing timers
}
