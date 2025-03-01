package de.jutechs.randomItemSpigot;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerDataManager {

    private final File dataFile;          // Now based on the plugin's data folder
    private static final Gson GSON = new Gson();

    private final Map<UUID, PlayerData> playerDataMap = new HashMap<>();

    /**
     * Pass in your plugin's data folder to store the JSON there.
     */
    public PlayerDataManager(File dataFolder) {
        // e.g. dataFolder = .../plugins/RandomItemSpigot/
        // We'll store the JSON in .../plugins/RandomItemSpigot/playtime_rewards.json
        this.dataFile = new File(dataFolder, "playtime_rewards.json");

        createFileIfNotExists();
        loadData();
    }

    /**
     * Fetches the PlayerData for a specific player, creating a default if none exists.
     */
    public PlayerData getPlayerData(Player player) {
        return playerDataMap.computeIfAbsent(player.getUniqueId(), uuid -> new PlayerData());
    }

    /**
     * Saves all current player data to JSON file.
     */
    public void saveData() {
        try (FileWriter writer = new FileWriter(dataFile)) {
            GSON.toJson(playerDataMap, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Reads data from the JSON file (if present) and populates playerDataMap.
     */
    private void loadData() {
        if (!dataFile.exists()) {
            return;
        }

        try (FileReader reader = new FileReader(dataFile)) {
            Type type = new TypeToken<Map<UUID, PlayerData>>() {}.getType();
            Map<UUID, PlayerData> data = GSON.fromJson(reader, type);
            if (data != null) {
                playerDataMap.putAll(data);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Create data file & directories if they do not exist.
     */
    private void createFileIfNotExists() {
        try {
            if (dataFile.getParentFile() != null && !dataFile.getParentFile().exists()) {
                dataFile.getParentFile().mkdirs();
            }
            if (!dataFile.exists()) {
                dataFile.createNewFile();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * A simple container class for each player's data.
     * You can add more fields if needed (such as last login time, etc.).
     */
    public static class PlayerData {
        public int playtimeTicks = 0;
        public int vouchers = 0;  // Start with 0 or a default value

        // If you want a default from config:
        // e.g., Main plugin = JavaPlugin.getPlugin(Main.class);
        // vouchers = plugin.getConfig().getInt("default-vouchers", 0);
        // But this is simpler if done via "computeIfAbsent()" in the manager
    }
}
