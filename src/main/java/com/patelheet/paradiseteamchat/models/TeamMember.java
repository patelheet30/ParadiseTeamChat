package com.patelheet.paradiseteamchat.models;

/**
 * Represents a player's membership in a team.
 * This is join table data model - links players to teams.
 */
public class TeamMember {
    private final int teamId;
    private final String playerName;
    private final long joinedDate;

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

    @Override
    public String toString() {
        return "TeamMember{" +
                "teamId=" + teamId +
                ", playerName='" + playerName + '\'' +
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
