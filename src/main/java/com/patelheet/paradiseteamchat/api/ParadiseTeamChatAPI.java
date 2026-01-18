package com.patelheet.paradiseteamchat.api;

/**
 * Main entry point for the ParadiseTeamChat public API.
 * 
 * <p>
 * External plugins can obtain an instance of this API through Bukkit's Services
 * Manaager:
 * 
 * <pre>{@code
 * RegisteredServiceProvider<ParadiseTeamChatAPI> provider = Bukkit.getServicesManager()
 *         .getRegistration(ParadiseTeamChatAPI.class);
 * 
 * if (provider != null) {
 *     ParadiseTeamChatAPI api = provider.getProvider();
 * }
 * }
 * </pre>
 * </p>
 * <b>Thread safety:</b> All service interfaces returned by this API are
 * thread-safe unless otherwise noted.
 *
 * Cache lookup methods are synchronous, while database operations return
 * CompletableFuture.
 * 
 * @author Heet Patel (patelheet30)
 * @version 1.0.0
 * @since 3.0.0
 */
public interface ParadiseTeamChatAPI {
    /**
     * Gets the API version following semantic versioning (MAJOR.MINOR.PATCH).
     * 
     * @return the API version (e.g., "1.0.0")
     */
    String getAPIVersion();

    /**
     * Gets the public version of the ParadiseTeamChat plugin.
     * 
     * @return the public version of the plugin (e.g., "3.0.0")
     */
    String getPublicVersion();

    /**
     * Checks if this API version is compatible with the requested minimum version.
     * 
     * <p>
     * Compatibility follows semantic versioning rules:
     * <ul>
     * <li>MAJOR version must be equal.</li>
     * <li>MINOR version must be greater than or equal.</li>
     * <li>PATCH version is ignored for compatibility checks.</li>
     * </ul>
     * </p>
     * Example: API version 1.5.2 is compatible with requested version 1.3.0, but
     * not
     * with 2.0.0 or 1.6.0.
     * 
     * @param minimumVersion the minimum required version (e.g., "1.3.0")
     * @return true if compatible, false otherwise
     * @throws IllegalArgumentException if the version format is invalid
     */
    boolean isCompatible(String minimumVersion);

    /**
     * Gets the service for team management operations.
     * 
     * <p>
     * This service provides methods for:
     * <ul>
     * <li>Querying teams by ID, name, or player</li>
     * <li>Creating and deleting teams</li>
     * <li>Checking name/tag availability</li>
     * </ul>
     * 
     * @return The TeamService instance (never null)
     */
    TeamService getTeamService();

    /**
     * Gets the service for team member operations.
     * 
     * <p>
     * This service provides methods for:
     * <ul>
     * <li>Adding and removing members</li>
     * <li>Managing member roles</li>
     * <li>Transferring team ownership</li>
     * </ul>
     * 
     * @return The MemberService instance (never null)
     */
    MemberService getMemberService();

    /**
     * Gets the service for role queries and permissions.
     * 
     * <p>
     * This service provides methods for:
     * <ul>
     * <li>Querying role definitions</li>
     * <li>Checking role permissions</li>
     * <li>Getting available roles</li>
     * </ul>
     * 
     * @return The RoleService instance (never null)
     */
    RoleService getRoleService();

    /**
     * Gets the service for team block operations.
     * 
     * <p>
     * This service provides methods for:
     * <ul>
     * <li>Registering and removing team blocks</li>
     * <li>Querying team block ownership</li>
     * <li>Checking effect permissions</li>
     * </ul>
     * 
     * @return The BlockService instance (never null)
     */
    BlockService getBlockService();

    /**
     * Gets the service for team invitation management.
     * 
     * <p>
     * This service provides methods for:
     * <ul>
     * <li>Sending and accepting invites</li>
     * <li>Checking pending invites</li>
     * <li>Clearing invites</li>
     * </ul>
     * 
     * @return The InviteService instance (never null)
     */
    InviteService getInviteService();

    /**
     * Gets the service for chat mode management.
     * 
     * <p>
     * This service provides methods for:
     * <ul>
     * <li>Querying player chat modes</li>
     * <li>Switching between TEAM and GLOBAL modes</li>
     * <li>Managing chat mode state</li>
     * </ul>
     * 
     * @return The ChatService instance (never null)
     */
    ChatService getChatService();
}
