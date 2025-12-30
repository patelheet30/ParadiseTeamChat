package com.patelheet.paradiseteamchat.database;

import com.patelheet.paradiseteamchat.ParadiseTeamChatPlugin;
import com.patelheet.paradiseteamchat.models.Team;
import com.patelheet.paradiseteamchat.models.TeamMember;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Repository class for managing Team and TeamMember data in the database.
 */
public class TeamRepository {
    private final ParadiseTeamChatPlugin plugin;
    private final DatabaseManager databaseManager;

    /**
     * Constructor for TeamRepository.
     * 
     * @param plugin          The main plugin instance.
     * @param databaseManager The database manager instance.
     */
    public TeamRepository(ParadiseTeamChatPlugin plugin, DatabaseManager databaseManager) {
        this.plugin = plugin;
        this.databaseManager = databaseManager;
    }

    /**
     * Build a Team object from a ResultSet.
     * 
     * @param rs The ResultSet containing team data.
     * @return A Team object.
     * @throws SQLException if a database access error occurs
     */
    private Team buildTeamFromResultSet(ResultSet rs) throws SQLException {
        return new Team(
                rs.getInt("id"),
                rs.getString("name"),
                rs.getString("tag"),
                rs.getString("owner_name"),
                rs.getLong("created_date"),
                rs.getInt("member_limit"));
    }

    /**
     * Loads team members into the given Team object.
     * 
     * @param team The Team object to load members into.
     */
    private void loadTeamMembers(Team team) {
        List<TeamMember> members = getTeamMembers(team.getId());

        for (TeamMember member : members) {
            // Add member to team object (skips owner since already added in constructor)
            if (!member.getPlayerName().equals(team.getOwnerName())) {
                team.addMember(member.getPlayerName());
            }
        }
    }

