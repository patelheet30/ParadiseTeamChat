package com.patelheet.teamchat;

import org.bukkit.plugin.java.JavaPlugin;

/**
 * TeamChat Plugin - Main Class
 * High-performance team communication system for Paper servers
 * 
 * @author Heet Patel (patelheet30)
 * @version 1.0.0
 */
public class TeamChatPlugin extends JavaPlugin {
    private static TeamChatPlugin instance;

    @Override
    public void onEnable() {
        instance = this;
        getLogger().info("========================================");
        getLogger().info("TeamChat Plugin v" + getPluginMeta().getDescription() + " by patelheet30");
        getLogger().info("Loading...");
        getLogger().info("========================================");

        // Rest of the plugin initialisation code will go here

        getLogger().info("TeamChat Plugin enabled successfully!");
    }

    @Override
    public void onDisable() {
        getLogger().info("========================================");
        getLogger().info("TeamChat Plugin shutting down...");
        getLogger().info("========================================");

        // Any necessary cleanup code will go here

        getLogger().info("TeamChat Plugin disabled successfully!");
    }

    /**
     * Get the plugin instance
     * 
     * @return The plugin instance
     */
    public static TeamChatPlugin getInstance() {
        return instance;
    }
}
