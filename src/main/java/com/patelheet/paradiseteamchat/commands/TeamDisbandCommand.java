package com.patelheet.paradiseteamchat.commands;

import com.patelheet.paradiseteamchat.ParadiseTeamChatPlugin;
import com.patelheet.paradiseteamchat.models.Team;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Command to disband a team.
 * Only team owners can disband their teams.
 */
public class TeamDisbandCommand extends BaseCommand {

    /**
     * Constructor for TeamDisbandCommand.
     * 
     * @param plugin The main plugin instance.
     */
    public TeamDisbandCommand(ParadiseTeamChatPlugin plugin) {
        super(plugin);
    }

    @Override
    public String getPermission() {
        return "teamchat.team.disband";
    }

    @Override
    public String getUsage() {
        return "/team disband";
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        // Validate sender is a player
        Player player = validatePlayer(sender);
        if (player == null) {
            return true;
        }

        // Check permissions
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

        // Check if player is the owner of the team
        if (!team.isOwner(playerName)) {
            player.sendMessage(configManager.getMessage("not-owner"));
            return true;
        }

        // Notify all team members about disbanding
        String disbandMessage = configManager.getMessage("team-disbanded");
        for (String memberName : team.getMembers()) {
            Player member = Bukkit.getPlayerExact(memberName);
            if (member != null && member.isOnline()) {
                member.sendMessage(disbandMessage);
            }
        }

        // Proceed to disband the team asynchronously
        int teamId = team.getId();
        String teamName = team.getName();

        plugin.getAsyncTaskManager().executeAsync(() -> {
            return plugin.getTeamRepository().deleteTeam(teamId);
        }).thenAccept(success -> {
            // Switch back to main thread to update cache and notify player
            plugin.getAsyncTaskManager().runSync(() -> {
                if (success) {
                    // Remove team from cache
                    plugin.getCacheManager().invalidateTeam(teamId);

                    // Clear all invites related to the team
                    plugin.getInviteManager().clearAllInvitesForTeam(teamId);

                    for (String memberName : team.getMembers()) {
                        plugin.getChatModeManager().forceGlobalMode(memberName);

                        // Notify online members about chat mode change
                        Player member = Bukkit.getPlayerExact(memberName);
                        if (member != null && member.isOnline()) {
                            member.sendMessage(configManager.getMessage("team-disbanded"));
                            member.sendMessage("§eChat mode switched to GLOBAL.");
                        }
                    }

                    plugin.logDebug("Team '" + teamName + "' disbanded by " + player.getName());
                } else {
                    player.sendMessage("§cFailed to disband team. Please try again.");
                }
            });
        }).exceptionally(throwable -> {
            plugin.getLogger().severe("Error disbanding team: " + throwable.getMessage());
            throwable.printStackTrace();
            player.sendMessage("§cAn error occurred while disbanding the team.");
            return null;
        });

        return true;
    }
}
