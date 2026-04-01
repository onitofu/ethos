package ru.nyansus.mc.domya_fate.storage;

import ru.nyansus.mc.domya_fate.karma.StatsStorage;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SqliteStatsStorage implements StatsStorage {

    private static final Logger LOGGER = Logger.getLogger(SqliteStatsStorage.class.getName());

    private final DatabaseManager db;

    public SqliteStatsStorage(DatabaseManager db) {
        this.db = db;
    }

    @Override
    public int getStat(UUID uuid, String key) {
        try (PreparedStatement ps = db.getConnection().prepareStatement(
                "SELECT value FROM stats WHERE uuid = ? AND stat_key = ?")) {
            ps.setString(1, uuid.toString());
            ps.setString(2, key);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt("value");
            }
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Failed to get stat " + key + " for " + uuid, e);
        }
        return 0;
    }

    @Override
    public void setStat(UUID uuid, String key, long value) {
        try (PreparedStatement ps = db.getConnection().prepareStatement(
                "INSERT OR REPLACE INTO stats (uuid, stat_key, value) VALUES (?, ?, ?)")) {
            ps.setString(1, uuid.toString());
            ps.setString(2, key);
            ps.setLong(3, value);
            ps.executeUpdate();
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Failed to set stat " + key + " for " + uuid, e);
        }
    }

    @Override
    public long getLongStat(UUID uuid, String key) {
        try (PreparedStatement ps = db.getConnection().prepareStatement(
                "SELECT value FROM stats WHERE uuid = ? AND stat_key = ?")) {
            ps.setString(1, uuid.toString());
            ps.setString(2, key);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getLong("value");
            }
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Failed to get long stat " + key + " for " + uuid, e);
        }
        return 0L;
    }

    @Override
    public void incrementStat(UUID uuid, String key) {
        try (PreparedStatement ps = db.getConnection().prepareStatement(
                "INSERT INTO stats (uuid, stat_key, value) VALUES (?, ?, 1) "
                        + "ON CONFLICT(uuid, stat_key) DO UPDATE SET value = value + 1")) {
            ps.setString(1, uuid.toString());
            ps.setString(2, key);
            ps.executeUpdate();
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Failed to increment stat " + key + " for " + uuid, e);
        }
    }

    @Override
    public void load() {
        // No-op
    }

    @Override
    public void save() {
        // No-op
    }
}
