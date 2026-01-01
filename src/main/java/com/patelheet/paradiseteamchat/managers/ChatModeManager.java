package com.patelheet.paradiseteamchat.managers;

import com.patelheet.paradiseteamchat.ParadiseTeamChatPlugin;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manager to handle player chat modes (GLOBAL or TEAM).
 * Tracks each player's current chat mode and provides methods to get, set,
 * toggle, and remove chat modes.
 */
public class ChatModeManager {

    private final ParadiseTeamChatPlugin plugin;

    // Map to track each player's chat mode
    private final Map<String, ChatMode> playerChatModes;

    /**
     * Enum representing the two possible chat modes.
     */
    public enum ChatMode {
        GLOBAL, // Normal server-wide chat
        TEAM // Private team chat
    }

    /**
     * Constructor for ChatModeManager.
     * 
     * @param plugin The main plugin instance.
     */
    public ChatModeManager(ParadiseTeamChatPlugin plugin) {
        this.plugin = plugin;
        this.playerChatModes = new ConcurrentHashMap<>();
    }

    /**
     * Initialises the ChatModeManager. Placeholder for future setup logic.
     */
    public void initialise() {
        plugin.getLogger().info("ChatModeManager initialized successfully.");
    }

    /**
     * Sets the chat mode for a player.
     * 
     * @param playerName The name of the player.
     * @param mode       The chat mode to set.
     */
    public void setChatMode(String playerName, ChatMode mode) {
        String lowerName = playerName.toLowerCase();
        playerChatModes.put(lowerName, mode);

        plugin.getLogger().info("Player " + playerName + " chat mode set to " + mode);
    }

    /**
     * Gets the chat mode for a player.
     * 
     * @param playerName The name of the player.
     * @return The current chat mode of the player.
     */
    public ChatMode getChatMode(String playerName) {
        String lowerName = playerName.toLowerCase();
        return playerChatModes.getOrDefault(lowerName, ChatMode.GLOBAL);
    }

    /**
     * Toggles the chat mode for a player.
     * 
     * @param playerName The name of the player.
     * @return The new chat mode after toggling.
     */
    public ChatMode toggleChatMode(String playerName) {
        ChatMode currentMode = getChatMode(playerName);
        ChatMode newMode = (currentMode == ChatMode.TEAM) ? ChatMode.GLOBAL : ChatMode.TEAM;
        setChatMode(playerName, newMode);
        return newMode;
    }

    /**
     * Checks if a player is currently in TEAM chat mode.
     * 
     * @param playerName The name of the player.
     * @return True if the player is in TEAM mode, false otherwise.
     */
    public boolean isInTeamMode(String playerName) {
        return getChatMode(playerName) == ChatMode.TEAM;
    }

    /**
     * Removes a player's chat mode tracking.
     * 
     * @param playerName The name of the player.
     */
    public void removePlayer(String playerName) {
        String lowerName = playerName.toLowerCase();
        ChatMode removedMode = playerChatModes.remove(lowerName);

        if (removedMode != null) {
            plugin.getLogger().info("Removed chat mode for player " + playerName);
        }
    }

    /**
     * Forces a player into GLOBAL chat mode.
     * Happens if a player's team is disbanded or they leave their team or are
     * kicked.
     * 
     * @param playerName The name of the player.
     */
    public void forceGlobalMode(String playerName) {
        setChatMode(playerName, ChatMode.GLOBAL);
        plugin.getLogger().info("Forced player " + playerName + " to GLOBAL chat mode");
    }

    /**
     * Gets the count of players being tracked for chat modes.
     * 
     * @return The number of players with tracked chat modes.
     */
    public int getTrackedPlayerCount() {
        return playerChatModes.size();
    }

    /**
     * Clears all chat mode data. Used during plugin shutdown.
     */
    public void clearAll() {
        playerChatModes.clear();
        plugin.getLogger().info("All chat modes cleared.");
    }

}
