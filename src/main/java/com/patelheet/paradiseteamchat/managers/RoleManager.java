package com.patelheet.paradiseteamchat.managers;

import com.patelheet.paradiseteamchat.models.RoleDefinition;
import com.patelheet.paradiseteamchat.models.RolePermissions;
import com.patelheet.paradiseteamchat.ParadiseTeamChatPlugin;
import org.bukkit.configuration.ConfigurationSection;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages the roles within the Paradise Team Chat plugin.
 * Responsible for loading, storing, and providing access to role definitions
 * and their permissions.
 */
public class RoleManager {
    private final ParadiseTeamChatPlugin plugin;
    private final Map<String, RoleDefinition> roles;
    private RoleDefinition ownerRole;
    private RoleDefinition defaultMemberRole;
    private boolean rolesEnabled;

    /**
     * Constructor to initialise the RoleManager.
     * 
     * @param plugin The main plugin instance.
     */
    public RoleManager(ParadiseTeamChatPlugin plugin) {
        this.plugin = plugin;
        this.roles = new ConcurrentHashMap<>();
    }

    /**
     * Initialises the RoleManager by loading roles from the configuration.
     */
    public void initialise() {
        loadRoles();
        plugin.getLogger().info("Role manager initialised with " + roles.size() + " roles.");
    }

    /**
     * Loads roles from the plugin configuration.
     */
    public void loadRoles() {
        roles.clear();

        ConfigurationSection rolesSection = plugin.getConfig().getConfigurationSection("roles");

        if (rolesSection == null) {
            plugin.getLogger().warning("No 'roles' section found in config.yml - using defaults");
            createDefaultRoles();
            rolesEnabled = false;
            return;
        }

        rolesEnabled = rolesSection.getBoolean("enabled", true);

        if (!rolesEnabled) {
            plugin.getLogger().info("Roles are disabled in config.yml - using defaults");
            createDefaultRoles();
            return;
        }

        loadOwnerRole(rolesSection);

        loadSubRoles(rolesSection);

        if (!roles.containsKey("member")) {
            plugin.getLogger().warning("No 'member' role defined - creating default");
            defaultMemberRole = RoleDefinition.createDefaultMemberRole();
            roles.put("member", defaultMemberRole);
        } else {
            defaultMemberRole = roles.get("member");
        }

        plugin.logDebug("Loaded roles: " + String.join(", ", roles.keySet()));
    }

    /**
     * Loads the owner role from the configuration.
     * 
     * @param rolesSection The configuration section containing role definitions.
     */
    private void loadOwnerRole(ConfigurationSection rolesSection) {
        ConfigurationSection ownerSection = rolesSection.getConfigurationSection("owner");

        String displayName = "Owner";
        String colour = "&6";

        if (ownerSection != null) {
            displayName = ownerSection.getString("display-name", displayName);
            colour = ownerSection.getString("colour", colour);
        }

        ownerRole = RoleDefinition.createOwnerRole(displayName, colour);
        roles.put("owner", ownerRole);

        plugin.logDebug("Loaded owner role: " + displayName + " (" + colour + ")");
    }

