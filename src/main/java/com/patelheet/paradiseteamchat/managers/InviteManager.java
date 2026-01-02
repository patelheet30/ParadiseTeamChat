package com.patelheet.paradiseteamchat.managers;

import com.patelheet.paradiseteamchat.ParadiseTeamChatPlugin;
import com.patelheet.paradiseteamchat.config.ConfigManager;
import com.patelheet.paradiseteamchat.models.Team;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/**
 * Manages team invitations within the ParadiseTeamChat plugin.
 * Handles sending invites, validating invite conditions, and notifying players.
 */
public class InviteManager {
    private final ParadiseTeamChatPlugin plugin;
    private final ConfigManager configManager;
    private final CacheManager cacheManager;

    /**
     * Constructor for InviteManager.
     * 
     * @param plugin The main plugin instance.
     */
    public InviteManager(ParadiseTeamChatPlugin plugin) {
        this.plugin = plugin;
        this.configManager = plugin.getConfigManager();
        this.cacheManager = plugin.getCacheManager();
    }

    /**
     * Sends a team invite from sender to targetName for the specified team.
     * 
     * @param sender     The player sending the invite
     * @param targetName The name of the player to invite
     * @param team       The team to which the player is being invited
     * @return true if the invite was sent successfully, false otherwise
     */
    public boolean sendInvite(Player sender, String targetName, Team team) {
        String senderName = sender.getName();

        // Verify sender is the team owner
        if (!team.isOwner(senderName)) {
            sender.sendMessage(configManager.getMessage("not-owner"));
            return false;
        }

        // Verify team is not full
        if (team.isFull()) {
            sender.sendMessage(configManager.getMessage("team-full"));
            return false;
        }

        // Verify target player exists and is online
        Player target = Bukkit.getPlayerExact(targetName);
        if (target == null || !target.isOnline()) {
            sender.sendMessage(configManager.getMessage("player-not-found", "{player}", targetName));
            return false;
        }

        String lowerTargetName = target.getName().toLowerCase();

        // Verify target player is not already in a team
        Team targetTeam = cacheManager.getPlayerTeam(lowerTargetName);
        if (targetTeam != null) {
            sender.sendMessage(configManager.getMessage("already-in-team"));
            return false;
        }

        // Verify target player has not already been invited
        if (cacheManager.hasInvite(lowerTargetName)) {
            sender.sendMessage(configManager.getMessage("already-invited"));
            return false;
        }

        // All checks passed - send invite
        cacheManager.addInvite(lowerTargetName, team.getId());

        // Notify both players
        sender.sendMessage(configManager.getMessage("player-invited", "{player}", target.getName()));
        target.sendMessage(configManager.getMessage("invite-received", "{team}", team.getName()));

        plugin.logDebug(senderName + " invited " + target.getName() + " to team " + team.getName());

        return true;
    }

    /**
     * Accepts a team invite for the specified player.
     * 
     * @param player The player accepting the invite.
     * @return true if the invite was accepted successfully, false otherwise.
     */
    public boolean acceptInvite(Player player) {
        String playerName = player.getName().toLowerCase();

        if (!cacheManager.hasInvite(playerName)) {
            player.sendMessage(configManager.getMessage("no-pending-invite"));
            return false;
        }

        Integer teamId = cacheManager.getInvite(playerName);
        if (teamId == null) {
            player.sendMessage(configManager.getMessage("no-pending-invite"));
            return false;
        }

        Team team = cacheManager.getTeamById(teamId);
        if (team == null) {
            player.sendMessage(configManager.getMessage("team-not-found", "{team}", "Unknown"));
            cacheManager.clearInvite(playerName);
            return false;
        }

        if (team.isFull()) {
            player.sendMessage(configManager.getMessage("team-full"));
            cacheManager.clearInvite(playerName);
            return false;
        }

        plugin.getAsyncTaskManager().asyncThenSync(
                () -> {
                    boolean added = plugin.getTeamRepository().addMember(teamId, playerName);
                    return added ? team : null;
                },
                (resultTeam) -> {
                    if (resultTeam != null) {
                        resultTeam.addMember(playerName);

                        cacheManager.updateTeamCache(resultTeam);
                        cacheManager.loadPlayerTeam(playerName, resultTeam);

                        cacheManager.clearInvite(playerName);

                        if (resultTeam.isFull()) {
                            clearAllInvitesForTeam(resultTeam.getId());
                            plugin.logDebug(
                                    "Team " + resultTeam.getName() + " is now full - cleared all pending invites.");
                        }

                        player.sendMessage(configManager.getMessage("player-joined", "{player}", player.getName()));

                        notifyTeamMembers(resultTeam,
                                configManager.getMessage("player-joined", "{player}", player.getName()));

                        plugin.logDebug(player.getName() + " joined team " + resultTeam.getName());

                    } else {
                        player.sendMessage("§cFailed to join team. Please try again.");
                    }
                });
        return true;
    }

    /**
     * Check if a player has an active invite.
     * 
     * @param playerName The name of the player to check.
     * @return true if the player has an active invite, false otherwise.
     */
    public boolean hasActiveInvite(String playerName) {
        return cacheManager.hasInvite(playerName.toLowerCase());
    }

    /**
     * Clears the invite for the specified player.
     * 
     * @param playerName The name of the player whose invite should be cleared.
     */
    public void clearInvite(String playerName) {
        cacheManager.clearInvite(playerName.toLowerCase());
    }

    /**
     * Clears all invites associated with a specific team.
     * 
     * @param teamId The ID of the team whose invites should be cleared.
     */
    public void clearAllInvitesForTeam(int teamId) {
        cacheManager.clearInvitesForTeam(teamId);
    }

    /**
     * Notifies all members of a team with a specific message.
     * 
     * @param team    The team whose members will be notified.
     * @param message The message to send to the team members.
     */
    private void notifyTeamMembers(Team team, String message) {
        for (String memberName : team.getMembers()) {
            Player member = Bukkit.getPlayerExact(memberName);
            if (member != null && member.isOnline()) {
                member.sendMessage(message);
            }
        }
    }

    /**
     * Gets the team ID of a pending invite for a player.
     * 
     * @param playerName The name of the player.
     * @return The team ID of the pending invite, or null if none exists.
     */
    public Integer getPendingInviteTeamId(String playerName) {
        return cacheManager.getInvite(playerName.toLowerCase());
    }

}
