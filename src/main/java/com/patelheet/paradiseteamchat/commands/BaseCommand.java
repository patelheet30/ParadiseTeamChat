package com.patelheet.paradiseteamchat.commands;

import com.patelheet.paradiseteamchat.ParadiseTeamChatPlugin;
import com.patelheet.paradiseteamchat.config.ConfigManager;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Abstract base class for all commands in the ParadiseTeamChat plugin.
 * Provides common functionality such as permission checking and usage
 * messaging.
 */
public abstract class BaseCommand {
    protected final ParadiseTeamChatPlugin plugin;
    protected final ConfigManager configManager;

    /**
     * Constructor for BaseCommand.
     * 
     * @param plugin The main plugin instance.
     */
    public BaseCommand(ParadiseTeamChatPlugin plugin) {
        this.plugin = plugin;
        this.configManager = plugin.getConfigManager();
    }

    /**
     * Executes the command with the given sender and arguments.
     * 
     * @param sender The command sender
     * @param args   The command arguments
     * @return true if the command was executed successfully, false otherwise
     */
    public abstract boolean execute(CommandSender sender, String[] args);

    /**
     * Gets the permission node required to execute this command.
     * 
     * @return The permission node as a String, or null if no permission is required
     */
    public abstract String getPermission();

    /**
     * Gets the usage string for this command.
     * 
     * @return The usage string
     */
    public abstract String getUsage();

    /**
     * Checks if the sender has the required permission to execute the command.
     * 
     * @param sender The command sender
     * @return true if the sender has permission, false otherwise
     */
    protected boolean checkPermission(CommandSender sender) {
        String permission = getPermission();

        if (permission == null) {
            return true;
        }

        if (!sender.hasPermission(permission)) {
            sender.sendMessage(configManager.getMessage("no-permission"));
            return false;
        }

        return true;
    }

    /**
     * Validates that the sender is a player.
     * 
     * @param sender The command sender
     * @return The Player object if the sender is a player, null otherwise
     */
    protected Player validatePlayer(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cThis command can only be used by players.");
            return null;
        }
        return (Player) sender;
    }

    /**
     * Sends the usage message to the sender.
     * 
     * @param sender The command sender
     */
    protected void sendUsage(CommandSender sender) {
        sender.sendMessage("§cUsage: " + getUsage());
    }

    /**
     * Sends a formatted message to the sender with the plugin prefix.
     * 
     * @param sender  The command sender
     * @param message The message to send
     */
    protected void sendMessage(CommandSender sender, String message) {
        sender.sendMessage(configManager.getMessage("prefix") + message);
    }

}