    /**
     * Loads sub-roles from the configuration.
     * 
     * @param rolesSection The configuration section containing role definitions.
     */
    private void loadSubRoles(ConfigurationSection rolesSection) {
        ConfigurationSection subRolesSection = rolesSection.getConfigurationSection("sub-roles");

        if (subRolesSection == null) {
            plugin.getLogger().warning("No 'sub-roles' section found - only owner role available");
            return;
        }

        Set<String> roleIds = subRolesSection.getKeys(false);

        for (String roleId : roleIds) {
            try {
                RoleDefinition role = loadRoleDefinition(roleId, subRolesSection);
                if (role != null) {
                    roles.put(roleId.toLowerCase(), role);
                    plugin.logDebug("Loaded sub-role: " + roleId + " - " + role.getDisplayName());
                }
            } catch (Exception e) {
                plugin.getLogger().severe("Failed to load role '" + roleId + "': " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    /**
     * Loads a single role definition from the configuration.
     * 
     * @param roleId          The ID of the role to load.
     * @param subRolesSection The configuration section containing sub-role
     *                        definitions.
     * @return The loaded RoleDefinition, or null if loading failed.
     */
    private RoleDefinition loadRoleDefinition(String roleId, ConfigurationSection subRolesSection) {
        ConfigurationSection roleSection = subRolesSection.getConfigurationSection(roleId);

        if (roleSection == null) {
            plugin.getLogger().warning("Role section for '" + roleId + "' is null");
            return null;
        }

        String displayName = roleSection.getString("display-name");
        String color = roleSection.getString("color");

        if (displayName == null || color == null) {
            plugin.getLogger().warning("Role '" + roleId + "' missing display-name or color - skipping");
            return null;
        }

        RolePermissions permissions = loadRolePermissions(roleId, roleSection);

        return new RoleDefinition(roleId, displayName, color, permissions, false);
    }

    /**
     * Loads role permissions from the configuration.
     * 
     * @param roleId      The ID of the role.
     * @param roleSection The configuration section containing role details.
     * @return The loaded RolePermissions.
     */
    private RolePermissions loadRolePermissions(String roleId, ConfigurationSection roleSection) {
        ConfigurationSection permsSection = roleSection.getConfigurationSection("permissions");

        if (permsSection == null) {
            plugin.getLogger().warning("No permissions section for role '" + roleId + "' - using defaults (all false)");
            return RolePermissions.defaultPermissions();
        }

        boolean canInvite = permsSection.getBoolean("invite", false);
        boolean canKick = permsSection.getBoolean("kick", false);
        boolean canEditTeam = permsSection.getBoolean("edit-team", false);
        boolean canPromote = permsSection.getBoolean("promote", false);
        boolean canDemote = permsSection.getBoolean("demote", false);

        return new RolePermissions(canInvite, canKick, canEditTeam, canPromote, canDemote);
    }

    /**
     * Creates default roles (owner and member) when roles are disabled or not
     * defined.
     */
    private void createDefaultRoles() {
        ownerRole = RoleDefinition.createOwnerRole("Owner", "&6");
        defaultMemberRole = RoleDefinition.createDefaultMemberRole();

        roles.put("owner", ownerRole);
        roles.put("member", defaultMemberRole);

        plugin.logDebug("Created default roles: owner, member");
    }

    /**
     * Retrieves a RoleDefinition by its ID.
     * 
     * @param roleId The ID of the role to retrieve.
     * @return The RoleDefinition corresponding to the given ID, or null if not
     *         found.
     */
    public RoleDefinition getRole(String roleId) {
        if (roleId == null) {
            return defaultMemberRole;
        }
        return roles.get(roleId.toLowerCase());
    }

    /**
     * Gets the owner role definition.
     * 
     * @return The RoleDefinition representing the owner role.
     */
    public RoleDefinition getOwnerRole() {
        return ownerRole;
    }

    /**
     * Gets the default member role definition.
     * 
     * @return The RoleDefinition representing the default member role.
     */
    public RoleDefinition getDefaultMemberRole() {
        return defaultMemberRole;
    }

    /**
     * Gets all role IDs.
     * 
     * @return A set of all role IDs.
     */
    public Set<String> getAllRoleIds() {
        return new HashSet<>(roles.keySet());
    }

    /**
     * Gets all sub-roles (non-owner roles).
     * 
     * @return A list of RoleDefinition objects representing sub-roles.
     */
    public List<RoleDefinition> getSubRoles() {
        List<RoleDefinition> subRoles = new ArrayList<>();
        for (RoleDefinition role : roles.values()) {
            if (!role.isOwnerRole()) {
                subRoles.add(role);
            }
        }
        return subRoles;
    }

    /**
     * Checks if a role with the given ID exists.
     * 
     * @param roleId The ID of the role to check.
     * @return True if the role exists, false otherwise.
     */
    public boolean roleExists(String roleId) {
        if (roleId == null) {
            return false;
        }
        return roles.containsKey(roleId.toLowerCase());
    }

    /**
     * Checks if a role has a specific permission.
     * 
     * @param roleId     The ID of the role.
     * @param permission The permission to check.
     * @return True if the role has the permission, false otherwise.
     */
    public boolean hasPermission(String roleId, String permission) {
        RoleDefinition role = getRole(roleId);
        if (role == null) {
            return false;
        }
        return role.hasPermission(permission);
    }

    /**
     * Validates if a role ID is valid for assignment to members.
     * 
     * @param roleId The role ID to validate.
     * @return True if the role ID is valid for assignment, false otherwise.
     */
    public boolean isValidRoleForAssignment(String roleId) {
        if (roleId == null || roleId.isEmpty()) {
            return false;
        }

        // Cannot assign owner role manually
        if (roleId.equalsIgnoreCase("owner")) {
            return false;
        }

        return roleExists(roleId);
    }

    /**
     * Checks if roles are enabled in the configuration.
     * 
     * @return true if roles are enabled, false otherwise.
     */
    public boolean isRolesEnabled() {
        return rolesEnabled;
    }

    /**
     * Gets the total count of roles managed.
     * 
     * @return The number of roles.
     */
    public int getRoleCount() {
        return roles.size();
    }

    /**
     * Reloads roles from the configuration.
     */
    public void reload() {
        loadRoles();
        plugin.getLogger().info("Roles reloaded: " + roles.size() + " roles available.");
    }

}
