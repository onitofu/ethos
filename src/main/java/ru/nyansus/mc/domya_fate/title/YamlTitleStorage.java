package ru.nyansus.mc.domya_fate.title;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

public class YamlTitleStorage implements TitleStorage {

    private static final Logger LOGGER = Logger.getLogger(YamlTitleStorage.class.getName());

    private final File file;
    private YamlConfiguration config;

    public YamlTitleStorage(File dataFolder) {
        this.file = new File(dataFolder, "player-titles.yml");
    }

    @Override
    public synchronized int getActiveTitle(UUID uuid) {
        return config.getInt(uuid + ".active", -1);
    }

    @Override
    public synchronized void setActiveTitle(UUID uuid, int titleId) {
        config.set(uuid + ".active", titleId);
    }

    @Override
    public synchronized Set<Integer> getUnlockedTitles(UUID uuid) {
        List<Integer> list = config.getIntegerList(uuid + ".unlocked");
        return new HashSet<>(list);
    }

    @Override
    public synchronized void unlockTitle(UUID uuid, int titleId) {
        Set<Integer> unlocked = getUnlockedTitles(uuid);
        unlocked.add(titleId);
        config.set(uuid + ".unlocked", unlocked.stream().sorted().toList());
    }

    @Override
    public synchronized void revokeTitle(UUID uuid, int titleId) {
        Set<Integer> unlocked = getUnlockedTitles(uuid);
        unlocked.remove(titleId);
        config.set(uuid + ".unlocked", unlocked.stream().sorted().toList());
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
            LOGGER.log(Level.SEVERE, "Failed to save player-titles.yml", e);
        }
    }
}
