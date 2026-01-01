package com.patelheet.paradiseteamchat.commands;

import com.patelheet.paradiseteamchat.ParadiseTeamChatPlugin;
import com.patelheet.paradiseteamchat.managers.ChatModeManager;
import com.patelheet.paradiseteamchat.managers.ChatModeManager.ChatMode;
import com.patelheet.paradiseteamchat.models.Team;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Team chat command to send messages to the player's team
 * /tc - Switch to TEAM mode
 * /tc <message> - Send quick team message without changing mode
 */
public class TeamChatCommand implements CommandExecutor {
    private final ParadiseTeamChatPlugin plugin;
    private final ChatModeManager chatModeManager;

    /**
     * Constructor for TeamChat command
     * 
     * @param plugin The main plugin instance
     */
    public TeamChatCommand(ParadiseTeamChatPlugin plugin) {
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

        // Check permission
        if (!player.hasPermission("teamchat.chat.team")) {
            player.sendMessage(plugin.getConfigManager().getMessage("no-permission"));
            return true;
        }

        // Check if player is in a team
        Team team = plugin.getCacheManager().getPlayerTeam(playerName);
        if (team == null) {
            player.sendMessage(plugin.getConfigManager().getMessage("not-in-team"));
            return true;
        }

        // Toggle chat mode to TEAM (no args)
        if (args.length == 0) {
            toggleToTeamMode(player, playerName);
            return true;
        }

        // Send quick message to team WITHOUT changing mode (args present)
        String message = String.join(" ", args);
        sendQuickTeamMessage(player, playerName, team, message);
        return true;
    }

    /**
     * Toggle the player's chat mode to TEAM
     * 
     * @param player     The player whose chat mode is to be switched
     * @param playerName The player's name in lowercase
     */
    private void toggleToTeamMode(Player player, String playerName) {
        ChatMode currentMode = chatModeManager.getChatMode(playerName);

        if (currentMode == ChatMode.TEAM) {
            player.sendMessage("§eYou are already in TEAM chat mode.");
            return;
        }

        // Switch to TEAM mode
        chatModeManager.setChatMode(playerName, ChatMode.TEAM);
        player.sendMessage(plugin.getConfigManager().getMessage("chat-mode-team"));
    }

    /**
     * Send a quick message to the player's team without changing the player's chat
     * mode
     * 
     * @param player     The player sending the message
     * @param playerName The player's name in lowercase
     * @param team       The team of the player
     * @param message    The message to be sent
     */
    private void sendQuickTeamMessage(Player player, String playerName, Team team, String message) {
        String playerDisplayName = player.getName();
        String formattedMessage = "§8[§aTC§8]§r " + playerDisplayName + "§7: §f" + message;

        // Send to all online team members
        for (String memberName : team.getMembers()) {
            Player member = Bukkit.getPlayerExact(memberName);

            if (member != null && member.isOnline()) {
                member.sendMessage(formattedMessage);
            }
        }
    }
}
