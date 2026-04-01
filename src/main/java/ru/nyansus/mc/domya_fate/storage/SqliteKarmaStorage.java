package ru.nyansus.mc.domya_fate.storage;

import ru.nyansus.mc.domya_fate.karma.KarmaStorage;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SqliteKarmaStorage implements KarmaStorage {

    private static final Logger LOGGER = Logger.getLogger(SqliteKarmaStorage.class.getName());

    private final DatabaseManager db;

    public SqliteKarmaStorage(DatabaseManager db) {
        this.db = db;
    }

    @Override
    public int getKarma(UUID uuid) {
        try (PreparedStatement ps = db.getConnection().prepareStatement(
                "SELECT karma FROM karma WHERE uuid = ?")) {
            ps.setString(1, uuid.toString());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt("karma");
            }
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Failed to get karma for " + uuid, e);
        }
        return 0;
    }

    @Override
    public long getLastUpdate(UUID uuid) {
        try (PreparedStatement ps = db.getConnection().prepareStatement(
                "SELECT last_update FROM karma WHERE uuid = ?")) {
            ps.setString(1, uuid.toString());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getLong("last_update");
            }
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Failed to get last update for " + uuid, e);
        }
        return System.currentTimeMillis();
    }

    @Override
    public void setKarma(UUID uuid, int karma, long timestamp) {
        try (PreparedStatement ps = db.getConnection().prepareStatement(
                "INSERT OR REPLACE INTO karma (uuid, karma, last_update) VALUES (?, ?, ?)")) {
            ps.setString(1, uuid.toString());
            ps.setInt(2, karma);
            ps.setLong(3, timestamp);
            ps.executeUpdate();
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Failed to set karma for " + uuid, e);
        }
    }

    @Override
    public void load() {
        // No-op: SQLite is always ready
    }

    @Override
    public void save() {
        // No-op: writes are immediate
    }
}
