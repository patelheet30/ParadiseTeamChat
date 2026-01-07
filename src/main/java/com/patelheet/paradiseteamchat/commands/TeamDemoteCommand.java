package com.patelheet.paradiseteamchat.commands;

import com.patelheet.paradiseteamchat.ParadiseTeamChatPlugin;
import com.patelheet.paradiseteamchat.models.Team;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * Command to demote a team member back to the default "member" role.
 * Requires 'demote' permission.
 */
public class TeamDemoteCommand extends BaseCommand implements TabCompleter {

    /**
     * Constructor for TeamDemoteCommand.
     * 
     * @param plugin The main plugin instance.
     */
    public TeamDemoteCommand(ParadiseTeamChatPlugin plugin) {
        super(plugin);
    }

    @Override
    public String getPermission() {
        return "teamchat.team.demote";
    }

    @Override
    public String getUsage() {
        return "/team demote <player>";
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

        if (args.length < 1) {
            sendUsage(sender);
            return true;
        }

        String targetName = args[0];
        String playerName = player.getName().toLowerCase();
        String targetLower = targetName.toLowerCase();

        // Get player's team
        Team team = plugin.getCacheManager().getPlayerTeam(playerName);
        if (team == null) {
            player.sendMessage(configManager.getMessage("not-in-team"));
            return true;
        }

        // Check demote permission
        String senderRoleId = team.getMemberRole(playerName);
        if (!plugin.getRoleManager().hasPermission(senderRoleId, "demote")) {
            player.sendMessage(configManager.getMessage("no-demote-permission"));
            return true;
        }

        // Validate target is in team
        if (!team.isMember(targetLower)) {
            player.sendMessage(configManager.getMessage("prefix") + "§c" + targetName + " is not in your team.");
            return true;
        }

        // Cannot demote owner
        if (team.isOwner(targetLower)) {
            player.sendMessage(configManager.getMessage("cannot-demote-owner"));
            return true;
        }

        // Check if already member
        String currentRole = team.getMemberRole(targetLower);
        if (currentRole.equals("member")) {
            player.sendMessage("§e" + targetName + " §cis already a Member.");
            return true;
        }

        // Demote the member
        int teamId = team.getId();

        plugin.getAsyncTaskManager().executeAsync(() -> {
            return plugin.getTeamRepository().setMemberRole(teamId, targetLower, "member");
        }).thenAccept(success -> {
            plugin.getAsyncTaskManager().runSync(() -> {
                if (success) {
                    // Update cache
                    team.setMemberRole(targetLower, "member");
                    plugin.getCacheManager().updateTeamCache(team);

                    // Notify sender
                    player.sendMessage(configManager.getMessage("role-demoted", "{player}", targetName));

                    // Notify demoted player
                    Player targetPlayer = Bukkit.getPlayerExact(targetName);
                    if (targetPlayer != null && targetPlayer.isOnline()) {
                        targetPlayer.sendMessage("§cYou have been demoted to §7Member§c.");
                    }

                    // Notify team
                    String notification = "§e" + targetName + "§c has been demoted to §7Member§c.";
                    for (String memberName : team.getMembers()) {
                        if (!memberName.equals(playerName) && !memberName.equals(targetLower)) {
                            Player member = Bukkit.getPlayerExact(memberName);
                            if (member != null && member.isOnline()) {
                                member.sendMessage(notification);
                            }
                        }
                    }

                    plugin.logDebug(playerName + " demoted " + targetLower + " to member");
                } else {
                    player.sendMessage("§cFailed to demote player. Please try again.");
                }
            });
        }).exceptionally(throwable -> {
            plugin.getLogger().severe("Error demoting player: " + throwable.getMessage());
            throwable.printStackTrace();
            player.sendMessage("§cAn error occurred while demoting the player.");
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

        // Check if player has demote permission
        String roleId = team.getMemberRole(playerName);
        if (!plugin.getRoleManager().hasPermission(roleId, "demote")) {
            return completions;
        }

        if (args.length == 1) {
            // Suggest team members with roles other than "member" (excluding owner and
            // self)
            String input = args[0].toLowerCase();
            for (String memberName : team.getMembers()) {
                if (!memberName.equals(playerName) && !team.isOwner(memberName)) {
                    String memberRole = team.getMemberRole(memberName);
                    if (!memberRole.equals("member") && memberName.startsWith(input)) {
                        completions.add(memberName);
                    }
                }
            }
        }

        return completions;
    }
}