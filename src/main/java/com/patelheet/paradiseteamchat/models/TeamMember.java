package com.patelheet.paradiseteamchat.models;

/**
 * Represents a player's membership in a team.
 * This is join table data model - links players to teams.
 */
public class TeamMember {
    private final int teamId;
    private final String playerName;
    private final long joinedDate;
    private String roleId;

    /**
     * Constructor for creating a TeamMember object
     * 
     * @param teamId     ID of the team
     * @param playerName Name of the player (case insensitive, stored in lowercase)
     * @param joinedDate Timestamp of when the player joined the team
     */
    public TeamMember(int teamId, String playerName, long joinedDate) {
        this.teamId = teamId;
        this.playerName = playerName.toLowerCase();
        this.joinedDate = joinedDate;
        this.roleId = "member";
    }

    /**
     * Constructor for creating a TeamMember object with a specific role
     * 
     * @param teamId     ID of the team
     * @param playerName Name of the player (case insensitive, stored in lowercase)
     * @param joinedDate Timestamp of when the player joined the team
     * @param roleId     Role ID assigned to the player
     */
    public TeamMember(int teamId, String playerName, long joinedDate, String roleId) {
        this.teamId = teamId;
        this.playerName = playerName.toLowerCase();
        this.joinedDate = joinedDate;
        this.roleId = roleId;
    }

    public int getTeamId() {
        return teamId;
    }

    public String getPlayerName() {
        return playerName;
    }

    public long getJoinedDate() {
        return joinedDate;
    }

    public String getRoleId() {
        return roleId;
    }

    public void setRoleId(String roleId) {
        this.roleId = roleId != null ? roleId.toLowerCase() : "member";
    }

    @Override
    public String toString() {
        return "TeamMember{" +
                "teamId=" + teamId +
                ", playerName='" + playerName + '\'' +
                ", roleId='" + roleId + '\'' +
                ", joinedDate=" + joinedDate +
                '}';
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null || getClass() != obj.getClass())
            return false;

        TeamMember that = (TeamMember) obj;

        if (teamId != that.teamId)
            return false;
        return playerName.equals(that.playerName);
    }

    @Override
    public int hashCode() {
        int result = teamId;
        result = 31 * result + playerName.hashCode();
        return result;
    }
}
