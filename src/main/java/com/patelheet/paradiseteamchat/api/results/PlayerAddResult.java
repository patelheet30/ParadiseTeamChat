package com.patelheet.paradiseteamchat.api.results;

/**
 * Result object for player addition operations.
 * Indicates whether a player was successfully added to a team.
 * 
 * <p>
 * This class is immutable and thread-safe.
 * </p>
 * 
 * @since API 1.0.0
 */
public class PlayerAddResult {
    private final boolean success;
    private final String playerName;
    private final int teamId;
    private final String errorMessage;
    private final FailureReason failureReason;

    /**
     * Enum representing possible failure reasons.
     */
    public enum FailureReason {
        /** Player is already in another team */
        ALREADY_IN_TEAM,

        /** Team is at member capacity */
        TEAM_FULL,

        /** Team does not exist */
        TEAM_NOT_FOUND,

        /** Player is already a member of this team */
        ALREADY_MEMBER,

        /** Invalid player name */
        INVALID_PLAYER,

        /** Database error occurred */
        DATABASE_ERROR,

        /** Unknown error */
        UNKNOWN,

        /** No failure (success case) */
        NONE
    }

    /**
     * Private constructor - use factory methods instead.
     * 
     * @param success       Whether the player addition was successful
     * @param playerName    The name of the player being added
     * @param teamId        The ID of the team to which the player is being added
     * @param errorMessage  Error message if addition failed
     * @param failureReason Reason for failure if addition failed
     */
    private PlayerAddResult(boolean success, String playerName, int teamId,
            String errorMessage, FailureReason failureReason) {
        this.success = success;
        this.playerName = playerName;
        this.teamId = teamId;
        this.errorMessage = errorMessage;
        this.failureReason = failureReason;
    }

    /**
     * Creates a successful player addition result.
     * 
     * @param playerName The name of the player that was added
     * @param teamId     The ID of the team they were added to
     * @return A successful PlayerAddResult
     */
    public static PlayerAddResult success(String playerName, int teamId) {
        return new PlayerAddResult(true, playerName, teamId, null, FailureReason.NONE);
    }

    /**
     * Creates a failed player addition result.
     * 
     * @param reason       The reason for failure
     * @param errorMessage Human-readable error message
     * @return A failed PlayerAddResult
     */
    public static PlayerAddResult failure(FailureReason reason, String errorMessage) {
        return new PlayerAddResult(false, null, -1, errorMessage, reason);
    }

    /**
     * Creates a failed result with unknown reason.
     * 
     * @param errorMessage Human-readable error message
     * @return A failed PlayerAddResult
     */
    public static PlayerAddResult failure(String errorMessage) {
        return failure(FailureReason.UNKNOWN, errorMessage);
    }

    /**
     * Checks if the player addition was successful.
     * 
     * @return true if successful, false otherwise
     */
    public boolean isSuccess() {
        return success;
    }

    /**
     * Gets the name of the player that was added.
     * 
     * @return The player name, or null if addition failed
     */
    public String getPlayerName() {
        return playerName;
    }

    /**
     * Gets the ID of the team the player was added to.
     * 
     * @return The team ID, or -1 if addition failed
     */
    public int getTeamId() {
        return teamId;
    }

    /**
     * Gets the error message if addition failed.
     * 
     * @return The error message, or null if successful
     */
    public String getErrorMessage() {
        return errorMessage;
    }

    /**
     * Gets the specific failure reason.
     * 
     * @return The failure reason, or NONE if successful
     */
    public FailureReason getFailureReason() {
        return failureReason;
    }

    @Override
    public String toString() {
        if (success) {
            return "PlayerAddResult{success=true, player='" + playerName + "', teamId=" + teamId + "}";
        } else {
            return "PlayerAddResult{success=false, reason=" + failureReason + ", error='" + errorMessage + "'}";
        }
    }
}
