package com.patelheet.paradiseteamchat.commands;

import com.patelheet.paradiseteamchat.ParadiseTeamChatPlugin;
import com.patelheet.paradiseteamchat.models.RoleDefinition;
import com.patelheet.paradiseteamchat.models.Team;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * Command to promote a team member to a specified role.
 * Requires 'promote' permission.
 */
public class TeamPromoteCommand extends BaseCommand implements TabCompleter {

    /**
     * Constructor for TeamPromoteCommand.
     * 
     * @param plugin The main plugin instance.
     */
    public TeamPromoteCommand(ParadiseTeamChatPlugin plugin) {
        super(plugin);
    }

    @Override
    public String getPermission() {
        return "teamchat.team.promote";
    }

    @Override
    public String getUsage() {
        return "/team promote <player> <role>";
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        Player player = validatePlayer(sender);
        if (player == null) {
            return true;
        }

        if (!checkPermission(sender)) {
            return true;
        }

        // Check if roles are enabled
        if (!plugin.getRoleManager().isRolesEnabled()) {
            player.sendMessage(configManager.getMessage("roles-disabled"));
            return true;
        }

        if (args.length < 2) {
            sendUsage(sender);
            return true;
        }

        String targetName = args[0];
        String roleId = args[1].toLowerCase();
        String playerName = player.getName().toLowerCase();
        String targetLower = targetName.toLowerCase();

        // Get player's team
        Team team = plugin.getCacheManager().getPlayerTeam(playerName);
        if (team == null) {
            player.sendMessage(configManager.getMessage("not-in-team"));
            return true;
        }

        // Check promote permission
        String senderRoleId = team.getMemberRole(playerName);
        if (!plugin.getRoleManager().hasPermission(senderRoleId, "promote")) {
            player.sendMessage(configManager.getMessage("no-promote-permission"));
            return true;
        }

        // Validate target is in team
        if (!team.isMember(targetLower)) {
            player.sendMessage(configManager.getMessage("prefix") + "§c" + targetName + " is not in your team.");
            return true;
        }

        // Cannot promote owner
        if (team.isOwner(targetLower)) {
            player.sendMessage(configManager.getMessage("cannot-promote-owner"));
            return true;
        }

        // Cannot promote to owner
        if (roleId.equals("owner")) {
            player.sendMessage(configManager.getMessage("cannot-promote-to-owner"));
            return true;
        }

        // Validate role exists
        if (!plugin.getRoleManager().isValidRoleForAssignment(roleId)) {
            player.sendMessage(configManager.getMessage("role-not-found", "{role}", roleId));
            return true;
        }

        // Check if already has role
        String currentRole = team.getMemberRole(targetLower);
        if (currentRole.equals(roleId)) {
            player.sendMessage(configManager.getMessage("already-has-role", "{player}", targetName));
            return true;
        }

        // Promote the member
        int teamId = team.getId();
        RoleDefinition newRole = plugin.getRoleManager().getRole(roleId);

        // Async DB update
        plugin.getAsyncTaskManager().executeAsync(() -> {
            return plugin.getTeamRepository().setMemberRole(teamId, targetLower, roleId);
        }).thenAccept(success -> {
            // Update cache and notify on main thread
            plugin.getAsyncTaskManager().runSync(() -> {
                if (success) {
                    // Update cache
                    team.setMemberRole(targetLower, roleId);
                    plugin.getCacheManager().updateTeamCache(team);

                    // Notify sender
                    player.sendMessage(configManager.getMessage("role-promoted", "{player}", targetName)
                            .replace("{role}", newRole.getFormattedDisplayName()));

                    // Notify promoted player
                    Player targetPlayer = Bukkit.getPlayerExact(targetName);
                    if (targetPlayer != null && targetPlayer.isOnline()) {
                        targetPlayer.sendMessage("§aYou have been promoted to " +
                                newRole.getFormattedDisplayName() + "§a!");
                    }

                    // Notify team
                    String notification = "§e" + targetName + "§a has been promoted to " +
                            newRole.getFormattedDisplayName() + "§a!";
                    for (String memberName : team.getMembers()) {
                        if (!memberName.equals(playerName) && !memberName.equals(targetLower)) {
                            Player member = Bukkit.getPlayerExact(memberName);
                            if (member != null && member.isOnline()) {
                                member.sendMessage(notification);
                            }
                        }
                    }

                    plugin.logDebug(playerName + " promoted " + targetLower + " to " + roleId);
                } else {
                    player.sendMessage("§cFailed to promote player. Please try again.");
                }
            });
        }).exceptionally(throwable -> {
            plugin.getLogger().severe("Error promoting player: " + throwable.getMessage());
            throwable.printStackTrace();
            player.sendMessage("§cAn error occurred while promoting the player.");
            return null;
        });

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, org.bukkit.command.Command command, String alias,
            String[] args) {
        List<String> completions = new ArrayList<>();

        if (!plugin.getRoleManager().isRolesEnabled()) {
            return completions;
        }

        if (!(sender instanceof Player)) {
            return completions;
        }

        Player player = (Player) sender;
        String playerName = player.getName().toLowerCase();
        Team team = plugin.getCacheManager().getPlayerTeam(playerName);

        if (team == null) {
            return completions;
        }

        // Check if player has promote permission
        String roleId = team.getMemberRole(playerName);
        if (!plugin.getRoleManager().hasPermission(roleId, "promote")) {
            return completions;
        }

        if (args.length == 1) {
            // Suggest team members (excluding owner and self)
            String input = args[0].toLowerCase();
            for (String memberName : team.getMembers()) {
                if (!memberName.equals(playerName) && !team.isOwner(memberName)
                        && memberName.startsWith(input)) {
                    completions.add(memberName);
                }
            }
        } else if (args.length == 2) {
            // Suggest available roles (excluding owner)
            String input = args[1].toLowerCase();
            for (String availableRole : plugin.getRoleManager().getAllRoleIds()) {
                if (!availableRole.equals("owner") && availableRole.startsWith(input)) {
                    RoleDefinition role = plugin.getRoleManager().getRole(availableRole);
                    if (role != null) {
                        completions.add(availableRole);
                    }
                }
            }
        }

        return completions;
    }
}
