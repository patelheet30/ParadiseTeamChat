package com.patelheet.paradiseteamchat.commands;

import com.patelheet.paradiseteamchat.ParadiseTeamChatPlugin;
import com.patelheet.paradiseteamchat.models.RoleDefinition;
import com.patelheet.paradiseteamchat.models.Team;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * Command to check a player's role in the team.
 */
public class TeamRoleCommand extends BaseCommand implements TabCompleter {

    public TeamRoleCommand(ParadiseTeamChatPlugin plugin) {
        super(plugin);
    }

    @Override
    public String getPermission() {
        return "teamchat.team.use";
    }

    @Override
    public String getUsage() {
        return "/team role [player]";
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

        String playerName = player.getName().toLowerCase();
        Team team = plugin.getCacheManager().getPlayerTeam(playerName);

        if (team == null) {
            player.sendMessage(configManager.getMessage("not-in-team"));
            return true;
        }

        // Check own role or someone else's
        String targetName;
        if (args.length == 0) {
            targetName = player.getName();
        } else {
            targetName = args[0];
        }

        String targetLower = targetName.toLowerCase();

        // Validate target is in team
        if (!team.isMember(targetLower)) {
            player.sendMessage(configManager.getMessage("prefix") + "§c" + targetName + " is not in your team.");
            return true;
        }

        // Get role
        String roleId = team.getMemberRole(targetLower);
        RoleDefinition role = plugin.getRoleManager().getRole(roleId);

        if (role != null) {
            String message = configManager.getMessage("role-info", "{player}", capitaliseFirst(targetLower))
                    .replace("{role}", role.getFormattedRoleBrackets());
            player.sendMessage(message);
        } else {
            player.sendMessage("§e" + capitaliseFirst(targetLower) + " §7has an unknown role.");
        }

        return true;
    }

    /**
     * Capitalises the first letter of a string.
     * 
     * @param str The input string.
     * @return The string with the first letter capitalised.
     */
    private String capitaliseFirst(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return str.substring(0, 1).toUpperCase() + str.substring(1);
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

        if (args.length == 1) {
            // Suggest team members
            String input = args[0].toLowerCase();
            for (String memberName : team.getMembers()) {
                if (memberName.startsWith(input)) {
                    completions.add(memberName);
                }
            }
        }

        return completions;
    }
}