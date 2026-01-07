package com.patelheet.paradiseteamchat.models;

/**
 * Represents a role definition within a team, including its ID, display name,
 * colour, permissions, and whether it is the owner role.
 */
public class RoleDefinition {
    private final String roleId;
    private final String displayName;
    private final String colour;
    private final RolePermissions permissions;
    private final boolean isOwnerRole;

    /**
     * Constructor to initialise a role definition.
     * 
     * @param roleId      The unique identifier for the role.
     * @param displayName The display name of the role.
     * @param colour      The colour code associated with the role.
     * @param permissions The permissions assigned to the role.
     * @param isOwnerRole Indicates if this role is the owner role.
     */
    public RoleDefinition(
            String roleId,
            String displayName,
            String colour,
            RolePermissions permissions,
            boolean isOwnerRole) {
        this.roleId = roleId.toLowerCase();
        this.displayName = displayName;
        this.colour = colour;
        this.permissions = permissions;
        this.isOwnerRole = isOwnerRole;
    }

    /**
     * Creates a RoleDefinition instance for the owner role.
     * 
     * @param displayName The display name of the owner role.
     * @param colour      The colour code associated with the owner role.
     * @return A new RoleDefinition instance representing the owner role.
     */
    public static RoleDefinition createOwnerRole(String displayName, String colour) {
        return new RoleDefinition(
                "owner",
                displayName,
                colour,
                RolePermissions.ownerPermissions(),
                true);
    }

    /**
     * Creates a RoleDefinition instance for the default member role.
     * 
     * @return A new RoleDefinition instance representing the default member role.
     */
    public static RoleDefinition createDefaultMemberRole() {
        return new RoleDefinition(
                "member",
                "Member",
                "&7",
                RolePermissions.defaultPermissions(),
                false);
    }

    /**
     * Gets the formatted display name with colour codes applied.
     * 
     * @return The formatted display name [eg: "§6Owner"]
     */
    public String getFormattedDisplayName() {
        return colour.replace('&', '§') + displayName;
    }

    /**
     * Gets the formatted display name enclosed in brackets with colour codes
     * applied.
     * 
     * @return The formatted display name in brackets [eg: "§6[Owner]"]
     */
    public String getFormattedRoleBrackets() {
        return colour.replace('&', '§') + "[" + displayName + "]";
    }

    /**
     * Checks if the role has a specific permission.
     * 
     * @param permission the permission to check
     * @return true if the role has the permission, false otherwise
     */
    public boolean hasPermission(String permission) {
        return permissions.hasPermission(permission);
    }

    public String getRoleId() {
        return roleId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getColour() {
        return colour;
    }

    public RolePermissions getPermissions() {
        return permissions;
    }

    public boolean isOwnerRole() {
        return isOwnerRole;
    }

    @Override
    public String toString() {
        return "RoleDefinition{" +
                "roleId='" + roleId + '\'' +
                ", displayName='" + displayName + '\'' +
                ", colour='" + colour + '\'' +
                ", isOwner=" + isOwnerRole +
                ", permissions=" + permissions +
                '}';
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null || getClass() != obj.getClass())
            return false;

        RoleDefinition that = (RoleDefinition) obj;
        return roleId.equals(that.roleId);
    }

    @Override
    public int hashCode() {
        return roleId.hashCode();
    }
}
