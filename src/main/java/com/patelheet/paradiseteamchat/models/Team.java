package com.patelheet.paradiseteamchat.models;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * Represents a team in the ParadiseTeamChat plugin.
 * This is a pure data model - contains only data and basic getters/setters.
 */
public class Team {

    private final int id;
    private final String name;
    private final String tag;
    private final String ownerName;
    private final long createdDate;
    private final int memberLimit;
    private final List<String> members;
    private final Map<String, String> memberRoles;

    /**
     * Constructor for creating a Team object
     * 
     * @param id          Database ID of the team
     * @param name        Name of the team (full name)
     * @param tag         Tag of the team (short identifier)
     * @param ownerName   Name of the team's owner
     * @param createdDate Timestamp of when the team was created
     * @param memberLimit Maximum number of members allowed in the team
     */
    public Team(int id, String name, String tag, String ownerName, long createdDate, int memberLimit) {
        this.id = id;
        this.name = name;
        this.tag = tag;
        this.ownerName = ownerName.toLowerCase();
        this.createdDate = createdDate;
        this.memberLimit = memberLimit;
        this.members = new ArrayList<>();
        this.memberRoles = new HashMap<>();

        this.members.add(this.ownerName); // Owner is the first member of the team
        this.memberRoles.put(this.ownerName, "owner");
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getTag() {
        return tag;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public long getCreatedDate() {
        return createdDate;
    }

    public int getMemberLimit() {
        return memberLimit;
    }

    public List<String> getMembers() {
        return new ArrayList<>(members);
    }

    public int getMemberCount() {
        return members.size();
    }

    /**
     * Get the role of a member in the team
     * 
     * @param playerName Name of the player
     * @return Role ID of the player
     */
    public String getMemberRole(String playerName) {
        return memberRoles.getOrDefault(playerName.toLowerCase(), "member");
    }

    /**
     * Set the role of a member in the team
     * 
     * @param playerName Name of the player
     * @param roleId     Role ID to assign to the player
     */
    public void setMemberRole(String playerName, String roleId) {
        String lowerName = playerName.toLowerCase();
        if (members.contains(lowerName)) {
            memberRoles.put(lowerName, roleId.toLowerCase());
        }
    }

    /**
     * Get a map of all member names to their roles
     * 
     * @return Map of member names to role IDs
     */
    public Map<String, String> getAllMemberRoles() {
        return new HashMap<>(memberRoles);
    }

    /**
     * Check if the team is full (has reached its member limit)
     * 
     * @return True if the team has reached its member limit, false otherwise
     */
    public boolean isFull() {
        return members.size() >= memberLimit;
    }

    /**
     * Check if a player is a member of the team
     * 
     * @param playerName Name of the player to check
     * @return True if the player is a member, false otherwise
     */
    public boolean isMember(String playerName) {
        return members.contains(playerName.toLowerCase());
    }

    /**
     * Check if a player is the owner of the team
     * 
     * @param playerName Name of the player to check
     * @return True if the player is the owner, false otherwise
     */
    public boolean isOwner(String playerName) {
        return ownerName.equalsIgnoreCase(playerName);
    }

    /**
     * Add a member to the team (in-memory only).
     * This does not persist to the database
     * Assigns the default role "member"
     * 
     * @param playerName Name of the player to add
     * @return True if the player was added successfully, false otherwise
     */
    public boolean addMember(String playerName) {
        return addMember(playerName, "member");
    }

    /**
     * Add a member to the team with a specific role (in-memory only).
     * This does not persist to the database
     * 
     * @param playerName Name of the player to add
     * @param roleId     Role ID to assign to the player
     * @return True if the player was added successfully, false otherwise
     */
    public boolean addMember(String playerName, String roleId) {
        String lowerName = playerName.toLowerCase();

        if (isFull()) {
            return false;
        }

        if (isMember(lowerName)) {
            return false;
        }

        boolean added = members.add(lowerName);
        if (added) {
            memberRoles.put(lowerName, roleId.toLowerCase());
        }
        return added;
    }

    /**
     * Remove a member from the team (in-memory only).
     * This does not persist to the database
     * 
     * @param playerName Name of the player to remove
     * @return True if the player was removed successfully, false otherwise
     */
    public boolean removeMember(String playerName) {
        String lowerName = playerName.toLowerCase();
        boolean removed = members.remove(lowerName);
        if (removed) {
            memberRoles.remove(lowerName);
        }
        return removed;
    }

    @Override
    public String toString() {
        return "Team{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", tag='" + tag + '\'' +
                ", ownerName='" + ownerName + '\'' +
                ", members=" + members.size() + "/" + memberLimit +
                '}';
    }
}
