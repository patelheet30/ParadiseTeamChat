package com.patelheet.paradiseteamchat.database;

import com.patelheet.paradiseteamchat.ParadiseTeamChatPlugin;
import com.patelheet.paradiseteamchat.models.TeamBlock;
import org.bukkit.Location;
import org.bukkit.Material;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Repository for managing TeamBlock entities in the database.
 */
public class TeamBlockRepository {

    private final ParadiseTeamChatPlugin plugin;
    private final DatabaseManager databaseManager;

    /**
     * Constructor for TeamBlockRepository.
     * 
     * @param plugin          The main plugin instance.
     * @param databaseManager The database manager instance.
     */
    public TeamBlockRepository(ParadiseTeamChatPlugin plugin, DatabaseManager databaseManager) {
        this.plugin = plugin;
        this.databaseManager = databaseManager;
    }

    /**
     * Builds a TeamBlock object from the current row of the given ResultSet.
     * 
     * @param rs The ResultSet positioned at the desired row.
     * @return The constructed TeamBlock object.
     * @throws SQLException if a database access error occurs
     */
    private TeamBlock buildTeamBlockFromResultSet(ResultSet rs) throws SQLException {
        return new TeamBlock(
                rs.getString("world"),
                rs.getInt("x"),
                rs.getInt("y"),
                rs.getInt("z"),
                Material.getMaterial(rs.getString("block_type")),
                rs.getString("owner_name"),
                rs.getInt("team_id"),
                rs.getLong("placed_date"));
    }

    /**
     * Adds a new TeamBlock to the database.
     * 
     * @param block The TeamBlock to add.
     * @return true if the block was added successfully, false otherwise.
     */
    public boolean addTeamBlock(TeamBlock block) {
        String sql = "INSERT INTO team_blocks (world, x, y, z, block_type, owner_name, team_id, placed_date) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?);";

        Connection conn = databaseManager.getConnection();

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, block.getWorld());
            pstmt.setInt(2, block.getX());
            pstmt.setInt(3, block.getY());
            pstmt.setInt(4, block.getZ());
            pstmt.setString(5, block.getBlockType().name());
            pstmt.setString(6, block.getOwnerName().toLowerCase());
            pstmt.setInt(7, block.getTeamId());
            pstmt.setLong(8, block.getPlacedDate());

            int affectedRows = pstmt.executeUpdate();

