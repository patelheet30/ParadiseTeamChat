package com.patelheet.paradiseteamchat.commands;

import com.patelheet.paradiseteamchat.ParadiseTeamChatPlugin;
import com.patelheet.paradiseteamchat.models.Team;
import com.patelheet.paradiseteamchat.utils.InputValidator.ValidationResult;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * Command handler for the /team invite command.
 * Allows team members to invite other players to their team.
 */
public class TeamInviteCommand extends BaseCommand implements TabCompleter {

    /**
     * Constructor for TeamInviteCommand.
     * 
     * @param plugin The main plugin instance.
     */
    public TeamInviteCommand(ParadiseTeamChatPlugin plugin) {
        super(plugin);
    }

    @Override
    public String getPermission() {
        return "teamchat.team.invite";
    }

    @Override
    public String getUsage() {
        return "/team invite <player>";
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        // Validate sender is a player
        Player player = validatePlayer(sender);
        if (player == null) {
            return true;
        }

        // Check permission
        if (!checkPermission(sender)) {
            return true;
        }

        // Check arguments
        if (args.length < 1) {
            sendUsage(sender);
            return true;
        }

        String targetName = args[0];
        String playerName = player.getName().toLowerCase();

        // Validate target player name
        ValidationResult nameResult = plugin.getInputValidator().validatePlayerName(targetName, playerName);
        if (!nameResult.isSuccess()) {
            player.sendMessage(configManager.getMessage("prefix") + nameResult.getErrorMessage());
            return true;
        }

        // Check if target player is already in a team
        Team team = plugin.getCacheManager().getPlayerTeam(playerName);
        if (team == null) {
            player.sendMessage(configManager.getMessage("not-in-team"));
            return true;
        }

        // Check if player has permission to invite
        if (plugin.getRoleManager().isRolesEnabled()) {
            String roleId = team.getMemberRole(playerName);
            if (!plugin.getRoleManager().hasPermission(roleId, "invite")) {
                player.sendMessage(configManager.getMessage("no-invite-permission"));
                return true;
            }
        }

        // Send invite (delegate to InviteManager)
        plugin.getInviteManager().sendInvite(player, targetName, team);

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, org.bukkit.command.Command command, String alias,
            String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            String input = args[0].toLowerCase();

            for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                String name = onlinePlayer.getName();

                if (sender instanceof Player && name.equalsIgnoreCase(sender.getName())) {
                    continue;
                }

                if (name.toLowerCase().startsWith(input)) {
                    completions.add(name);
                }
            }
        }

        return completions;
    }

}
