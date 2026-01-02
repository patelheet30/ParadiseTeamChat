package com.patelheet.paradiseteamchat.database;

import com.patelheet.paradiseteamchat.ParadiseTeamChatPlugin;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Manages the SQLite database connection and schema.
 * Handles initialisation, schema creation, and graceful shutdown.
 */
public class DatabaseManager {
    private final ParadiseTeamChatPlugin plugin;
    private Connection connection;
    private final String databasePath;

    /**
     * Constructor for DatabaseManager.
     * 
     * @param plugin The main plugin instance.
     */
    public DatabaseManager(ParadiseTeamChatPlugin plugin) {
        this.plugin = plugin;
        this.databasePath = plugin.getDataFolder().getAbsolutePath() + File.separator + "paradiseteamchat.db";
    }

    public void initialise() {
        try {
            if (!plugin.getDataFolder().exists()) {
                plugin.getDataFolder().mkdirs();
            }

            connection = DriverManager.getConnection("jdbc:sqlite:" + databasePath);

            try (Statement stmt = connection.createStatement()) {
                stmt.execute("PRAGMA foreign_keys = ON;");
            }

            plugin.getLogger().info("Connected to SQLite database at " + databasePath);

            // Create schema
            createSchema();

            plugin.getLogger().info("Database schema initialised successfully.");
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to initialise database: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Creates the necessary database schema if it does not already exist.
     * 
     * @throws SQLException if a database access error occurs
     */
    private void createSchema() throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(
                    "CREATE TABLE IF NOT EXISTS teams (" +
                            "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                            "name TEXT UNIQUE NOT NULL COLLATE NOCASE," +
                            "name_lower TEXT UNIQUE NOT NULL," + // For case-sensitive storage
                            "tag TEXT UNIQUE NOT NULL COLLATE NOCASE," +
                            "owner_name TEXT NOT NULL," +
                            "created_date INTEGER NOT NULL," +
                            "member_limit INTEGER NOT NULL" +
                            ");");

            stmt.execute(
                    "CREATE TABLE IF NOT EXISTS team_members (" +
                            "team_id INTEGER NOT NULL," +
                            "player_name TEXT NOT NULL," +
                            "joined_date INTEGER NOT NULL," +
                            "PRIMARY KEY (team_id, player_name)," +
                            "FOREIGN KEY (team_id) REFERENCES teams(id) ON DELETE CASCADE" +
                            ");");

            stmt.execute(
                    "CREATE INDEX IF NOT EXISTS idx_members_name " +
                            "ON team_members(player_name);");

            stmt.execute(
                    "CREATE INDEX IF NOT EXISTS idx_teams_owner " +
                            "ON teams(owner_name);");

            stmt.execute(
                    "CREATE INDEX IF NOT EXISTS idx_teams_name_lower " +
                            "ON teams(name_lower);");
        }
    }

    /**
     * Gets the active database connection.
     * 
     * @return The active Connection object.
     */
    public Connection getConnection() {
        return connection;
    }

    /**
     * Checks if the database connection is active and valid.
     * 
     * @return true if connected, false otherwise.
     */
    public boolean isConnected() {
        try {
            return connection != null && !connection.isClosed();
        } catch (SQLException e) {
            return false;
        }
    }

    /**
     * Closes the database connection gracefully.
     */
    public void close() {
        if (connection != null) {
            try {
                connection.close();
                plugin.getLogger().info("Database connection closed.");
            } catch (SQLException e) {
                plugin.getLogger().severe("Failed to close database connection: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

}
