package com.patelheet.paradiseteamchat.listeners;

import com.patelheet.paradiseteamchat.ParadiseTeamChatPlugin;
import com.patelheet.paradiseteamchat.managers.ChatModeManager.ChatMode;
import com.patelheet.paradiseteamchat.models.Team;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Handles player session events (join/quit) for the ParadiseTeamChat plugin.
 * Loads and unloads player team data and manages chat modes.
 */
public class PlayerSessionListener implements Listener {
    private final ParadiseTeamChatPlugin plugin;

    /**
     * Constructor for PlayerSessionListener.
     * 
     * @param plugin The main plugin instance.
     */
    public PlayerSessionListener(ParadiseTeamChatPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Handles player join events.
     * Loads player's team data asynchronously to prevent login lag.
     * 
     * @param event The PlayerJoinEvent
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        String playerName = player.getName().toLowerCase();

        // Set default chat mode to GLOBAL
        plugin.getChatModeManager().setChatMode(playerName, ChatMode.GLOBAL);

        // Load player's team from database asynchronously
        plugin.getAsyncTaskManager().supplyAsync(() -> {
            return plugin.getTeamRepository().getTeamByPlayer(playerName);
        }).thenAccept(team -> {
            // Switch back to main thread to update cache
            plugin.getAsyncTaskManager().runSync(() -> {
                if (team != null) {
                    // Player is in a team - load into cache
                    plugin.getCacheManager().loadPlayerTeam(playerName, team);
                    plugin.getLogger().info("Loaded team '" + team.getName() + "' for player " + player.getName());
                } else {
                    // Player is not in a team - ensure cache is clear
                    plugin.getCacheManager().loadPlayerTeam(playerName, null);
                    plugin.getLogger().info("Player " + player.getName() + " is not in a team.");
                }

                // Check for pending invites
                if (plugin.getInviteManager().hasActiveInvite(playerName)) {
                    Integer teamId = plugin.getInviteManager().getPendingInviteTeamId(playerName);
                    if (teamId != null) {
                        Team inviteTeam = plugin.getCacheManager().getTeamById(teamId);
                        if (inviteTeam != null) {
                            player.sendMessage(plugin.getConfigManager().getMessage("invite-received", "{team}",
                                    inviteTeam.getName()));
                        } else {
                            // Team no longer exists - clear stale invite
                            plugin.getInviteManager().clearInvite(playerName);
                        }
                    }
                }
            });
        }).exceptionally(throwable -> {
            plugin.getLogger()
                    .severe("Error loading team data for " + player.getName() + ": " + throwable.getMessage());
            throwable.printStackTrace();
            return null;
        });
    }

    /**
     * Handles player quit events.
     * Performs complete cleanup to prevent memory leaks.
     * 
     * @param event The PlayerQuitEvent
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        String playerName = player.getName().toLowerCase();

        // Unload player's team from cache
        plugin.getCacheManager().unloadPlayerTeam(playerName);

        // Clear any pending invites sent TO this player
        plugin.getInviteManager().clearInvite(playerName);

        // Remove from chat mode map
        plugin.getChatModeManager().removePlayer(playerName);

        plugin.getLogger().info("Cleaned up session data for player " + player.getName());
    }
}