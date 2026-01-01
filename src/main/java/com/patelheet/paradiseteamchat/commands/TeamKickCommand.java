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
 * Command to kick a member from the team.
 * Only team owners can kick members.
 */
public class TeamKickCommand extends BaseCommand implements TabCompleter {

    /**
     * Constructor for the TeamKickCommand.
     * 
     * @param plugin The main plugin instance.
     */
    public TeamKickCommand(ParadiseTeamChatPlugin plugin) {
        super(plugin);
    }

    @Override
    public String getPermission() {
        return "teamchat.team.kick";
    }

    @Override
    public String getUsage() {
        return "/team kick <player>";
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
        String targetLower = targetName.toLowerCase();

        // Validate team membership and ownership
        Team team = plugin.getCacheManager().getPlayerTeam(playerName);
        if (team == null) {
            player.sendMessage(configManager.getMessage("not-in-team"));
            return true;
        }

        // Only team owners can kick members
        if (!team.isOwner(playerName)) {
            player.sendMessage(configManager.getMessage("not-owner"));
            return true;
        }

        // Prevent kicking oneself
        if (targetLower.equals(playerName)) {
            player.sendMessage("§cYou cannot kick yourself! Use /team disband to delete your team.");
            return true;
        }

        // Check if target is in the team
        if (!team.isMember(targetLower)) {
            player.sendMessage(configManager.getMessage("prefix") + "§c" + targetName + " is not in your team.");
            return true;
        }

        // Proceed to kick the member asynchronously
        int teamId = team.getId();

        plugin.getAsyncTaskManager().executeAsync(() -> {
            return plugin.getTeamRepository().removeMember(teamId, targetLower);
        }).thenAccept(success -> {
            // Switch back to main thread to update cache and notify players
            plugin.getAsyncTaskManager().runSync(() -> {
                if (success) {
                    // Update team cache
                    team.removeMember(targetLower);
                    plugin.getCacheManager().updateTeamCache(team);

                    // Unload kicked player's team cache
                    plugin.getCacheManager().unloadPlayerTeam(targetLower);

                    // Force kicked player to GLOBAL chat mode
                    plugin.getChatModeManager().forceGlobalMode(targetLower);

                    // Notify sender
                    player.sendMessage(configManager.getMessage("player-kicked", "{player}", targetName));

                    // Notify kicked player if online
                    Player kickedPlayer = Bukkit.getPlayerExact(targetName);
                    if (kickedPlayer != null && kickedPlayer.isOnline()) {
                        kickedPlayer.sendMessage("§cYou have been kicked from the team.");
                    }

                    // Notify other team members
                    String notification = "§e" + targetName + " §cwas kicked from the team.";
                    for (String memberName : team.getMembers()) {
                        if (!memberName.equals(playerName) && !memberName.equals(targetLower)) {
                            Player member = Bukkit.getPlayerExact(memberName);
                            if (member != null && member.isOnline()) {
                                member.sendMessage(notification);
                            }
                        }
                    }

                    plugin.getLogger().info(playerName + " kicked " + targetLower + " from team " + team.getName());
                } else {
                    player.sendMessage("§cFailed to kick player. Please try again.");
                }
            });
        }).exceptionally(throwable -> {
            plugin.getLogger().severe("Error kicking player: " + throwable.getMessage());
            throwable.printStackTrace();
            player.sendMessage("§cAn error occurred while kicking the player.");
            return null;
        });

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, org.bukkit.command.Command command, String alias,
            String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1 && sender instanceof Player) {
            Player player = (Player) sender;
            String playerName = player.getName().toLowerCase();

            Team team = plugin.getCacheManager().getPlayerTeam(playerName);

            if (team != null && team.isOwner(playerName)) {
                String input = args[0].toLowerCase();

                for (String memberName : team.getMembers()) {
                    if (!memberName.equals(playerName) && memberName.startsWith(input)) {
                        completions.add(memberName);
                    }
                }
            }
        }

        return completions;
    }
}
