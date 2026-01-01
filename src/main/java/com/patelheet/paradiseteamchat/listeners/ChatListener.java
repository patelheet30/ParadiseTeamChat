package com.patelheet.paradiseteamchat.listeners;

import com.patelheet.paradiseteamchat.ParadiseTeamChatPlugin;
import com.patelheet.paradiseteamchat.managers.ChatModeManager;
import com.patelheet.paradiseteamchat.managers.ChatModeManager.ChatMode;
import com.patelheet.paradiseteamchat.models.Team;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

/**
 * Listener to handle player chat events and route messages based on chat mode
 * (GLOBAL or TEAM).
 */
public class ChatListener implements Listener {
    private final ParadiseTeamChatPlugin plugin;
    private final ChatModeManager chatModeManager;
    private final PlainTextComponentSerializer plainSerializer;

    /**
     * Constructor for ChatListener.
     * 
     * @param plugin The main plugin instance.
     */
    public ChatListener(ParadiseTeamChatPlugin plugin) {
        this.plugin = plugin;
        this.chatModeManager = plugin.getChatModeManager();
        this.plainSerializer = PlainTextComponentSerializer.plainText();
    }

    /**
     * Formats a team chat message.
     * 
     * @param team    The team the player belongs to.
     * @param player  The player sending the message.
     * @param message The message content.
     * @return The formatted team chat message.
     */
    private String formatTeamMessage(Team team, Player player, String message) {
        String tag = team.getTag();
        String playerDisplayName = player.getName();

        // Format: [TC] PlayerName: message
        return "§8[§aTC§8]§r " + playerDisplayName + "§7: §f" + message;
    }

    /**
     * Handles team chat messages.
     * 
     * @param team    The team to send the message to.
     * @param message The message content.
     */
    private void sendToTeamMembers(Team team, String message) {
        for (String memberName : team.getMembers()) {
            Player member = Bukkit.getPlayerExact(memberName);

            if (member != null && member.isOnline()) {
                member.sendMessage(message);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerChat(AsyncChatEvent event) {
        if (event.isCancelled()) {
            return;
        }

        Player player = event.getPlayer();
        String playerName = player.getName().toLowerCase();
        ChatMode mode = chatModeManager.getChatMode(playerName);

        // If in GLOBAL mode, format the message with the Team Tag
        if (mode == ChatMode.GLOBAL) {
            Team team = plugin.getCacheManager().getPlayerTeam(playerName);

            event.renderer((source, sourceDisplayName, message, viewer) -> {
                Component formattedName;

                if (team != null) {
                    // Format: [TAG] PlayerName
                    String tagPrefix = "§8[§b" + team.getTag() + "§8]§r ";
                    formattedName = LegacyComponentSerializer.legacySection().deserialize(tagPrefix)
                            .append(sourceDisplayName);
                } else {
                    formattedName = sourceDisplayName;
                }

                return formattedName
                        .append(Component.text("§7: §f"))
                        .append(message);
            });
            return;
        }

        // Handle Team Chat (if mode is TEAM)
        handleTeamChat(event, player, playerName);
    }

    private void handleTeamChat(AsyncChatEvent event, Player player, String playerName) {
        // Cancel the original chat event to prevent global message
        event.setCancelled(true);

        // Get the player's team
        Team team = plugin.getCacheManager().getPlayerTeam(playerName);

        // If player is not in a team, force GLOBAL mode and notify
        if (team == null) {
            chatModeManager.forceGlobalMode(playerName);
            player.sendMessage("§cYou are not in a team! Chat mode switched to GLOBAL.");
            plugin.getLogger().warning("Player " + playerName + " in TEAM mode but not in a team. Forced to GLOBAL.");
            return;
        }

        // Serialize the message to plain text
        String message = plainSerializer.serialize(event.message());

        // Format and send the team message
        String formattedMessage = formatTeamMessage(team, player, message);

        sendToTeamMembers(team, formattedMessage);

        plugin.getLogger().info("[TEAM:" + team.getName() + "] " + player.getName() + ": " + message);
    }
}
