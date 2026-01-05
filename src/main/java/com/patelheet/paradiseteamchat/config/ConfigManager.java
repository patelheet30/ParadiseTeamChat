package com.patelheet.paradiseteamchat.config;

import org.bukkit.configuration.file.FileConfiguration;

import com.patelheet.paradiseteamchat.ParadiseTeamChatPlugin;

import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Manages the plugin's configuration file.
 * Loads config.yml, provides easy access to values, and handles validation.
 */
public class ConfigManager {
    private final ParadiseTeamChatPlugin plugin;
    private FileConfiguration config;

    private int maxMembers;
    private int maxNameLength;
    private int maxTagLength;
    private Pattern namePattern;
    private Pattern tagPattern;
    private List<String> bannedWords;
    private boolean caseSensitive;
    private boolean asyncOperations;
    private int cacheCleanupDelay;
    private String messagePrefix;
    private boolean debugMode;
    private boolean allowEditing;

    /**
     * Constructor for ConfigManager
     * 
     * @param plugin The main plugin instance
     */
    public ConfigManager(ParadiseTeamChatPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Load (or reload) the configuration file.
     * This saves the default config if it doesn't exist, then loads it.
     */
    public void loadConfig() {
        // Save default config.yml from resources if it doesn't exist.
        plugin.saveDefaultConfig();

        // Reload config from Disk
        plugin.reloadConfig();
        config = plugin.getConfig();

        // Load and cache values
        loadValues();

        // Validate loaded values.
        validateConfig();
    }

    /**
     * Reload the configuration file.
     */
    public void reloadConfig() {
        loadConfig();
    }

    /**
     * Load and cache configuration values for easy access.
     */
    private void loadValues() {
        // Team settings
        maxMembers = config.getInt("team.max-members", 10);
        maxNameLength = config.getInt("team.max-name-length", 16);
        maxTagLength = config.getInt("team.max-tag-length", 6);
        allowEditing = config.getBoolean("team.allow-editing", true);

        // Load regex patterns (with error handling)
        String nameRegex = config.getString("team.name-regex", "^[A-Za-z0-9]+$");
        String tagRegex = config.getString("team.tag-regex", "^[A-Za-z0-9]+$");

        try {
            namePattern = Pattern.compile(nameRegex);
        } catch (PatternSyntaxException e) {
            plugin.getLogger().warning("Invalid name-regex in config, using default");
            namePattern = Pattern.compile("^[A-Za-z0-9]+$");
        }

        try {
            tagPattern = Pattern.compile(tagRegex);
        } catch (PatternSyntaxException e) {
            plugin.getLogger().warning("Invalid tag-regex in config, using default");
            tagPattern = Pattern.compile("^[A-Za-z0-9]+$");
        }

        // Blacklist settings
        bannedWords = config.getStringList("blacklist.banned-words");
        caseSensitive = config.getBoolean("blacklist.case-sensitive", false);

        // Performance settings
        asyncOperations = config.getBoolean("performance.async-operations", true);
        cacheCleanupDelay = config.getInt("performance.cache-cleanup-delay", 60);

        // Message prefix
        messagePrefix = config.getString("messages.prefix", "&8[&bTeamChat&8]&r ");

        // Debug mode
        debugMode = config.getBoolean("debug-mode", false);
    }

    /**
     * Validate configuration values to ensure they're sensible
     */
    private void validateConfig() {
        if (maxMembers < 2) {
            plugin.getLogger().warning("max-members is too low (must be at least 2), using default: 10");
            maxMembers = 10;
        }

        if (maxNameLength < 3) {
            plugin.getLogger().warning("max-name-length is too low (must be at least 3), using default: 16");
            maxNameLength = 16;
        }

        if (maxTagLength < 2) {
            plugin.getLogger().warning("max-tag-length is too low (must be at least 2), using default: 6");
            maxTagLength = 6;
        }

        if (cacheCleanupDelay < 10) {
            plugin.getLogger().warning("cache-cleanup-delay is too low (must be at least 10), using default: 60");
            cacheCleanupDelay = 60;
        }
    }

    public int getMaxMembers() {
        return maxMembers;
    }

    public int getMaxNameLength() {
        return maxNameLength;
    }

    public int getMaxTagLength() {
        return maxTagLength;
    }

    public Pattern getNamePattern() {
        return namePattern;
    }

    public Pattern getTagPattern() {
        return tagPattern;
    }

    public List<String> getBannedWords() {
        return bannedWords;
    }

    public boolean isCaseSensitive() {
        return caseSensitive;
    }

    public boolean isAsyncOperations() {
        return asyncOperations;
    }

    public int getCacheCleanupDelay() {
        return cacheCleanupDelay;
    }

    public boolean isDebugMode() {
        return debugMode;
    }

    public boolean isEditingAllowed() {
        return allowEditing;
    }

    /**
     * Convert colour codes (&a, &b, etc.) to Minecraft format (§a, §b, etc.)
     * 
     * @param message The message to colourise
     * @return The colourised message
     */
    private String colourise(String message) {
        return message.replaceAll("&", "§");
    }

    /**
     * Get a message from the config with the prefix added and colourised.
     * 
     * @param path The path to the message in config.yml
     * @return The formatted message
     */
    public String getMessage(String path) {
        String message = config.getString("messages." + path, "Message not found: " + path);
        return colourise(messagePrefix + message);
    }

    /**
     * Get a message from the config with a placeholder replaced, prefix added, and
     * colourised.
     * 
     * @param path        The path to the message in config.yml
     * @param placeholder The placeholder to replace
     * @param value       The value to replace the placeholder with
     * @return The formatted message
     */
    public String getMessage(String path, String placeholder, String value) {
        String message = getMessage(path);
        return message.replace(placeholder, value);
    }

    /**
     * Get the raw FileConfiguration object
     * 
     * @return The config object
     */
    public FileConfiguration getConfig() {
        return config;
    }
}
