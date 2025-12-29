package com.patelheet.paradiseteamchat;

import org.bukkit.plugin.java.JavaPlugin;

import com.patelheet.paradiseteamchat.config.ConfigManager;

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

        // Rest of the plugin initialisation code will go here

        getLogger().info("ParadiseTeamChat Plugin enabled successfully!");
    }

    @Override
    public void onDisable() {
        getLogger().info("========================================");
        getLogger().info("ParadiseTeamChat Plugin shutting down...");
        getLogger().info("========================================");

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
}
