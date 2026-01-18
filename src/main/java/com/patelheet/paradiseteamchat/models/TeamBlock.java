package com.patelheet.paradiseteamchat.models;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;

/**
 * Represents a block placed by a team member in the game world that gives
 * team-exclusive benefits.
 */
public class TeamBlock {

    private final String world;
    private final int x;
    private final int y;
    private final int z;
    private final Material blockType;
    private final String ownerName;
    private final int teamId;
    private final long placedDate;

    /**
     * Constructor for TeamBlock.
     * 
     * @param world      World Name
     * @param x          X Coordinate
     * @param y          Y Coordinate
     * @param z          Z Coordinate
     * @param blockType  Type of Block
     * @param ownerName  Owner's Name (lower case)
     * @param teamId     Team ID
     * @param placedDate Timestamp when the block was placed
     */
    public TeamBlock(
            String world,
            int x,
            int y,
            int z,
            Material blockType,
            String ownerName,
            int teamId,
            long placedDate) {
        this.world = world;
        this.x = x;
        this.y = y;
        this.z = z;
        this.blockType = blockType;
        this.ownerName = ownerName.toLowerCase();
        this.teamId = teamId;
        this.placedDate = placedDate;
    }

    /**
     * Convenience constructor for TeamBlock using Location.
     * 
     * @param location   Location of the block
     * @param blockType  Type of Block
     * @param ownerName  Owner's Name
     * @param teamId     Team ID
     * @param placedDate Timestamp when the block was placed
     */
    public TeamBlock(Location location, Material blockType, String ownerName, int teamId, long placedDate) {
        this(
                location.getWorld().getName(),
                location.getBlockX(),
                location.getBlockY(),
                location.getBlockZ(),
                blockType,
                ownerName,
                teamId,
                placedDate);
    }

    public String getWorld() {
        return world;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getZ() {
        return z;
    }

    public Material getBlockType() {
        return blockType;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public int getTeamId() {
        return teamId;
    }

    public long getPlacedDate() {
        return placedDate;
    }

    /**
     * Gets the Location object for this TeamBlock.
     * 
     * @return Location of the block, or null if the world is not loaded.
     */
    public Location getLocation() {
        org.bukkit.World bukkitWorld = Bukkit.getWorld(world);
        if (bukkitWorld == null) {
            return null;
        }
        return new Location(bukkitWorld, x, y, z);
    }

    /**
     * Gets a unique key representing the block's location.
     * Format: world:x:y:z
     * 
     * @return Location key as a String.
     */
    public String getLocationKey() {
        return world + ":" + x + ":" + y + ":" + z;
    }

    /**
     * Static method to generate location key from a Location object.
     * 
     * @param location Location object
     * @return Location key as a String.
     */
    public static String locationKey(Location location) {
        return location.getWorld().getName() + ":" +
                location.getBlockX() + ":" +
                location.getBlockY() + ":" +
                location.getBlockZ();
    }

    @Override
    public String toString() {
        return "TeamBlock{" +
                "world='" + world + '\'' +
                ", x=" + x +
                ", y=" + y +
                ", z=" + z +
                ", blockType=" + blockType +
                ", ownerName='" + ownerName + '\'' +
                ", teamId=" + teamId +
                ", placedDate=" + placedDate +
                '}';
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null || getClass() != obj.getClass())
            return false;

        TeamBlock that = (TeamBlock) obj;

        if (x != that.x)
            return false;
        if (y != that.y)
            return false;
        if (z != that.z)
            return false;
        return world.equals(that.world);
    }

    @Override
    public int hashCode() {
        int result = world.hashCode();
        result = 31 * result + x;
        result = 31 * result + y;
        result = 31 * result + z;
        return result;
    }
}
