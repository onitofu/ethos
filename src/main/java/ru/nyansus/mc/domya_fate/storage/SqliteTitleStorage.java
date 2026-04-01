package ru.nyansus.mc.domya_fate.storage;

import ru.nyansus.mc.domya_fate.title.TitleStorage;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SqliteTitleStorage implements TitleStorage {

    private static final Logger LOGGER = Logger.getLogger(SqliteTitleStorage.class.getName());

    private final DatabaseManager db;

    public SqliteTitleStorage(DatabaseManager db) {
        this.db = db;
    }

    @Override
    public int getActiveTitle(UUID uuid) {
        try (PreparedStatement ps = db.getConnection().prepareStatement(
                "SELECT title_id FROM active_title WHERE uuid = ?")) {
            ps.setString(1, uuid.toString());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt("title_id");
            }
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Failed to get active title for " + uuid, e);
        }
        return -1;
    }

    @Override
    public void setActiveTitle(UUID uuid, int titleId) {
        try {
            if (titleId < 0) {
                try (PreparedStatement ps = db.getConnection().prepareStatement(
                        "DELETE FROM active_title WHERE uuid = ?")) {
                    ps.setString(1, uuid.toString());
                    ps.executeUpdate();
                }
            } else {
                try (PreparedStatement ps = db.getConnection().prepareStatement(
                        "INSERT OR REPLACE INTO active_title (uuid, title_id) VALUES (?, ?)")) {
                    ps.setString(1, uuid.toString());
                    ps.setInt(2, titleId);
                    ps.executeUpdate();
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Failed to set active title for " + uuid, e);
        }
    }

    @Override
    public Set<Integer> getUnlockedTitles(UUID uuid) {
        Set<Integer> titles = new HashSet<>();
        try (PreparedStatement ps = db.getConnection().prepareStatement(
                "SELECT title_id FROM player_titles WHERE uuid = ?")) {
            ps.setString(1, uuid.toString());
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                titles.add(rs.getInt("title_id"));
            }
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Failed to get unlocked titles for " + uuid, e);
        }
        return titles;
    }

    @Override
    public void unlockTitle(UUID uuid, int titleId) {
        try (PreparedStatement ps = db.getConnection().prepareStatement(
                "INSERT OR IGNORE INTO player_titles (uuid, title_id) VALUES (?, ?)")) {
            ps.setString(1, uuid.toString());
            ps.setInt(2, titleId);
            ps.executeUpdate();
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Failed to unlock title for " + uuid, e);
        }
    }

    @Override
    public void revokeTitle(UUID uuid, int titleId) {
        try (PreparedStatement ps = db.getConnection().prepareStatement(
                "DELETE FROM player_titles WHERE uuid = ? AND title_id = ?")) {
            ps.setString(1, uuid.toString());
            ps.setInt(2, titleId);
            ps.executeUpdate();
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Failed to revoke title for " + uuid, e);
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
