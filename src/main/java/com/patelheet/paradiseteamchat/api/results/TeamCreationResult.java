package com.patelheet.paradiseteamchat.api.results;

import com.patelheet.paradiseteamchat.api.models.TeamData;

/**
 * Result object for team creation operations.
 * Contains success status, the created team data (if successful), and error
 * messages (if failed).
 * 
 * <p>
 * This class is immutable and thread-safe.
 * </p>
 * 
 * @since API 1.0.0
 */
public class TeamCreationResult {
    private final boolean success;
    private final int teamId;
    private final TeamData teamData;
    private final String errorMessage;
    private final FailureReason failureReason;

    /**
     * Enum representing possible failure reasons.
     */
    public enum FailureReason {
        /** Team name already exists */
        NAME_EXISTS,

        /** Team tag already exists */
        TAG_EXISTS,

        /** Player is already in a team */
        PLAYER_IN_TEAM,

        /** Invalid team name format */
        INVALID_NAME,

        /** Invalid team tag format */
        INVALID_TAG,

        /** Name contains banned words */
        BANNED_WORD,

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
     * @param success       Whether the team creation was successful
     * @param teamId        The ID of the created team
     * @param teamData      The data of the created team
     * @param errorMessage  Error message if creation failed
     * @param failureReason Reason for failure if creation failed
     */
    private TeamCreationResult(boolean success, int teamId, TeamData teamData,
            String errorMessage, FailureReason failureReason) {
        this.success = success;
        this.teamId = teamId;
        this.teamData = teamData;
        this.errorMessage = errorMessage;
        this.failureReason = failureReason;
    }

    /**
     * Indicates if the team creation was successful.
     * 
     * @param teamId   The ID of the created team
     * @param teamData The data of the created team
     * @return A successful TeamCreationResult instance
     */
    public static TeamCreationResult success(int teamId, TeamData teamData) {
        return new TeamCreationResult(true, teamId, teamData, null, FailureReason.NONE);
    }

    /**
     * Creates a failed team creation result.
     * 
     * @param reason       The reason for failure
     * @param errorMessage Human-readable error message
     * @return A failed TeamCreationResult
     */
    public static TeamCreationResult failure(FailureReason reason, String errorMessage) {
        return new TeamCreationResult(false, -1, null, errorMessage, reason);
    }

    /**
     * Creates a failed result with unknown reason.
     * 
     * @param errorMessage Human-readable error message
     * @return A failed TeamCreationResult
     */
    public static TeamCreationResult failure(String errorMessage) {
        return failure(FailureReason.UNKNOWN, errorMessage);
    }

    /**
     * Checks if the team creation was successful.
     * 
     * @return true if successful, false otherwise
     */
    public boolean isSuccess() {
        return success;
    }

    /**
     * Gets the ID of the created team.
     * 
     * @return The team ID
     */
    public int getTeamId() {
        return teamId;
    }

    /**
     * Gets the data of the created team.
     * 
     * @return The TeamData object
     */
    public TeamData getTeamData() {
        return teamData;
    }

    /**
     * Gets the error message if creation failed.
     * 
     * @return The error message
     */
    public String getErrorMessage() {
        return errorMessage;
    }

    /**
     * Gets the reason for failure if creation failed.
     * 
     * @return The FailureReason enum value
     */
    public FailureReason getFailureReason() {
        return failureReason;
    }

    @Override
    public String toString() {
        if (success) {
            return "TeamCreationResult{success=true, teamId=" + teamId + ", team=" + teamData.getName() + "}";
        } else {
            return "TeamCreationResult{success=false, reason=" + failureReason + ", error='" + errorMessage + "'}";
        }
    }
}
