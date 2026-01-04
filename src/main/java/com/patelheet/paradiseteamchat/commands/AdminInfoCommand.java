package com.patelheet.paradiseteamchat.commands;

import com.patelheet.paradiseteamchat.ParadiseTeamChatPlugin;
import com.patelheet.paradiseteamchat.models.Team;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Admin command to view detailed team information.
 * Shows additional details not visible to regular players.
 */
public class AdminInfoCommand extends BaseCommand implements TabCompleter {

    /**
     * Constructor for AdminInfoCommand.
     * 
     * @param plugin The main plugin instance.
     */
    public AdminInfoCommand(ParadiseTeamChatPlugin plugin) {
        super(plugin);
    }

    @Override
    public String getPermission() {
        return "teamchat.admin";
    }

    @Override
    public String getUsage() {
        return "/teamadmin info <team>";
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

        // Try cache first
        Team team = findTeamInCache(targetTeamName);

        if (team != null) {
            displayAdminTeamInfo(sender, team);
            return true;
        }

        // Query database
        plugin.getAsyncTaskManager().supplyAsync(() -> {
            return plugin.getTeamRepository().getTeamByName(targetTeamName);
        }).thenAccept(foundTeam -> {
            plugin.getAsyncTaskManager().runSync(() -> {
                if (foundTeam == null) {
                    sender.sendMessage(configManager.getMessage("team-not-found", "{team}", targetTeamName));
                } else {
                    displayAdminTeamInfo(sender, foundTeam);
                }
            });
        }).exceptionally(throwable -> {
            plugin.getLogger().severe("Error fetching team info: " + throwable.getMessage());
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
     * Displays detailed admin team information.
     * 
     * @param sender The command sender.
     * @param team   The team to display info for.
     */
    private void displayAdminTeamInfo(CommandSender sender, Team team) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy HH:mm:ss");
        String createdDate = dateFormat.format(new Date(team.getCreatedDate()));

        // Count online members
        int onlineCount = 0;
        for (String memberName : team.getMembers()) {
            Player member = Bukkit.getPlayerExact(memberName);
            if (member != null && member.isOnline()) {
                onlineCount++;
            }
        }

        sender.sendMessage("§8§m                                    ");
        sender.sendMessage("§c§lAdmin Team Information");
        sender.sendMessage("");
        sender.sendMessage("§eTeam ID: §f" + team.getId());
        sender.sendMessage("§eTeam Name: §f" + team.getName());
        sender.sendMessage("§eTeam Tag: §f[" + team.getTag() + "]");
        sender.sendMessage("§eOwner: §f" + capitaliseFirst(team.getOwnerName()));
        sender.sendMessage("§eMembers: §f" + team.getMemberCount() + "/" + team.getMemberLimit() +
                " §7(" + onlineCount + " online)");
        sender.sendMessage("§eCreated: §f" + createdDate);
        sender.sendMessage("");
        sender.sendMessage("§eTeam Members:");

        for (String memberName : team.getMembers()) {
            Player member = Bukkit.getPlayerExact(memberName);
            boolean isOnline = (member != null && member.isOnline());
            String status = isOnline ? "§a●" : "§7●";
            String displayName = capitaliseFirst(memberName);

            // Show owner and online status
            if (team.isOwner(memberName)) {
                sender.sendMessage("  " + status + " §f" + displayName + " §6[Owner]");
            } else {
                sender.sendMessage("  " + status + " §f" + displayName);
            }
        }

        sender.sendMessage("§8§m                                    ");
    }

    /**
     * Capitalises the first letter of a string.
     * 
     * @param str The input string.
     * @return The string with first letter capitalised.
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

        if (args.length == 1) {
            String input = args[0].toLowerCase();

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