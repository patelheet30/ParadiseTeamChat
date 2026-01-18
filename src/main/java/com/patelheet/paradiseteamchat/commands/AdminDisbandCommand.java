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
 * Admin command to force disband any team.
 * Does not require team ownership - admins can delete any team.
 */
public class AdminDisbandCommand extends BaseCommand implements TabCompleter {

    /**
     * Constructor for AdminDisbandCommand.
     * 
     * @param plugin The main plugin instance.
     */
    public AdminDisbandCommand(ParadiseTeamChatPlugin plugin) {
        super(plugin);
    }

    @Override
    public String getPermission() {
        return "teamchat.admin";
    }

    @Override
    public String getUsage() {
        return "/teamadmin disband <team>";
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        // Check permission
        if (!checkPermission(sender)) {
            return true;
        }

        // Validate arguments
        if (args.length < 1) {
            sendUsage(sender);
            return true;
        }

        String targetTeamName = args[0];

        // Try to find team in cache first
        Team team = findTeamInCache(targetTeamName);

        if (team != null) {
            // Team found in cache - disband it
            disbandTeam(sender, team);
            return true;
        }

        // Not in cache - query database asynchronously
        plugin.getAsyncTaskManager().supplyAsync(() -> {
            return plugin.getTeamRepository().getTeamByName(targetTeamName);
        }).thenAccept(foundTeam -> {
            plugin.getAsyncTaskManager().runSync(() -> {
                if (foundTeam == null) {
                    sender.sendMessage(configManager.getMessage("team-not-found", "{team}", targetTeamName));
                } else {
                    disbandTeam(sender, foundTeam);
                }
            });
        }).exceptionally(throwable -> {
            plugin.getLogger().severe("Error fetching team for admin disband: " + throwable.getMessage());
            throwable.printStackTrace();
            sender.sendMessage("§cAn error occurred while fetching team information.");
            return null;
        });

        return true;
    }

    /**
     * Finds a team by name from the cache.
     * 
     * @param teamName The name of the team to find.
     * @return The Team object if found in cache, null otherwise.
     */
    private Team findTeamInCache(String teamName) {
        String lowerName = teamName.toLowerCase();

        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            Team team = plugin.getCacheManager().getPlayerTeam(onlinePlayer.getName().toLowerCase());
            if (team != null && team.getName().equalsIgnoreCase(lowerName)) {
                return team;
            }
        }

        return null;
    }

    /**
     * Performs the actual team disbanding.
     * 
     * @param sender The command sender (admin).
     * @param team   The team to disband.
     */
    private void disbandTeam(CommandSender sender, Team team) {
        int teamId = team.getId();
        String teamName = team.getName();
        List<String> members = new ArrayList<>(team.getMembers());

        // Notify all team members BEFORE deletion
        String disbandMessage = "§cYour team has been disbanded by an administrator.";
        for (String memberName : members) {
            Player member = Bukkit.getPlayerExact(memberName);
            if (member != null && member.isOnline()) {
                member.sendMessage(disbandMessage);
            }
        }

        // Delete team from database (async)
        plugin.getAsyncTaskManager().executeAsync(() -> {
            return plugin.getTeamRepository().deleteTeam(teamId);
        }).thenAccept(success -> {
            // Back to main thread for cache updates and notifications
            plugin.getAsyncTaskManager().runSync(() -> {
                if (success) {
                    // Remove team from cache
                    plugin.getCacheManager().invalidateTeam(teamId);

                    // Clear all invites for this team
                    plugin.getInviteManager().clearAllInvitesForTeam(teamId);

                    // Clean up team blocks
                    plugin.getTeamBlockManager().cleanupTeamBlocks(teamId);

                    // Force all members to GLOBAL chat mode
                    for (String memberName : members) {
                        plugin.getChatModeManager().forceGlobalMode(memberName);

                        // Notify online members about chat mode change
                        Player member = Bukkit.getPlayerExact(memberName);
                        if (member != null && member.isOnline()) {
                            member.sendMessage("§eChat mode switched to GLOBAL.");
                        }
                    }

                    // Notify admin
                    sender.sendMessage("§aTeam '§e" + teamName + "§a' has been disbanded.");
                    sender.sendMessage("§7" + members.size() + " members affected.");

                    // Log action
                    plugin.getLogger().info("Admin " + sender.getName() + " disbanded team '" + teamName + "'");
                    plugin.logDebug("Force disbanded team " + teamName + " (ID: " + teamId + ")");

                } else {
                    sender.sendMessage("§cFailed to disband team. Please try again.");
                }
            });
        }).exceptionally(throwable -> {
            plugin.getLogger().severe("Error in admin disband: " + throwable.getMessage());
            throwable.printStackTrace();
            sender.sendMessage("§cAn error occurred while disbanding the team.");
            return null;
        });
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, org.bukkit.command.Command command, String alias,
            String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            String input = args[0].toLowerCase();

            // Suggest cached team names
            for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                Team team = plugin.getCacheManager().getPlayerTeam(onlinePlayer.getName().toLowerCase());
                if (team != null) {
                    String teamName = team.getName();
                    if (teamName.toLowerCase().startsWith(input) && !completions.contains(teamName)) {
                        completions.add(teamName);
                    }
                }
            }
        }

        return completions;
    }
}