package ru.nyansus.mc.domya_fate.storage;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DatabaseManager {

    private static final Logger LOGGER = Logger.getLogger(DatabaseManager.class.getName());

    private final File dbFile;
    private Connection connection;

    public DatabaseManager(File dataFolder) {
        this.dbFile = new File(dataFolder, "domya-fate.db");
    }

    public void initialize() throws SQLException {
        dbFile.getParentFile().mkdirs();
        connection = DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath());
        connection.setAutoCommit(true);
        createTables();
    }

    private void createTables() throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("CREATE TABLE IF NOT EXISTS karma ("
                    + "uuid TEXT PRIMARY KEY,"
                    + "karma INTEGER NOT NULL DEFAULT 0,"
                    + "last_update INTEGER NOT NULL DEFAULT 0)");

            stmt.execute("CREATE TABLE IF NOT EXISTS player_titles ("
                    + "uuid TEXT NOT NULL,"
                    + "title_id INTEGER NOT NULL,"
                    + "PRIMARY KEY (uuid, title_id))");

            stmt.execute("CREATE TABLE IF NOT EXISTS active_title ("
                    + "uuid TEXT PRIMARY KEY,"
                    + "title_id INTEGER NOT NULL)");

            stmt.execute("CREATE TABLE IF NOT EXISTS stats ("
                    + "uuid TEXT NOT NULL,"
                    + "stat_key TEXT NOT NULL,"
                    + "value INTEGER NOT NULL DEFAULT 0,"
                    + "PRIMARY KEY (uuid, stat_key))");
        }
    }

    public Connection getConnection() {
        return connection;
    }

    public boolean isReady() {
        return connection != null;
    }

    public void close() {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                LOGGER.log(Level.WARNING, "Failed to close database connection", e);
            }
        }
    }
}
