package com.patelheet.paradiseteamchat.utils;

import com.patelheet.paradiseteamchat.ParadiseTeamChatPlugin;
import com.patelheet.paradiseteamchat.config.ConfigManager;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Handles all input validation for the ParadiseTeamChat plugin.
 * Validates team names, tags, and sanitises user input to prevent malicious
 * content.
 * 
 * This class acts as the first line of defense against:
 * - Invalid characters in team names/tags
 * - Banned words (impersonation of staff)
 * - Colour code injection
 * - Invisible Unicode characters
 * - SQL injection (though prepared statements handle this too)
 */
public class InputValidator {

    private final ParadiseTeamChatPlugin plugin;
    private final ConfigManager configManager;

    /**
     * Constructor for InputValidator.
     * 
     * @param plugin The main plugin instance.
     */
    public InputValidator(ParadiseTeamChatPlugin plugin) {
        this.plugin = plugin;
        this.configManager = plugin.getConfigManager();
    }

    /**
     * Checks if the input contains any banned words.
     * 
     * This prevents players from creating teams with names or tags that impersonate
     * staff or use offensive language.
     * 
     * @param input The input string to check.
     * @return true if the input contains any banned words, false otherwise.
     */
    public boolean containsBannedWords(String input) {
        List<String> bannedWords = configManager.getBannedWords();
        boolean caseSensitive = configManager.isCaseSensitive();

        String checkInput = caseSensitive ? input : input.toLowerCase();

        for (String bannedWord : bannedWords) {
            String checkWord = caseSensitive ? bannedWord : bannedWord.toLowerCase();

            if (checkInput.contains(checkWord)) {
                plugin.getLogger().info("InputValidator: Detected banned word '" + bannedWord + "' in input.");
                return true;
            }
        }
        return false;
    }

    /**
     * Sanitises user input by removing colour codes and invisible Unicode
     * characters.
     * 
     * @param input The input string to sanitise.
     * @return The sanitised input string.
     */
    public String santiseInput(String input) {
        if (input == null) {
            return "";
        }

        // Remove colour codes (e.g., &a, §b)
        String sanitised = input.replaceAll("[&§][0-9a-fk-or]", "");

        // Remove invisible Unicode characters
        sanitised = sanitised.replaceAll("[\u200B-\u200D\uFEFF]", "");

        // Trim leading and trailing whitespace
        sanitised = sanitised.trim();

        return sanitised;
    }

    /**
     * Represents the result of an input validation operation.
     * 
     * Instead of throwing exceptions for invalid input, methods can return
     * instances of this class to indicate success or failure, along with relevant
     * messages.
     */
    public static class ValidationResult {
        private final boolean success;
        private final String value;
        private final String errorMessage;

        /**
         * Constructor for ValidationResult.
         * 
         * @param success      Whether the validation was successful.
         * @param value        The validated value if successful.
         * @param errorMessage The error message if validation failed.
         */
        public ValidationResult(boolean success, String value, String errorMessage) {
            this.success = success;
            this.value = value;
            this.errorMessage = errorMessage;
        }

        /**
         * Creates a successful ValidationResult.
         * 
         * @param value The validated value.
         * @return A successful ValidationResult instance. (success, sanitised value,
         *         null)
         */
        public static ValidationResult success(String value) {
            return new ValidationResult(true, value, null);
        }

        /**
         * Creates a failed ValidationResult.
         * 
         * @param errorMessage The error message describing the failure.
         * @return A failed ValidationResult instance. (false, null, error message)
         */
        public static ValidationResult failure(String errorMessage) {
            return new ValidationResult(false, null, errorMessage);
        }

        /**
         * Indicates whether the validation was successful.
         * 
         * @return true if successful, false otherwise.
         */
        public boolean isSuccess() {
            return success;
        }

        /**
         * Gets the validated value.
         * 
         * @return The validated value if successful, null otherwise.
         */
        public String getValue() {
            return value;
        }

        /**
         * Gets the error message.
         * 
         * @return The error message if validation failed, null otherwise.
         */
        public String getErrorMessage() {
            return errorMessage;
        }

        @Override
        public String toString() {
            if (success) {
                return "ValidationResult{success=true, value='" + value + "'}";
            } else {
                return "ValidationResult{success=false, error='" + errorMessage + "'}";
            }
        }
    }

    /**
     * Validates a team name according to configured rules.
     * 
     * @param teamName The team name to validate.
     * @return A ValidationResult indicating success or failure with relevant
     *         messages.
     */
    public ValidationResult validateTeamName(String teamName) {
        String sanitised = santiseInput(teamName);

        if (sanitised.isEmpty()) {
            return ValidationResult.failure("Team name cannot be empty or consist solely of invalid characters.");
        }

        int maxLength = configManager.getMaxNameLength();
        if (sanitised.length() > maxLength) {
            return ValidationResult
                    .failure(configManager.getMessage("name-too-long", "{max}", String.valueOf(maxLength)));
        }

        Pattern namePattern = configManager.getNamePattern();
        if (!namePattern.matcher(sanitised).matches()) {
            return ValidationResult.failure(configManager.getMessage("invalid-name"));
        }

        if (containsBannedWords(sanitised)) {
            return ValidationResult.failure(configManager.getMessage("banned-word"));
        }

        return ValidationResult.success(sanitised);
    }

    /**
     * Validates a team tag according to configured rules.
     * 
     * @param teamTag The team tag to validate.
     * @return A ValidationResult indicating success or failure with relevant
     *         messages.
     */
    public ValidationResult validateTeamTag(String teamTag) {
        String sanitised = santiseInput(teamTag);

        if (sanitised.isEmpty()) {
            return ValidationResult.failure("Team tag cannot be empty or consist solely of invalid characters.");
        }

        int maxLength = configManager.getMaxTagLength();
        if (sanitised.length() > maxLength) {
            return ValidationResult
                    .failure(configManager.getMessage("tag-too-long", "{max}", String.valueOf(maxLength)));
        }

        Pattern tagPattern = configManager.getTagPattern();
        if (!tagPattern.matcher(sanitised).matches()) {
            return ValidationResult.failure(configManager.getMessage("invalid-tag"));
        }

        if (containsBannedWords(sanitised)) {
            return ValidationResult.failure(configManager.getMessage("banned-word"));
        }

        return ValidationResult.success(sanitised);
    }

    /**
     * Validates a player name for invitations.
     * 
     * @param playerName The player name to validate.
     * @param senderName The name of the player sending the invite.
     * @return A ValidationResult indicating success or failure with relevant
     *         messages.
     */
    public ValidationResult validatePlayerName(String playerName, String senderName) {
        if (playerName == null || playerName.trim().isEmpty()) {
            return ValidationResult.failure("Player name cannot be empty.");
        }

        String sanitised = santiseInput(playerName);

        if (sanitised.equalsIgnoreCase(senderName)) {
            return ValidationResult.failure(configManager.getMessage("cannot-invite-self"));
        }

        if (!Pattern.matches("^[A-Za-z0-9_]+$", sanitised)) {
            return ValidationResult.failure("Invalid player name format.");
        }

        return ValidationResult.success(sanitised);
    }

}
