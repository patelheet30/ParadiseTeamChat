package com.patelheet.paradiseteamchat.managers;

import com.patelheet.paradiseteamchat.ParadiseTeamChatPlugin;
import com.patelheet.paradiseteamchat.database.TeamBlockRepository;
import com.patelheet.paradiseteamchat.models.Team;
import com.patelheet.paradiseteamchat.models.TeamBlock;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class TeamBlockManager {

    private final ParadiseTeamChatPlugin plugin;
    private final TeamBlockRepository teamBlockRepository;
    private final CacheManager cacheManager;

    private final Map<String, TeamBlock> blockCache;
    private final Map<String, Set<Location>> activeEffectBlocks;

    private static final Set<Material> TEAM_EFFECT_BLOCKS = Set.of(
            Material.BEACON,
            Material.CONDUIT);

    public TeamBlockManager(ParadiseTeamChatPlugin plugin) {
        this.plugin = plugin;
        this.teamBlockRepository = plugin.getTeamBlockRepository();
        this.cacheManager = plugin.getCacheManager();
        this.blockCache = new ConcurrentHashMap<>();
        this.activeEffectBlocks = new ConcurrentHashMap<>();
    }

    public void initialise() {
        blockCache.clear();
        activeEffectBlocks.clear();

        // Load all team blocks from the database asynchronously
        plugin.getAsyncTaskManager().supplyAsync(() -> {
            return teamBlockRepository.getAllTeamBlocks();
        }).thenAccept(blocks -> {
            plugin.getAsyncTaskManager().runSync(() -> {
                for (TeamBlock block : blocks) {
                    blockCache.put(block.getLocationKey(), block);

                    Location loc = block.getLocation();
                    if (loc != null) {
                        String world = block.getWorld();
                        activeEffectBlocks.computeIfAbsent(world, k -> ConcurrentHashMap.newKeySet()).add(loc);
                    }
                }
                plugin.getLogger().info("Loaded " + blocks.size() + " team blocks into cache.");
            });
        }).exceptionally(throwable -> {
            plugin.getLogger().severe("Failed to load team blocks: " + throwable.getMessage());
            throwable.printStackTrace();
            return null;
        });
    }

    /**
     * Check if a material type is supported for team effects.
     * 
     * @param material The material to check.
     * @return True if supported, false otherwise.
     */
    public static boolean isSupportedBlock(Material material) {
        return TEAM_EFFECT_BLOCKS.contains(material);
    }

    /**
     * Register a block as team-owned
     * 
     * @param location  Location of the block
     * @param blockType Material type of the block (e.g., BEACON, CONDUIT)
     * @param ownerName Name of the player who owns the block
     * @param teamId    ID of the team
     * @return CompletableFuture that resolves to true if registration was
     *         successful, false otherwise
     */
    public CompletableFuture<Boolean> registerTeamBlock(Location location, Material blockType, String ownerName,
            int teamId) {
        String locationKey = TeamBlock.locationKey(location);

        if (blockCache.containsKey(locationKey)) {
            plugin.logDebug("Block at " + locationKey + " is already registered.");
            return CompletableFuture.completedFuture(false);
        }

        TeamBlock teamBlock = new TeamBlock(location, blockType, ownerName, teamId, System.currentTimeMillis());

        // Save to database asynchronously
        return plugin.getAsyncTaskManager().supplyAsync(() -> {
            return teamBlockRepository.addTeamBlock(teamBlock);
        }).thenApply(success -> {
            // Update cache on the main thread
            plugin.getAsyncTaskManager().runSync(() -> {
                if (success) {
                    blockCache.put(locationKey, teamBlock);
                    String world = location.getWorld().getName();
                    activeEffectBlocks.computeIfAbsent(world, k -> ConcurrentHashMap.newKeySet()).add(location);

                    plugin.logDebug("Registered team block at " + locationKey + " for team ID " + teamId);
                }
            });
            return success;
        });
    }

    /**
     * Get the TeamBlock at a specific location
     * 
     * @param location Location of the block
     * @return The TeamBlock if found, null otherwise
     */
    public TeamBlock getTeamBlock(Location location) {
        String locationKey = TeamBlock.locationKey(location);
        return blockCache.get(locationKey);
    }

    public boolean canRecieveEffect(Location blockLocation, Player player) {
        TeamBlock teamBlock = getTeamBlock(blockLocation);

        if (teamBlock == null) {
            return false;
        }

        Team blockTeam = cacheManager.getTeamById(teamBlock.getTeamId());

        if (blockTeam == null) {
            // Team not found - block should work normally
            return true;
        }

        String playerName = player.getName().toLowerCase();
        boolean isMember = blockTeam.isMember(playerName);

        if (!isMember) {
            plugin.logDebug("Player " + playerName + " denied effect from team block at " + teamBlock.getLocationKey());
        }

        return isMember;
    }

    /**
     * Remove a team block at a specific location
     * 
     * @param location Location of the block to remove
     * @return CompletableFuture that resolves to true if removal was successful,
     *         false otherwise
     */
    public CompletableFuture<Boolean> removeTeamBlock(Location location) {
        String locationKey = TeamBlock.locationKey(location);

        // Check if exists in cache
        if (!blockCache.containsKey(locationKey)) {
            plugin.logDebug("No team block found at " + locationKey + " to remove");
            return CompletableFuture.completedFuture(false);
        }

        // Async database removal
        return plugin.getAsyncTaskManager().supplyAsync(() -> {
            return teamBlockRepository.removeTeamBlock(location);
        }).thenApply(success -> {
            // Update cache on main thread
            plugin.getAsyncTaskManager().runSync(() -> {
                if (success) {
                    blockCache.remove(locationKey);
                    String world = location.getWorld().getName();
                    Set<Location> locations = activeEffectBlocks.get(world);
                    if (locations != null) {
                        locations.remove(location);
                        if (locations.isEmpty()) {
                            activeEffectBlocks.remove(world);
                        }
                    }
                    plugin.logDebug("Removed team block at " + locationKey);
                }
            });
            return success;
        });
    }

    /**
     * Cleanup all team blocks for a specific team
     * Ran when a team is disbanded
     */
    public void cleanupTeamBlocks(int teamId) {
        // Async database removal
        plugin.getAsyncTaskManager().executeAsync(() -> {
            return teamBlockRepository.removeAllTeamBlocks(teamId);
        }).thenAccept(success -> {
            // Update cache on main thread
            plugin.getAsyncTaskManager().runSync(() -> {
                if (success) {
                    // Remove from cache
                    blockCache.entrySet().removeIf(entry -> {
                        if (entry.getValue().getTeamId() == teamId) {
                            TeamBlock block = entry.getValue();
                            Location loc = block.getLocation();
                            if (loc != null) {
                                Set<Location> worldBlocks = activeEffectBlocks.get(block.getWorld());
                                if (worldBlocks != null) {
                                    worldBlocks.remove(loc);
                                    if (worldBlocks.isEmpty()) {
                                        activeEffectBlocks.remove(block.getWorld());
                                    }
                                }
                            }
                            return true;
                        }
                        return false;
                    });
                }
            });
        }).exceptionally(throwable -> {
            plugin.getLogger().severe("Error cleaning up team blocks: " + throwable.getMessage());
            throwable.printStackTrace();
            return null;
        });
    }

    /**
     * Find effect blocks near a location
     * 
     * @param playerLocation Player's location
     * @param blockType      Type of block to find (BEACON or CONDUIT)
     * @param maxRange       Maximum range to search
     * @return The nearest matching block location, or null if none found
     */
    public Location findNearbyEffectBlock(Location playerLocation, Material blockType, int maxRange) {
        String world = playerLocation.getWorld().getName();
        Set<Location> worldBlocks = activeEffectBlocks.get(world);

        if (worldBlocks == null || worldBlocks.isEmpty()) {
            return null; // No effect blocks in this world
        }

        double maxRangeSquared = maxRange * maxRange;
        Location nearest = null;
        double nearestDistSquared = Double.MAX_VALUE;

        // Only check known effect block locations (10-50 checks instead of 125,000!)
        for (Location blockLoc : worldBlocks) {
            // Check block type matches
            if (blockLoc.getBlock().getType() != blockType) {
                continue;
            }

            // Check if in range
            double distSquared = playerLocation.distanceSquared(blockLoc);
            if (distSquared <= maxRangeSquared && distSquared < nearestDistSquared) {
                nearest = blockLoc;
                nearestDistSquared = distSquared;
            }
        }

        return nearest;
    }

    /**
     * Get the number of team blocks currently cached.
     * 
     * @return Number of cached team blocks.
     */
    public int getCachedBlockCount() {
        return blockCache.size();
    }

    /**
     * Clear all cached team blocks.
     * Used during plugin shutdown or reload.
     */
    public void clearCache() {
        blockCache.clear();
        activeEffectBlocks.clear();
        plugin.logDebug("Team block cache cleared.");
    }

    /**
     * Get all cached team blocks (for debugging/admin commands).
     * 
     * @return Map of location keys to TeamBlock objects.
     */
    public Map<String, TeamBlock> getAllCachedBlocks() {
        return new ConcurrentHashMap<>(blockCache);
    }

    /**
     * Reload team blocks from database.
     * Useful after manual database changes or for admin commands.
     */
    public void reload() {
        plugin.logDebug("Reloading team blocks from database...");
        initialise();
    }

}
