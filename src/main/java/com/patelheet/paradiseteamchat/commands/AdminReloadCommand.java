package com.patelheet.paradiseteamchat.commands;

import com.patelheet.paradiseteamchat.ParadiseTeamChatPlugin;
import org.bukkit.command.CommandSender;

/**
 * Command to reload the plugin configuration.
 * Admin-only command to refresh config.yml without restarting server.
 */
public class AdminReloadCommand extends BaseCommand {

    /**
     * Constructor for ReloadCommand.
     * 
     * @param plugin The main plugin instance.
     */
    public AdminReloadCommand(ParadiseTeamChatPlugin plugin) {
        super(plugin);
    }

    @Override
    public String getPermission() {
        return "teamchat.admin";
    }

    @Override
    public String getUsage() {
        return "/teamadmin reload";
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        // Check permission (double-check, though router already verified)
        if (!checkPermission(sender)) {
            return true;
        }

        sender.sendMessage("§eReloading ParadiseTeamChat configuration...");

        try {
            // Reload configuration
            plugin.getConfigManager().reloadConfig();
            plugin.getRoleManager().reloadRoles();

            sender.sendMessage(configManager.getMessage("config-reloaded"));

            // Log reload action
            plugin.getLogger().info("Configuration reloaded by " + sender.getName());
            plugin.logDebug("Config reloaded - new values loaded into memory");
            plugin.logDebug("Roles reloaded - " + plugin.getRoleManager().getRoleCount() + " roles loaded");

        } catch (Exception e) {
            sender.sendMessage("§cFailed to reload configuration! Check console for errors.");
            plugin.getLogger().severe("Error reloading config: " + e.getMessage());
            e.printStackTrace();
        }

        return true;
    }
}