    /**
     * Creates a new team in the database.
     * 
     * @param team The team to create.
     * @return The ID of the newly created team, or -1 if creation failed.
     */
    public int createTeam(Team team) {
        String sql = "INSERT INTO teams (name, name_lower, tag, owner_name, created_date, member_limit) " +
                "VALUES (?, ?, ?, ?, ?, ?);";

        Connection conn = databaseManager.getConnection();

        try (PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, team.getName());
            pstmt.setString(2, team.getName().toLowerCase());
            pstmt.setString(3, team.getTag());
            pstmt.setString(4, team.getOwnerName());
            pstmt.setLong(5, team.getCreatedDate());
            pstmt.setInt(6, team.getMemberLimit());

            int affectedRows = pstmt.executeUpdate();

            if (affectedRows == 0) {
                plugin.getLogger().warning("Creating team failed, no rows affected.");
                return -1;
            }

            try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    int teamId = generatedKeys.getInt(1);

                    addMember(teamId, team.getOwnerName());

                    plugin.getLogger().info("Team '" + team.getName() + "' created with ID: " + teamId);
                    return teamId;
                } else {
                    plugin.getLogger().warning("Creating team failed, no ID obtained.");
                    return -1;
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to create team: " + e.getMessage());
            e.printStackTrace();
            return -1;
        }
    }

    /**
     * Deletes a team from the database.
     * 
     * @param teamId The ID of the team to delete.
     * @return True if the team was deleted successfully, false otherwise.
     */
    public boolean deleteTeam(int teamId) {
        String sql = "DELETE FROM teams WHERE id = ?;";

        Connection conn = databaseManager.getConnection();

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, teamId);

            int affectedRows = pstmt.executeUpdate();

            if (affectedRows > 0) {
                plugin.getLogger().info("Team deleted: ID " + teamId);
                return true;
            } else {
                plugin.getLogger().warning("Team deletion failed: ID " + teamId + " not found");
                return false;
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to delete team: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Gets a team by its name.
     * 
     * @param name The name of the team.
     * @return The Team object, or null if not found.
     */
    public Team getTeamByName(String name) {
        String sql = "SELECT * FROM teams WHERE name_lower = ?";

        Connection conn = databaseManager.getConnection();

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, name.toLowerCase());

            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                Team team = buildTeamFromResultSet(rs);
                loadTeamMembers(team);
                return team;
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Error getting team by name: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Gets a team by its ID.
     * 
     * @param id The ID of the team.
     * @return The Team object, or null if not found.
     */
    public Team getTeamById(int id) {
        String sql = "SELECT * FROM teams WHERE id = ?";

        Connection conn = databaseManager.getConnection();

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);

            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                Team team = buildTeamFromResultSet(rs);
                loadTeamMembers(team);
                return team;
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Error getting team by ID: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Gets the team by a player's name.
     * 
     * @param playerName The player's name.
     * @return The Team object, or null if not found.
     */
    public Team getTeamByPlayer(String playerName) {
        String sql = "SELECT t.* FROM teams t " +
                "INNER JOIN team_members tm ON t.id = tm.team_id " +
                "WHERE tm.player_name = ?";

        Connection conn = databaseManager.getConnection();

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, playerName.toLowerCase());

            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                Team team = buildTeamFromResultSet(rs);
                loadTeamMembers(team);
                return team;
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Error getting team by player: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Adds a member to a team.
     * 
     * @param teamId     The ID of the team.
     * @param playerName The name of the player to add.
     * @return True if the member was added successfully, false otherwise.
     */
    public boolean addMember(int teamId, String playerName) {
        String sql = "INSERT INTO team_members (team_id, player_name, joined_date) VALUES (?, ?, ?)";

        Connection conn = databaseManager.getConnection();

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, teamId);
            pstmt.setString(2, playerName.toLowerCase());
            pstmt.setLong(3, System.currentTimeMillis());

            int affectedRows = pstmt.executeUpdate();

            if (affectedRows > 0) {
                plugin.getLogger().info("Member added: " + playerName + " to team ID " + teamId);
                return true;
            }
        } catch (SQLException e) {
            if (e.getMessage().contains("UNIQUE constraint failed")) {
                plugin.getLogger().warning("Player " + playerName + " is already in team ID " + teamId);
            } else {
                plugin.getLogger().severe("Error adding member: " + e.getMessage());
                e.printStackTrace();
            }
        }
        return false;
    }

    /**
     * Remove a member from a team.
     * 
     * @param teamId     The ID of the team.
     * @param playerName The name of the player to remove.
     * @return True if the member was removed successfully, false otherwise.
     */
    public boolean removeMember(int teamId, String playerName) {
        String sql = "DELETE FROM team_members WHERE team_id = ? AND player_name = ?";

        Connection conn = databaseManager.getConnection();

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, teamId);
            pstmt.setString(2, playerName.toLowerCase());

            int affectedRows = pstmt.executeUpdate();

            if (affectedRows > 0) {
                plugin.getLogger().info("Member removed: " + playerName + " from team ID " + teamId);
                return true;
            } else {
                plugin.getLogger().warning("Member removal failed: " + playerName + " not found in team ID " + teamId);
                return false;
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Error removing member: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Get all members of a team.
     * 
     * @param teamId The ID of the team.
     * @return A list of TeamMember objects.
     */
    public List<TeamMember> getTeamMembers(int teamId) {
        List<TeamMember> members = new ArrayList<>();
        String sql = "SELECT * FROM team_members WHERE team_id = ?";

        Connection conn = databaseManager.getConnection();

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, teamId);

            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                members.add(new TeamMember(
                        rs.getInt("team_id"),
                        rs.getString("player_name"),
                        rs.getLong("joined_date")));
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Error getting team members: " + e.getMessage());
            e.printStackTrace();
        }
        return members;
    }

    /**
     * Get all teams in the database.
     * 
     * @return A list of Team objects.
     */
    public List<Team> getAllTeams() {
        List<Team> teams = new ArrayList<>();
        String sql = "SELECT * FROM teams";

        Connection conn = databaseManager.getConnection();

        try (Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Team team = buildTeamFromResultSet(rs);
                loadTeamMembers(team);
                teams.add(team);
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Error getting all teams: " + e.getMessage());
            e.printStackTrace();
        }
        return teams;
    }
}
