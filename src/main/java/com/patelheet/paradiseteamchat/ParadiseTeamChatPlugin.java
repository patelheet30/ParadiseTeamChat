package com.patelheet.paradiseteamchat;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import com.patelheet.paradiseteamchat.commands.GlobalChatCommand;
import com.patelheet.paradiseteamchat.commands.TeamAdminCommand;
import com.patelheet.paradiseteamchat.commands.TeamChatCommand;
import com.patelheet.paradiseteamchat.commands.TeamCommand;
import com.patelheet.paradiseteamchat.config.ConfigManager;
import com.patelheet.paradiseteamchat.database.DatabaseManager;
import com.patelheet.paradiseteamchat.database.TeamRepository;
import com.patelheet.paradiseteamchat.integrations.TeamChatExpansion;
import com.patelheet.paradiseteamchat.listeners.ChatListener;
import com.patelheet.paradiseteamchat.listeners.PlayerSessionListener;
import com.patelheet.paradiseteamchat.managers.AsyncTaskManager;
import com.patelheet.paradiseteamchat.managers.CacheManager;
import com.patelheet.paradiseteamchat.managers.ChatModeManager;
import com.patelheet.paradiseteamchat.managers.InviteManager;
import com.patelheet.paradiseteamchat.utils.InputValidator;

/**
 * ParadiseTeamChat Plugin - Main Class
 * High-performance and customisable team communication and management system
 * for Paper servers
 * 
 * @author Heet Patel (patelheet30)
 * @version 1.5.0
 */
public class ParadiseTeamChatPlugin extends JavaPlugin {
    private static ParadiseTeamChatPlugin instance;
    private ConfigManager configManager;
    private DatabaseManager databaseManager;
    private TeamRepository teamRepository;
    private CacheManager cacheManager;
    private AsyncTaskManager asyncTaskManager;
    private InputValidator inputValidator;
    private InviteManager inviteManager;
    private ChatModeManager chatModeManager;
    private TeamChatExpansion placeholderExpansion;

    @Override
    public void onEnable() {
        instance = this;
        getLogger().info("========================================");
        getLogger().info("ParadiseTeamChat Plugin v" + getPluginMeta().getVersion() + " by patelheet30");
        getLogger().info(getPluginMeta().getDescription());
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

        inputValidator = new InputValidator(this);
        getLogger().info("ParadiseTeamChat plugin input validator initialised.");

        inviteManager = new InviteManager(this);
        getLogger().info("ParadiseTeamChat plugin invite manager initialised.");

        chatModeManager = new ChatModeManager(this);
        chatModeManager.initialise();
        getLogger().info("ParadiseTeamChat plugin chat mode manager initialised.");

        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            placeholderExpansion = new TeamChatExpansion(this);
            placeholderExpansion.register();
            getLogger().info("PlaceholderAPI integration enabled!");
        } else {
            getLogger().warning("PlaceholderAPI not found - placeholders will not work!");
        }

        registerCommands();
        getLogger().info("ParadiseTeamChat plugin commands registered.");

        registerListeners();
        getLogger().info("ParadiseTeamChat plugin listeners registered.");

        if (configManager.isDebugMode()) {
            getLogger().warning("Debug mode is ENABLED - verbose logging active!");
        }

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
            cacheManager.shutdown();
            cacheManager.clearAll();
            getLogger().info("ParadiseTeamChat plugin cache cleared.");
        }

        if (databaseManager != null) {
            databaseManager.close();
            getLogger().info("ParadiseTeamChat plugin database connection closed.");
        }

        if (chatModeManager != null) {
            chatModeManager.shutdown();
            getLogger().info("ParadiseTeamChat plugin chat mode manager shut down.");
        }

        if (placeholderExpansion != null) {
            placeholderExpansion.unregister();
        }

        // Any necessary cleanup code will go here

        getLogger().info("ParadiseTeamChat Plugin disabled successfully!");
    }

    /**
     * Register commands and their executors
     */
    private void registerCommands() {
        TeamCommand teamCommand = new TeamCommand(this);
        getCommand("team").setExecutor(teamCommand);
        getCommand("team").setTabCompleter(teamCommand);

        TeamChatCommand teamChatCommand = new TeamChatCommand(this);
        getCommand("teamchat").setExecutor(teamChatCommand);

        GlobalChatCommand globalChatCommand = new GlobalChatCommand(this);
        getCommand("globalchat").setExecutor(globalChatCommand);

        TeamAdminCommand teamAdminCommand = new TeamAdminCommand(this);
        getCommand("teamadmin").setExecutor(teamAdminCommand);
        getCommand("teamadmin").setTabCompleter(teamAdminCommand);
    }

    private void registerListeners() {
        getServer().getPluginManager().registerEvents(new ChatListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerSessionListener(this), this);
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

    public InputValidator getInputValidator() {
        return inputValidator;
    }

    public InviteManager getInviteManager() {
        return inviteManager;
    }

    public ChatModeManager getChatModeManager() {
        return chatModeManager;
    }

    public void logDebug(String message) {
        if (configManager.isDebugMode()) {
            getLogger().info("[DEBUG] " + message);
        }
    }
}
