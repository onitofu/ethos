package ru.nyansus.mc.domya_fate.karma;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

public class StatsStorage {

    private static final Logger LOGGER = Logger.getLogger(StatsStorage.class.getName());

    private final File file;
    private YamlConfiguration config;

    public StatsStorage(File dataFolder) {
        this.file = new File(dataFolder, "stats.yml");
    }

    public synchronized int getStat(UUID uuid, String key) {
        return config.getInt(uuid + "." + key, 0);
    }

    public synchronized void incrementStat(UUID uuid, String key) {
        int current = getStat(uuid, key);
        config.set(uuid + "." + key, current + 1);
    }

    public synchronized void load() {
        if (!file.exists()) {
            config = new YamlConfiguration();
            return;
        }
        config = YamlConfiguration.loadConfiguration(file);
    }

    public synchronized void save() {
        try {
            file.getParentFile().mkdirs();
            config.save(file);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to save stats.yml", e);
        }
    }
}
