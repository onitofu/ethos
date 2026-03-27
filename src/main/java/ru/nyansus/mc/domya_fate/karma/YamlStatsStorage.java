package ru.nyansus.mc.domya_fate.karma;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

public class YamlStatsStorage implements StatsStorage {

    private static final Logger LOGGER = Logger.getLogger(YamlStatsStorage.class.getName());

    private final File file;
    private YamlConfiguration config;

    public YamlStatsStorage(File dataFolder) {
        this.file = new File(dataFolder, "stats.yml");
    }

    @Override
    public synchronized int getStat(UUID uuid, String key) {
        return config.getInt(uuid + "." + key, 0);
    }

    @Override
    public synchronized void setStat(UUID uuid, String key, long value) {
        config.set(uuid + "." + key, value);
    }

    @Override
    public synchronized long getLongStat(UUID uuid, String key) {
        return config.getLong(uuid + "." + key, 0L);
    }

    @Override
    public synchronized void incrementStat(UUID uuid, String key) {
        int current = getStat(uuid, key);
        config.set(uuid + "." + key, current + 1);
    }

    @Override
    public synchronized void load() {
        if (!file.exists()) {
            config = new YamlConfiguration();
            return;
        }
        config = YamlConfiguration.loadConfiguration(file);
    }

    @Override
    public synchronized void save() {
        try {
            file.getParentFile().mkdirs();
            config.save(file);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to save stats.yml", e);
        }
    }
}
