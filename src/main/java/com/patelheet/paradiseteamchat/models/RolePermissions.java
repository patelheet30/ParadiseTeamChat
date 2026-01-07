package com.patelheet.paradiseteamchat.models;

/**
 * Represents the permissions associated with a specific role within a team.
 * Each permission determines what actions a user with this role can perform.
 */
public class RolePermissions {
    private final boolean canInvite;
    private final boolean canKick;
    private final boolean canEditTeam;
    private final boolean canPromote;
    private final boolean canDemote;

    /**
     * Constructor to initialise role permissions.
     * 
     * @param canInvite   whether the role can invite members
     * @param canKick     whether the role can kick members
     * @param canEditTeam whether the role can edit team settings
     * @param canPromote  whether the role can promote members
     * @param canDemote   whether the role can demote members
     */
    public RolePermissions(
            boolean canInvite,
            boolean canKick,
            boolean canEditTeam,
            boolean canPromote,
            boolean canDemote) {
        this.canInvite = canInvite;
        this.canKick = canKick;
        this.canEditTeam = canEditTeam;
        this.canPromote = canPromote;
        this.canDemote = canDemote;
    }

    /**
     * Creates a RolePermissions instance with all permissions granted.
     * 
     * @return RolePermissions with all permissions set to true
     */
    public static RolePermissions ownerPermissions() {
        return new RolePermissions(true, true, true, true, true);
    }

    /**
     * Creates a RolePermissions instance with no permissions granted.
     * 
     * @return RolePermissions with all permissions set to false
     */
    public static RolePermissions defaultPermissions() {
        return new RolePermissions(false, false, false, false, false);
    }

    public boolean hasPermission(String permission) {
        return switch (permission) {
            case "invite" -> canInvite;
            case "kick" -> canKick;
            case "edit_team" -> canEditTeam;
            case "promote" -> canPromote;
            case "demote" -> canDemote;
            default -> false;
        };
    }

    public boolean canInvite() {
        return canInvite;
    }

    public boolean canKick() {
        return canKick;
    }

    public boolean canEditTeam() {
        return canEditTeam;
    }

    public boolean canPromote() {
        return canPromote;
    }

    public boolean canDemote() {
        return canDemote;
    }

    @Override
    public String toString() {
        return "RolePermissions{" +
                "invite=" + canInvite +
                ", kick=" + canKick +
                ", editTeam=" + canEditTeam +
                ", promote=" + canPromote +
                ", demote=" + canDemote +
                '}';
    }
}
