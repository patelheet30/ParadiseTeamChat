package com.patelheet.paradiseteamchat.listeners;

import com.patelheet.paradiseteamchat.ParadiseTeamChatPlugin;
import com.patelheet.paradiseteamchat.managers.TeamBlockManager;
import com.patelheet.paradiseteamchat.models.TeamBlock;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPotionEffectEvent;

/**
 * Listens for special block effects applied to players from team blocks
 * (beacons, conduits etc.) and
 * applies them only if the player is a member of the team that owns the block.
 */
public class BlockEffectListener implements Listener {
    private final ParadiseTeamChatPlugin plugin;
    private final TeamBlockManager teamBlockManager;

    private static final int BEACON_RANGE = 50;
    private static final int CONDUIT_RANGE = 42;

    /**
     * Constructor for BlockEffectListener.
     * 
     * @param plugin The main plugin instance.
     */
    public BlockEffectListener(ParadiseTeamChatPlugin plugin) {
        this.plugin = plugin;
        this.teamBlockManager = plugin.getTeamBlockManager();
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityPotionEffect(EntityPotionEffectEvent event) {
        // Check if team blocks are enabled
        if (!plugin.getConfigManager().isTeamBlocksEnabled()) {
            return;
        }

        // Only care about players
        if (!(event.getEntity() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getEntity();

        // Only care about added effects
        if (event.getAction() != EntityPotionEffectEvent.Action.ADDED) {
            return;
        }

        // Only care about beacon and conduit effects
        EntityPotionEffectEvent.Cause cause = event.getCause();
        if (cause != EntityPotionEffectEvent.Cause.BEACON &&
                cause != EntityPotionEffectEvent.Cause.CONDUIT) {
            return;
        }

        // Find the source block of the effect
        Location effectSource = findEffectSource(player, cause);

        if (effectSource == null) {
            plugin.logDebug("Couldn't find " + cause + " source for " + player.getName() +
                    " - allowing effect");
            return;
        }

        // Check if the block is a team block
        TeamBlock teamBlock = teamBlockManager.getTeamBlock(effectSource);
        if (teamBlock == null) {
            // Not a team block, allow effect
            return;
        }

        if (!teamBlockManager.canRecieveEffect(effectSource, player)) {
            // Player not in team - block the effect
            event.setCancelled(true);

            plugin.logDebug("Blocked " + cause + " effect for " + player.getName() +
                    " at " + effectSource + " (not in team " + teamBlock.getTeamId() + ")");
        }
    }

    /**
     * Find the block that is the source of the effect on the player.
     * Uses cached locations instead of searching the entire cube.
     * 
     * @param player The player receiving the effect.
     * @param cause  The cause of the potion effect.
     * @return The location of the effect source block, or null if none found.
     */
    private Location findEffectSource(Player player, EntityPotionEffectEvent.Cause cause) {
        Material targetType = (cause == EntityPotionEffectEvent.Cause.BEACON)
                ? Material.BEACON
                : Material.CONDUIT;

        int range = (targetType == Material.BEACON) ? BEACON_RANGE : CONDUIT_RANGE;

        Location playerLoc = player.getLocation();

        Location source = teamBlockManager.findNearbyEffectBlock(playerLoc, targetType, range);

        if (source != null) {
            plugin.logDebug("Found " + targetType + " source at " + source + " for player " + player.getName());
        }

        return source;
    }
}
