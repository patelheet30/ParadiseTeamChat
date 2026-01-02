package com.patelheet.paradiseteamchat.commands;

import com.patelheet.paradiseteamchat.ParadiseTeamChatPlugin;
import com.patelheet.paradiseteamchat.models.Team;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Command to leave a team.
 * Players can use this command to exit their current team.
 */
public class TeamLeaveCommand extends BaseCommand {

    /**
     * Constructor for TeamLeaveCommand.
     * 
     * @param plugin The main plugin instance.
     */
    public TeamLeaveCommand(ParadiseTeamChatPlugin plugin) {
        super(plugin);
    }

    @Override
    public String getPermission() {
        return "teamchat.team.use";
    }

    @Override
    public String getUsage() {
        return "/team leave";
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

        String playerName = player.getName().toLowerCase();

        // Check if player is in a team
        Team team = plugin.getCacheManager().getPlayerTeam(playerName);
        if (team == null) {
            player.sendMessage(configManager.getMessage("not-in-team"));
            return true;
        }

        // Check if player is the team owner
        if (team.isOwner(playerName)) {
            player.sendMessage(
                    "§cYou are the team owner! Use §e/team disband §cto delete your team, or §e/team kick §cto remove other members first.");
            return true;
        }

        // Proceed to remove player from team database (asynchronously)
        int teamId = team.getId();
        String teamName = team.getName();

        plugin.getAsyncTaskManager().executeAsync(() -> {
            return plugin.getTeamRepository().removeMember(teamId, playerName);
        }).thenAccept(success -> {
            // Switch back to main thread for cache updates and messaging
            plugin.getAsyncTaskManager().runSync(() -> {
                if (success) {
                    // Update caches
                    team.removeMember(playerName);
                    plugin.getCacheManager().updateTeamCache(team);

                    // Unload player's team cache
                    plugin.getCacheManager().unloadPlayerTeam(playerName);

                    // Notify player and team members
                    player.sendMessage(configManager.getMessage("player-left"));

                    // Force player to GLOBAL chat mode
                    plugin.getChatModeManager().forceGlobalMode(playerName);

                    String notification = "§e" + player.getName() + " §7has left the team.";
                    for (String memberName : team.getMembers()) {
                        Player member = Bukkit.getPlayerExact(memberName);
                        if (member != null && member.isOnline()) {
                            member.sendMessage(notification);
                        }
                    }

                    plugin.logDebug(player.getName() + " left team " + teamName);
                } else {
                    player.sendMessage("§cFailed to leave team. Please try again.");
                }
            });
        }).exceptionally(throwable -> {
            plugin.getLogger().severe("Error leaving team: " + throwable.getMessage());
            throwable.printStackTrace();
            player.sendMessage("§cAn error occurred while leaving the team.");
            return null;
        });
        return true;
    }
}
