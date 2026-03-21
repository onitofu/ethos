package ru.nyansus.mc.domya_fate.karma;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

public class YamlKarmaStorage implements KarmaStorage {

    private static final Logger LOGGER = Logger.getLogger(YamlKarmaStorage.class.getName());

    private final File file;
    private YamlConfiguration config;

    public YamlKarmaStorage(File dataFolder) {
        this.file = new File(dataFolder, "karma.yml");
    }

    @Override
    public synchronized int getKarma(UUID uuid) {
        return config.getInt(uuid + ".karma", 0);
    }

    @Override
    public synchronized long getLastUpdate(UUID uuid) {
        return config.getLong(uuid + ".last-update", System.currentTimeMillis());
    }

    @Override
    public synchronized void setKarma(UUID uuid, int karma, long timestamp) {
        config.set(uuid + ".karma", karma);
        config.set(uuid + ".last-update", timestamp);
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
            LOGGER.log(Level.SEVERE, "Failed to save karma.yml", e);
        }
    }
}
