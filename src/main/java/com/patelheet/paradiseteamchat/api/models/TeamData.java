package com.patelheet.paradiseteamchat.api.models;

import com.patelheet.paradiseteamchat.models.Team;

import java.util.Collections;
import java.util.List;

/**
 * Immutable data transfer object representing a team.
 * This is the public-facing API representation of a team.
 * 
 * <p>
 * This class is thread-safe and immutable - all data is copied
 * at construction time and cannot be modified after creation.
 * </p>
 * 
 * @since API 1.0.0
 */
public class TeamData {

    private final int id;
    private final String name;
    private final String tag;
    private final String ownerName;
    private final long createdDate;
    private final int memberLimit;
    private final List<String> members;

    /**
     * Private constructor - use factory method {@link #fromTeam(Team)} instead.
     * 
     * @param id          The team's unique database ID
     * @param name        The team's name
     * @param tag         The team's tag
     * @param ownerName   The owner's username (lowercase)
     * @param createdDate Unix timestamp of creation
     * @param memberLimit Maximum number of members allowed
     * @param members     Unmodifiable list of member names
     */
    public TeamData(int id, String name, String tag, String ownerName, long createdDate, int memberLimit,
            List<String> members) {
        this.id = id;
        this.name = name;
        this.tag = tag;
        this.ownerName = ownerName;
        this.createdDate = createdDate;
        this.memberLimit = memberLimit;
        this.members = Collections.unmodifiableList(members);
    }

    /**
     * Creates a TeamData instance from an internal Team object.
     * This is the primary way to convert internal team data to API format.
     * 
     * @param team The internal team object to convert
     * @return An immutable TeamData representation
     * @throws NullPointerException if team is null
     */
    public static TeamData fromTeam(Team team) {
        if (team == null) {
            throw new NullPointerException("Team cannot be null");
        }

        return new TeamData(
                team.getId(),
                team.getName(),
                team.getTag(),
                team.getOwnerName(),
                team.getCreatedDate(),
                team.getMemberLimit(),
                List.copyOf(team.getMembers()) // Defensive copy
        );
    }

    // == Getters ==

    /**
     * Gets the unique database ID of the team.
     * 
     * @return The team's ID
     */
    public int getId() {
        return id;
    }

    /**
     * Gets the name of the team.
     * 
     * @return The team's name
     */
    public String getName() {
        return name;
    }

    /**
     * Gets the team's tag (short identifier).
     * 
     * @return The team's tag
     */
    public String getTag() {
        return tag;
    }

    /**
     * Gets the owner's username (lowercase).
     * 
     * @return The owner's name
     */
    public String getOwnerName() {
        return ownerName;
    }

    /**
     * Gets the Unix timestamp of when the team was created.
     * 
     * @return The creation date as a Unix timestamp
     */
    public long getCreatedDate() {
        return createdDate;
    }

    /**
     * Gets the maximum number of members allowed in the team.
     * 
     * @return The member limit
     */
    public int getMemberLimit() {
        return memberLimit;
    }

    /**
     * Gets an unmodifiable list of member names in the team.
     * 
     * @return The list of member names
     */
    public List<String> getMembers() {
        return members;
    }

    /**
     * Gets the current number of members in the team.
     * 
     * @return The current member count
     */
    public int getMemberCount() {
        return members.size();
    }

    // == Utility Methods ==

    /**
     * Checks if the team has reached its member limit.
     * 
     * @return True if the team is full, false otherwise
     */
    public boolean isFull() {
        return members.size() >= memberLimit;
    }

    /**
     * Checks if a player is a member of the team.
     * 
     * @param playerName The player's username to check
     * @return True if the player is a member, false otherwise
     */
    public boolean isMember(String playerName) {
        if (playerName == null) {
            return false;
        }
        return members.contains(playerName.toLowerCase());
    }

    /**
     * Checks if a player is the owner of the team.
     * 
     * @param playerName The player's username to check
     * @return True if the player is the owner, false otherwise
     */
    public boolean isOwner(String playerName) {
        if (playerName == null) {
            return false;
        }
        return ownerName.equalsIgnoreCase(playerName);
    }

    /**
     * Gets the number of available slots left in the team.
     * 
     * @return The number of available member slots
     */
    public int getAvailableSlots() {
        return Math.max(0, memberLimit - members.size());
    }

    // == Overrides ==

    @Override
    public String toString() {
        return "TeamData{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", tag='" + tag + '\'' +
                ", ownerName='" + ownerName + '\'' +
                ", members=" + members.size() + "/" + memberLimit +
                '}';
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null || getClass() != obj.getClass())
            return false;

        TeamData teamData = (TeamData) obj;
        return id == teamData.id;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(id);
    }
}
