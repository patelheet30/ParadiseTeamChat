package com.patelheet.paradiseteamchat.api.models;

import java.util.List;
import java.util.Map;

/**
 * Immutable interface representing a Team.
 * 
 * <p>
 * This interface provides read-only access to team data. All collections
 * returned are immutable or defensive copies to ensure thread safety.
 * <p>
 * 
 * <b>Thread safety:</b> Implementations of this interface must be thread-safe
 * for reads.
 * 
 * @author Heet Patel (patelheet30)
 * @version 1.0.0
 * @since 3.0.0
 */
public interface ITeam {
    /**
     * Gets the unique ID of the team.
     * 
     * @return the team ID (positive integer)
     */
    int getId();

    /**
     * Gets the name of the team.
     * 
     * @return the team name (never null or empty)
     */
    String getName();

    /**
     * Gets the tag of the team.
     * 
     * <p>
     * The tag is a short identifier for the team, often used in chat
     * prefixes.
     * 
     * @return the team tag (never null or empty)
     */
    String getTag();

    /**
     * Gets the owner's player name.
     * 
     * <p>
     * The owner has full permissions and cannot be kicked.
     * 
     * @return the owner's player name in lowercase (never null or empty)
     */
    String getOwnerName();

    /**
     * Gets the timestamp when the team was created.
     * 
     * @return the creation timestamp in milliseconds since epoch
     */
    long getCreatedDate();

    /**
     * Gets the maximum number of members allowed in the team.
     * 
     * @return the member limit
     */
    int getMemberLimit();

    /**
     * Gets an immutable list of all member player names in the team.
     * 
     * <p>
     * The list includes the owner and all regular members.
     * All names are in lowercase.
     * 
     * <p>
     * <b>Thread safety:</b> The returned list is immutable or a defensive copy.
     * 
     * @return an immutable list of member player names (never null, never empty)
     */
    List<String> getMembers();

    /**
     * Gets the current number of members in the team.
     * 
     * <p>
     * This includes the owner and all regular members.
     * 
     * @return the member count (always atleast 1)
     */
    int getMemberCount();

    /**
     * Gets the role ID assigned to a specific member in the team.
     * 
     * @param playerName The player's name whose role is to be fetched.
     *                   (case-insensitive)
     * @return The role ID of the member, defaults to "member"
     */
    String getMemberRole(String playerName);

    /**
     * Gets the roles assigned to each member in the team.
     * 
     * <p>
     * <bt>Thread safety:</b> The returned map is immutable or a defensive copy.
     * 
     * @return a map of player names to their role IDs (never null)
     */
    Map<String, String> getAllMemberRoles();

    /**
     * Checks if the team has reached its member limit.
     * 
     * @return true if the team is full, false otherwise
     */
    boolean isFull();

    /**
     * Checks if a player is a member of the team.
     * 
     * @param playerName The player's name to check (case-insensitive)
     * @return true if the player is a member, false otherwise
     */
    boolean isMember(String playerName);

    /**
     * Checks if a player is the owner of the team.
     * 
     * @param playerName The player's name to check (case-insensitive)
     * @return true if the player is the owner, false otherwise
     */
    boolean isOwner(String playerName);
}
