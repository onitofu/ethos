package ru.nyansus.mc.domya_fate.storage;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class YamlMigrator {

    private static final Logger LOGGER = Logger.getLogger(YamlMigrator.class.getName());

    private final File dataFolder;
    private final DatabaseManager db;
    private final Map<Integer, Integer> idMap;

    public YamlMigrator(JavaPlugin plugin, DatabaseManager db) {
        this.dataFolder = plugin.getDataFolder();
        this.db = db;
        this.idMap = loadIdMap(plugin);
    }

    private Map<Integer, Integer> loadIdMap(JavaPlugin plugin) {
        Map<Integer, Integer> map = new HashMap<>();
        try (InputStream stream = plugin.getResource("id-migration.yml")) {
            if (stream == null) {
                return map;
            }
            YamlConfiguration config = YamlConfiguration.loadConfiguration(
                    new InputStreamReader(stream, StandardCharsets.UTF_8));
            for (String key : config.getKeys(false)) {
                try {
                    map.put(Integer.parseInt(key), config.getInt(key));
                } catch (NumberFormatException e) {
                    LOGGER.warning("Invalid migration key: " + key);
                }
            }
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Failed to load id-migration.yml", e);
        }
        LOGGER.info("Loaded " + map.size() + " ID mappings for migration");
        return map;
    }

    public boolean needsMigration() {
        File karmaFile = new File(dataFolder, "karma.yml");
        File titlesFile = new File(dataFolder, "player-titles.yml");
        File statsFile = new File(dataFolder, "stats.yml");
        return karmaFile.exists() || titlesFile.exists() || statsFile.exists();
    }

    public void migrate() {
        LOGGER.info("Starting YAML to SQLite migration...");
        Connection conn = db.getConnection();

        try {
            conn.setAutoCommit(false);

            int karmaCount = migrateKarma(conn);
            int titleCount = migrateTitles(conn);
            int statCount = migrateStats(conn);

            conn.commit();
            conn.setAutoCommit(true);

            LOGGER.info("Migration complete: " + karmaCount + " karma records, "
                    + titleCount + " title records, " + statCount + " stat records");

            renameOldFiles();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Migration failed, rolling back", e);
            try {
                conn.rollback();
                conn.setAutoCommit(true);
            } catch (SQLException ex) {
                LOGGER.log(Level.SEVERE, "Rollback failed", ex);
            }
        }
    }

    private int migrateKarma(Connection conn) throws SQLException {
        File file = new File(dataFolder, "karma.yml");
        if (!file.exists()) {
            return 0;
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        int count = 0;

        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT OR REPLACE INTO karma (uuid, karma, last_update) VALUES (?, ?, ?)")) {
            for (String uuid : config.getKeys(false)) {
                ConfigurationSection section = config.getConfigurationSection(uuid);
                if (section == null) {
                    continue;
                }
                ps.setString(1, uuid);
                ps.setInt(2, section.getInt("karma", 0));
                ps.setLong(3, section.getLong("last-update", System.currentTimeMillis()));
                ps.addBatch();
                count++;
            }
            ps.executeBatch();
        }
        return count;
    }

    private int migrateTitles(Connection conn) throws SQLException {
        File file = new File(dataFolder, "player-titles.yml");
        if (!file.exists()) {
            return 0;
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        int count = 0;

        String unlockSql = "INSERT OR IGNORE INTO player_titles (uuid, title_id) VALUES (?, ?)";
        String activeSql = "INSERT OR REPLACE INTO active_title (uuid, title_id) VALUES (?, ?)";
        try (PreparedStatement unlockPs = conn.prepareStatement(unlockSql);
                PreparedStatement activePs = conn.prepareStatement(activeSql)) {

            for (String uuid : config.getKeys(false)) {
                ConfigurationSection section = config.getConfigurationSection(uuid);
                if (section == null) {
                    continue;
                }

                List<Integer> unlocked = section.getIntegerList("unlocked");
                for (int oldId : unlocked) {
                    Integer newId = idMap.get(oldId);
                    if (newId == null) {
                        continue;
                    }
                    unlockPs.setString(1, uuid);
                    unlockPs.setInt(2, newId);
                    unlockPs.addBatch();
                    count++;
                }

                int active = section.getInt("active", -1);
                Integer newActive = idMap.get(active);
                if (newActive == null) {
                    newActive = 0;
                }
                activePs.setString(1, uuid);
                activePs.setInt(2, newActive);
                activePs.addBatch();
            }
            unlockPs.executeBatch();
            activePs.executeBatch();
        }
        return count;
    }

    private int migrateStats(Connection conn) throws SQLException {
        File file = new File(dataFolder, "stats.yml");
        if (!file.exists()) {
            return 0;
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        int count = 0;

        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT OR REPLACE INTO stats (uuid, stat_key, value) VALUES (?, ?, ?)")) {
            for (String uuid : config.getKeys(false)) {
                ConfigurationSection section = config.getConfigurationSection(uuid);
                if (section == null) {
                    continue;
                }
                for (String key : section.getKeys(false)) {
                    ps.setString(1, uuid);
                    ps.setString(2, key);
                    ps.setLong(3, section.getLong(key, 0));
                    ps.addBatch();
                    count++;
                }
            }
            ps.executeBatch();
        }
        return count;
    }

    private void renameOldFiles() {
        renameFile("karma.yml");
        renameFile("player-titles.yml");
        renameFile("stats.yml");
    }

    private void renameFile(String name) {
        File file = new File(dataFolder, name);
        if (file.exists()) {
            File backup = new File(dataFolder, name + ".migrated");
            if (file.renameTo(backup)) {
                LOGGER.info("Renamed " + name + " -> " + name + ".migrated");
            } else {
                LOGGER.warning("Failed to rename " + name);
            }
        }
    }
}
