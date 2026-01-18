package com.patelheet.paradiseteamchat.listeners;

import com.patelheet.paradiseteamchat.ParadiseTeamChatPlugin;
import com.patelheet.paradiseteamchat.managers.TeamBlockManager;
import com.patelheet.paradiseteamchat.models.Team;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;

/**
 * Listener to handle team block placement and breaking.
 * Registers blocks placed by team members as team blocks,
 * which provide benefits only to team members.
 */
public class TeamBlockListener implements Listener {

    private final ParadiseTeamChatPlugin plugin;
    private final TeamBlockManager teamBlockManager;

    public TeamBlockListener(ParadiseTeamChatPlugin plugin) {
        this.plugin = plugin;
        this.teamBlockManager = plugin.getTeamBlockManager();
    }

    /**
     * Formats the block name for display.
     * 
     * @param event The block place event.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        // Check if team blocks are enabled
        if (!plugin.getConfigManager().isTeamBlocksEnabled()) {
            return;
        }

        Player player = event.getPlayer();
        Block block = event.getBlockPlaced();
        Material type = block.getType();

        // Only care about effect blocks
        if (!teamBlockManager.isSupportedBlock(type)) {
            return;
        }

        String playerName = player.getName().toLowerCase();
        Team team = plugin.getCacheManager().getPlayerTeam(playerName);

        if (team == null) {
            // Player has no team, do not register as team block
            plugin.logDebug(playerName + " placed " + type + " without team - public block");
            return;
        }

        // Register as team block
        teamBlockManager.registerTeamBlock(block.getLocation(), type, playerName, team.getId()).thenAccept(success -> {
            if (success) {
                // Notify player on main thread
                plugin.getAsyncTaskManager().runSync(() -> {
                    player.sendMessage("§aThis " + formatBlockName(type) + " will only benefit your team members!");
                });

                plugin.logDebug("Registered team " + type + " at " +
                        block.getLocation() + " for team " + team.getName());
            }
        }).exceptionally(throwable -> {
            plugin.getLogger().severe("Error registering team block: " + throwable.getMessage());
            throwable.printStackTrace();
            return null;
        });
    }

    /**
     * Handles block break events.
     * 
     * @param event The block break event.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();

        // Only care about effect blocks
        if (!teamBlockManager.isSupportedBlock(block.getType())) {
            return;
        }

        // Check if this is a team block (not a team block, nothing to do)
        if (teamBlockManager.getTeamBlock(block.getLocation()) == null) {
            return;
        }

        // Remove from tracking
        teamBlockManager.removeTeamBlock(block.getLocation()).thenAccept(success -> {
            if (success) {
                plugin.logDebug("Removed team block at " + block.getLocation());
            }
        }).exceptionally(throwable -> {
            plugin.getLogger().severe("Error removing team block: " + throwable.getMessage());
            throwable.printStackTrace();
            return null;
        });
    }

    /**
     * Formats a block name for display.
     * 
     * @param material The block material.
     * @return The formatted block name.
     */
    private String formatBlockName(Material material) {
        return material.name().toLowerCase().replace('_', ' ');
    }

}
