package com.patelheet.paradiseteamchat.integrations;

import com.patelheet.paradiseteamchat.ParadiseTeamChatPlugin;
import com.patelheet.paradiseteamchat.models.RoleDefinition;
import com.patelheet.paradiseteamchat.models.Team;
import xyz.jpenilla.squaremap.api.Squaremap;
import xyz.jpenilla.squaremap.api.SquaremapProvider;

import java.util.*;

public class SquaremapIntegration {
    private final ParadiseTeamChatPlugin plugin;
    private boolean registered = false;

    public SquaremapIntegration(ParadiseTeamChatPlugin plugin) {
        this.plugin = plugin;
    }

    public void register() {
        try {
            Squaremap api = SquaremapProvider.get();

            api.playerManager().registerDataProvider("paradiseteamchat", (uuid, playerName) -> {
                return getPlayerTeamData(playerName);
            });

            registered = true;
            plugin.getLogger().info("Squaremap integration enabled - team info will appear on web map!");

        } catch (Exception e) {
            plugin.getLogger().warning("Failed to enable Squaremap integration: " + e.getMessage());
            plugin.logDebug("Squaremap integration error: " + e);
        }
    }

    private Map<String, Object> getPlayerTeamData(String playerName) {
        try {
            String lowerName = playerName.toLowerCase();

            Team team = plugin.getCacheManager().getPlayerTeam(lowerName);

            if (team == null) {
                return null;
            }

            Map<String, Object> data = new HashMap<>();

            data.put("team", team.getName());
            data.put("tag", team.getTag());

            String roleId = team.getMemberRole(lowerName);
            RoleDefinition role = plugin.getRoleManager().getRole(roleId);

            if (role != null) {
                data.put("role", role.getDisplayName());
            }

            data.put("members", team.getMemberCount() + "/" + team.getMemberLimit());

            Map<String, List<String>> membersByRole = groupMembersByRole(team);
            data.put("teammates", membersByRole);

            return data;
        } catch (Exception e) {
            plugin.logDebug("Error fetching team data for Squaremap: " + e.getMessage());
            return null;
        }
    }

    private Map<String, List<String>> groupMembersByRole(Team team) {
        // Use LinkedHashMap to preserve order (owner first, then other roles)
        Map<String, List<String>> grouped = new LinkedHashMap<>();

        // Get all members and their roles
        for (String memberName : team.getMembers()) {
            String roleId = team.getMemberRole(memberName);
            RoleDefinition role = plugin.getRoleManager().getRole(roleId);

            String roleDisplayName = role != null ? role.getDisplayName() : "Member";

            // Capitalize first letter of player name for display
            String displayName = capitaliseFirst(memberName);

            // Add to the appropriate role group
            grouped.computeIfAbsent(roleDisplayName, k -> new ArrayList<>()).add(displayName);
        }

        return grouped;
    }

    private String capitaliseFirst(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }

    public void unregister() {
        if (!registered) {
            return;
        }

        try {
            Squaremap api = SquaremapProvider.get();
            api.playerManager().unregisterDataProvider("paradiseteamchat");
            plugin.logDebug("Squaremap integration unregistered");
        } catch (Exception e) {
            // Squaremap might have unloaded first, this is fine
            plugin.logDebug("Could not unregister Squaremap integration: " + e.getMessage());
        }
    }

    public boolean isRegistered() {
        return registered;
    }

}
