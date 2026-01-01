package com.patelheet.paradiseteamchat.managers;

import com.patelheet.paradiseteamchat.ParadiseTeamChatPlugin;
import com.patelheet.paradiseteamchat.models.Team;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Iterator;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

/**
 * Manages caching of teams and invites to provide quick access and reduce
 * database load.
 */
public class CacheManager {
    private final ParadiseTeamChatPlugin plugin;

    // Maps Player Name Strings and Team IDs to Team objects for quick access
    private final Map<String, Team> playerTeamCache;
    private final Map<Integer, Team> teamIdCache;

    // Maps Player Name Strings to Team Invite IDs for managing pending invites
    private final Map<String, Integer> pendingInvites;

    private BukkitTask cleanupTask;

    /**
     * Constructor for CacheManager.
     * 
     * @param plugin The main plugin instance.
     */
    public CacheManager(ParadiseTeamChatPlugin plugin) {
        this.plugin = plugin;

        // Initalise caches because ConcurrentHashMap is thread-safe
        this.playerTeamCache = new ConcurrentHashMap<>();
        this.teamIdCache = new ConcurrentHashMap<>();
        this.pendingInvites = new ConcurrentHashMap<>();

    }

    /**
     * Initialises the CacheManager, starting periodic cleanup tasks.
     */
    public void initialise() {
        int cleanupDelay = plugin.getConfigManager().getCacheCleanupDelay();
        long delayTicks = cleanupDelay * 20L; // Convert seconds to ticks

        cleanupTask = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            performCleanup();
        }, delayTicks, delayTicks);

        plugin.getLogger()
                .info("CacheManager initialised with cleanup task running every " + cleanupDelay + " seconds.");
    }

    /**
     * Performs periodic cleanup of the caches to remove stale data.
     */
    private void performCleanup() {
        int teamsRemoved = 0;
        int invitesRemoved = 0;

        // Clean up teams with no online members
        Iterator<Map.Entry<Integer, Team>> teamIterator = teamIdCache.entrySet().iterator();
        while (teamIterator.hasNext()) {
            Map.Entry<Integer, Team> entry = teamIterator.next();
            Team team = entry.getValue();

            // Check if any team member is online
            boolean hasOnlineMembers = false;
            for (String memberName : team.getMembers()) {
                Player member = Bukkit.getPlayerExact(memberName);
                if (member != null && member.isOnline()) {
                    hasOnlineMembers = true;
                    break;
                }
            }

            // Remove team if no members are online
            if (!hasOnlineMembers) {
                teamIterator.remove();
                // Also remove from player cache
                for (String memberName : team.getMembers()) {
                    playerTeamCache.remove(memberName);
                }
                teamsRemoved++;
            }
        }

        // Clean up invites for offline players
        Iterator<Map.Entry<String, Integer>> inviteIterator = pendingInvites.entrySet().iterator();
        while (inviteIterator.hasNext()) {
            Map.Entry<String, Integer> entry = inviteIterator.next();
            String playerName = entry.getKey();

            Player player = Bukkit.getPlayerExact(playerName);
            if (player == null || !player.isOnline()) {
                inviteIterator.remove();
                invitesRemoved++;
            }
        }

        // Log cleanup results if anything was removed
        if (teamsRemoved > 0 || invitesRemoved > 0) {
            plugin.getLogger()
                    .info("Cache cleanup: Removed " + teamsRemoved + " teams and " + invitesRemoved + " invites.");
        }
    }

    /**
     * Shuts down the CacheManager, stopping any ongoing tasks.
     */
    public void shutdown() {
        if (cleanupTask != null) {
            cleanupTask.cancel();
            plugin.getLogger().info("Cache cleanup task stopped.");
        }
    }

    /**
     * Loads a player's team into the cache when they join.
     * 
     * @param playerName The name of the player.
     * @param team       The team object to cache for the player.
     */
    public void loadPlayerTeam(String playerName, Team team) {
        String lowerName = playerName.toLowerCase();

        if (team != null) {
            playerTeamCache.put(lowerName, team);
            teamIdCache.put(team.getId(), team);

            plugin.getLogger().info("Loaded team '" + team.getName() + "' into cache for player " + playerName + ".");
        } else {
            playerTeamCache.remove(lowerName);
            plugin.getLogger().info("Removed team cache for player " + playerName + ".");
        }
    }

    /**
     * Unloads a player's team from the cache when they leave, ensuring that
     * no other online members are part of the same team before removing it.
     * 
     * @param playerName The name of the player.
     */
    public void unloadPlayerTeam(String playerName) {
        String lowerName = playerName.toLowerCase();

        Team team = playerTeamCache.remove(lowerName);

        if (team != null) {
            // Check if any other members of this team are still online
            boolean hasOtherOnlineMembers = false;
            for (String member : team.getMembers()) {
                if (!member.equals(lowerName) && playerTeamCache.containsKey(member)) {
                    hasOtherOnlineMembers = true;
                    break;
                }
            }

            if (!hasOtherOnlineMembers) {
                teamIdCache.remove(team.getId());
                plugin.getLogger().info("Team '" + team.getName() + "' unloaded from cache (no online members).");
            }

            plugin.getLogger().info("Unloaded player '" + playerName + "' from cache.");
        }
    }

    /**
     * Gets the cached team for a given player (instant lookup, no DB call).
     * 
     * @param playerName The name of the player.
     * @return The Team object associated with the player, or null if not found.
     */
    public Team getPlayerTeam(String playerName) {
        return playerTeamCache.get(playerName.toLowerCase());
    }

    /**
     * Gets the cached team by its ID.
     * 
     * @param teamId The ID of the team.
     * @return The Team object associated with the ID, or null if not found.
     */
    public Team getTeamById(int teamId) {
        return teamIdCache.get(teamId);
    }

    /**
     * Updates the cache for a given team when its data changes.
     * 
     * @param team The updated Team object.
     */
    public void updateTeamCache(Team team) {
        teamIdCache.put(team.getId(), team);

        for (String member : team.getMembers()) {
            playerTeamCache.put(member, team);
        }

        plugin.getLogger().info("Updated cache for team '" + team.getName() + "'.");
    }

    /**
     * Removes a team from the cache entirely. Called when a team is deleted.
     * 
     * @param teamId The ID of the team to invalidate.
     */
    public void invalidateTeam(int teamId) {
        Team team = teamIdCache.remove(teamId);

        if (team != null) {
            for (String member : team.getMembers()) {
                playerTeamCache.remove(member);
            }

            plugin.getLogger().info("Invalidated cache for team '" + team.getName() + "'.");
        }
    }

    /**
     * Adds a pending invite to the cache.
     * 
     * @param playerName The name of the player being invited
     * @param teamId     The ID of the team extending the invite
     */
    public void addInvite(String playerName, int teamId) {
        String lowerName = playerName.toLowerCase();
        pendingInvites.put(lowerName, teamId);
        plugin.getLogger().info("Invite cached: " + playerName + " -> Team ID " + teamId);
    }

    /**
     * Checks if a player has a pending invite in the cache.
     * 
     * @param playerName The name of the player to check
     * @return
     */
    public boolean hasInvite(String playerName) {
        return pendingInvites.containsKey(playerName.toLowerCase());
    }

    /**
     * Gets the team ID of a pending invite for a player.
     * 
     * @param playerName The name of the player to check
     * @return The team ID of the pending invite, or null if none exists
     */
    public Integer getInvite(String playerName) {
        return pendingInvites.get(playerName.toLowerCase());
    }

    /**
     * Clears a pending invite for a player. Called when a player accepts/declines
     * an invite or leaves the server.
     * 
     * @param playerName The name of the player whose invite should be cleared
     */
    public void clearInvite(String playerName) {
        String lowerName = playerName.toLowerCase();
        Integer teamId = pendingInvites.remove(lowerName);

        if (teamId != null) {
            plugin.getLogger()
                    .info("Cleared invite for player '" + playerName + "' (was invited to team ID " + teamId + ").");
        }
    }

    /**
     * Clears all pending invites for a specific team. Called when a team is
     * deleted or becomes full.
     * 
     * @param teamId The ID of the team whose invites should be cleared
     */
    public void clearInvitesForTeam(int teamId) {
        pendingInvites.entrySet().removeIf(entry -> entry.getValue().equals(teamId));
        plugin.getLogger().info("Cleared all pending invites for team ID " + teamId);
    }

    /**
     * Get the number of teams currently cached.
     * 
     * @return The number of cached teams.
     */
    public int getCachedTeamCount() {
        return teamIdCache.size();
    }

    /**
     * Get the number of players with cached team data.
     * 
     * @return The number of cached players.
     */
    public int getCachedPlayerCount() {
        return playerTeamCache.size();
    }

    /**
     * Get the number of pending invites.
     * 
     * @return The number of pending invites.
     */
    public int getPendingInviteCount() {
        return pendingInvites.size();
    }

    /**
     * Clear all caches (for reload or shutdown).
     * 
     * @implNote This method should be used cautiously as it removes all cached
     *           data.
     */
    public void clearAll() {
        playerTeamCache.clear();
        teamIdCache.clear();
        pendingInvites.clear();
        plugin.getLogger().info("All caches cleared.");
    }
}
