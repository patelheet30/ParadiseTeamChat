package com.patelheet.paradiseteamchat.commands;

import com.patelheet.paradiseteamchat.ParadiseTeamChatPlugin;
import com.patelheet.paradiseteamchat.managers.ChatModeManager;
import com.patelheet.paradiseteamchat.managers.ChatModeManager.ChatMode;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Global chat command to send messages to the normal chat
 * /gc - Switch to GLOBAL mode
 * /gc <message> - Send quick global message without changing mode
 */
public class GlobalChatCommand implements CommandExecutor {
    private final ParadiseTeamChatPlugin plugin;
    private final ChatModeManager chatModeManager;

    /**
     * Constructor for GlobalChatCommand
     * 
     * @param plugin The main plugin instance
     */
    public GlobalChatCommand(ParadiseTeamChatPlugin plugin) {
        this.plugin = plugin;
        this.chatModeManager = plugin.getChatModeManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cThis command can only be used by players.");
            return true;
        }

        Player player = (Player) sender;
        String playerName = player.getName().toLowerCase();

        // Switch to GLOBAL mode (no args)
        if (args.length == 0) {
            switchToGlobalMode(player, playerName);
            return true;
        }

        // Send quick global message WITHOUT changing mode (args present)
        String message = String.join(" ", args);
        sendQuickGlobalMessage(player, message);
        return true;
    }

    /**
     * Switch the player's chat mode to GLOBAL
     * 
     * @param player     The player whose chat mode is to be switched
     * @param playerName The player's name in lowercase
     */
    private void switchToGlobalMode(Player player, String playerName) {
        ChatMode currentMode = chatModeManager.getChatMode(playerName);

        if (currentMode == ChatMode.GLOBAL) {
            player.sendMessage("§eYou are already in GLOBAL chat mode.");
            return;
        }

        // Switch to GLOBAL mode
        chatModeManager.setChatMode(playerName, ChatMode.GLOBAL);
        player.sendMessage(plugin.getConfigManager().getMessage("chat-mode-global"));
    }

    /**
     * Send a quick global message without changing the player's chat mode
     * 
     * @param player  The player sending the message
     * @param message The message to be sent
     */
    private void sendQuickGlobalMessage(Player player, String message) {
        player.chat(message);
    }

}
