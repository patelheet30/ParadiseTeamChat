package com.patelheet.paradiseteamchat;

import org.bukkit.plugin.java.JavaPlugin;

import com.patelheet.paradiseteamchat.config.ConfigManager;
import com.patelheet.paradiseteamchat.database.DatabaseManager;
import com.patelheet.paradiseteamchat.database.TeamRepository;
import com.patelheet.paradiseteamchat.managers.AsyncTaskManager;
import com.patelheet.paradiseteamchat.managers.CacheManager;

/**
 * ParadiseTeamChat Plugin - Main Class
 * High-performance and customisable team communication and management system
 * for Paper servers
 * 
 * @author Heet Patel (patelheet30)
 * @version 1.0.0
 */
public class ParadiseTeamChatPlugin extends JavaPlugin {
    private static ParadiseTeamChatPlugin instance;
    private ConfigManager configManager;
    private DatabaseManager databaseManager;
    private TeamRepository teamRepository;
    private CacheManager cacheManager;
    private AsyncTaskManager asyncTaskManager;

    @Override
    public void onEnable() {
        instance = this;
        getLogger().info("========================================");
        getLogger().info("ParadiseTeamChat Plugin v" + getPluginMeta().getDescription() + " by patelheet30");
        getLogger().info("Loading...");
        getLogger().info("========================================");

        configManager = new ConfigManager(this);
        configManager.loadConfig();
        getLogger().info("ParadiseTeamChat plugin configuration added.");

        databaseManager = new DatabaseManager(this);
        databaseManager.initialise();
        getLogger().info("ParadiseTeamChat plugin database initialised.");

        teamRepository = new TeamRepository(this, databaseManager);
        getLogger().info("ParadiseTeamChat plugin team repository initialised.");

        cacheManager = new CacheManager(this);
        getLogger().info("ParadiseTeamChat plugin cache manager initialised.");

        asyncTaskManager = new AsyncTaskManager(this);
        getLogger().info("ParadiseTeamChat plugin async task manager initialised.");

        // Rest of the plugin initialisation code will go here

        getLogger().info("ParadiseTeamChat Plugin enabled successfully!");
    }

    @Override
    public void onDisable() {
        getLogger().info("========================================");
        getLogger().info("ParadiseTeamChat Plugin shutting down...");
        getLogger().info("========================================");

        if (asyncTaskManager != null) {
            asyncTaskManager.cancelAllTasks();
            getLogger().info("ParadiseTeamChat plugin async task manager shut down.");
        }

        if (cacheManager != null) {
            cacheManager.clearAll();
            getLogger().info("ParadiseTeamChat plugin cache cleared.");
        }

        if (databaseManager != null) {
            databaseManager.close();
            getLogger().info("ParadiseTeamChat plugin database connection closed.");
        }
        // Any necessary cleanup code will go here

        getLogger().info("ParadiseTeamChat Plugin disabled successfully!");
    }

    /**
     * Get the plugin instance
     * 
     * @return The plugin instance
     */
    public static ParadiseTeamChatPlugin getInstance() {
        return instance;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public TeamRepository getTeamRepository() {
        return teamRepository;
    }

    public CacheManager getCacheManager() {
        return cacheManager;
    }

    public AsyncTaskManager getAsyncTaskManager() {
        return asyncTaskManager;
    }
}