            if (affectedRows > 0) {
                plugin.logDebug("Team block added: " + block.getBlockType() + " at " + block.getLocationKey());
                return true;
            }
        } catch (SQLException e) {
            if (e.getMessage().contains("UNIQUE constraint failed")) {
                plugin.getLogger().warning("Team block already exists at " + block.getLocationKey());
            } else {
                plugin.getLogger().severe("Error adding team block: " + e.getMessage());
                e.printStackTrace();
            }
        }
        return false;
    }

    /**
     * Retrieves a TeamBlock from the database by its location.
     * 
     * @param world The world name.
     * @param x     The x coordinate.
     * @param y     The y coordinate.
     * @param z     The z coordinate.
     * @return The TeamBlock if found, null otherwise.
     */
    public TeamBlock getTeamBlock(String world, int x, int y, int z) {
        String sql = "SELECT * FROM team_blocks WHERE world = ? AND x = ? AND y = ? AND z = ?";

        Connection conn = databaseManager.getConnection();

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, world);
            pstmt.setInt(2, x);
            pstmt.setInt(3, y);
            pstmt.setInt(4, z);

            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return buildTeamBlockFromResultSet(rs);
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Error getting team block: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Retrieves a TeamBlock from the database by its Location.
     * 
     * @param location The Location of the block.
     * @return The TeamBlock if found, null otherwise.
     */
    public TeamBlock getTeamBlock(Location location) {
        return getTeamBlock(
                location.getWorld().getName(),
                location.getBlockX(),
                location.getBlockY(),
                location.getBlockZ());
    }

    /**
     * Removes a TeamBlock from the database by its location.
     * 
     * @param world The world name.
     * @param x     The x coordinate.
     * @param y     The y coordinate.
     * @param z     The z coordinate.
     * @return true if the block was removed successfully, false otherwise.
     */
    public boolean removeTeamBlock(String world, int x, int y, int z) {
        String sql = "DELETE FROM team_blocks WHERE world = ? AND x = ? AND y = ? AND z = ?";

        Connection conn = databaseManager.getConnection();

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, world);
            pstmt.setInt(2, x);
            pstmt.setInt(3, y);
            pstmt.setInt(4, z);

            int affectedRows = pstmt.executeUpdate();

            if (affectedRows > 0) {
                plugin.logDebug("Team block removed at " + world + ":" + x + ":" + y + ":" + z);
                return true;
            } else {
                plugin.getLogger().warning("Team block removal failed: not found at " +
                        world + ":" + x + ":" + y + ":" + z);
                return false;
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Error removing team block: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Removes a TeamBlock from the database by its Location.
     * 
     * @param location The Location of the block.
     * @return true if the block was removed successfully, false otherwise.
     */
    public boolean removeTeamBlock(Location location) {
        return removeTeamBlock(
                location.getWorld().getName(),
                location.getBlockX(),
                location.getBlockY(),
                location.getBlockZ());
    }

    /**
     * Retrieves all TeamBlocks for a specific team.
     * 
     * @param teamId The ID of the team.
     * @return A list of TeamBlocks belonging to the team.
     */
    public List<TeamBlock> getTeamBlocks(int teamId) {
        List<TeamBlock> blocks = new ArrayList<>();
        String sql = "SELECT * FROM team_blocks WHERE team_id = ?";

        Connection conn = databaseManager.getConnection();

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, teamId);

            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                blocks.add(buildTeamBlockFromResultSet(rs));
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Error getting team blocks: " + e.getMessage());
            e.printStackTrace();
        }
        return blocks;
    }

    /**
     * Retrieves all TeamBlocks of a specific type.
     * 
     * @param blockType The type of the block.
     * @return A list of TeamBlocks of the specified type.
     */
    public List<TeamBlock> getTeamBlocksByType(Material blockType) {
        List<TeamBlock> blocks = new ArrayList<>();
        String sql = "SELECT * FROM team_blocks WHERE block_type = ?";

        Connection conn = databaseManager.getConnection();

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, blockType.name());

            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                blocks.add(buildTeamBlockFromResultSet(rs));
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Error getting team blocks by type: " + e.getMessage());
            e.printStackTrace();
        }
        return blocks;
    }

    /**
     * Retrieves all TeamBlocks in the database.
     * 
     * @return A list of all TeamBlocks.
     */
    public List<TeamBlock> getAllTeamBlocks() {
        List<TeamBlock> blocks = new ArrayList<>();
        String sql = "SELECT * FROM team_blocks";

        Connection conn = databaseManager.getConnection();

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                blocks.add(buildTeamBlockFromResultSet(rs));
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Error getting all team blocks: " + e.getMessage());
            e.printStackTrace();
        }
        return blocks;
    }

    /**
     * Removes all TeamBlocks for a specific team. Called when a team is disbanded.
     * 
     * @param teamId The ID of the team.
     * @return true if the blocks were removed successfully, false otherwise.
     */
    public boolean removeAllTeamBlocks(int teamId) {
        String sql = "DELETE FROM team_blocks WHERE team_id = ?";

        Connection conn = databaseManager.getConnection();

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, teamId);

            int affectedRows = pstmt.executeUpdate();

            plugin.logDebug("Removed " + affectedRows + " team blocks for team ID " + teamId);
            return true;
        } catch (SQLException e) {
            plugin.getLogger().severe("Error removing team blocks for team: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Gets the count of TeamBlocks for a specific team.
     * 
     * @param teamId The ID of the team.
     * @return The count of TeamBlocks.
     */
    public int getTeamBlockCount(int teamId) {
        String sql = "SELECT COUNT(*) FROM team_blocks WHERE team_id = ?";

        Connection conn = databaseManager.getConnection();

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, teamId);

            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Error counting team blocks: " + e.getMessage());
            e.printStackTrace();
        }
        return 0;
    }

}
