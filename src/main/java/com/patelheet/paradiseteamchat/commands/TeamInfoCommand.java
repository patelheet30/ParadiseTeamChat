package com.patelheet.paradiseteamchat.commands;

import com.patelheet.paradiseteamchat.ParadiseTeamChatPlugin;
import com.patelheet.paradiseteamchat.models.RoleDefinition;
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
 * Command to display team information.
 * Players can view details about their own team or other teams.
 */
public class TeamInfoCommand extends BaseCommand implements TabCompleter {

    /**
     * Constructor for TeamInfoCommand.
     * 
     * @param plugin The main plugin instance.
     */
    public TeamInfoCommand(ParadiseTeamChatPlugin plugin) {
        super(plugin);
    }

    @Override
    public String getPermission() {
        return "teamchat.team.use";
    }

    @Override
    public String getUsage() {
        return "/team info [team]";
    }

    /**
     * Finds a team by name from the cache (if any member is online).
     * 
     * @param teamName The name of the team to find.
     * @return The Team object if found, null otherwise.
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

    /**
     * Displays team information to the player.
     * 
     * @param player The player to display the info to.
     * @param team   The team whose info is to be displayed.
     */
    private void displayTeamInfo(Player player, Team team) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy");
        String createdDate = dateFormat.format(new Date(team.getCreatedDate()));

        player.sendMessage("§8§m                                    ");
        player.sendMessage("§b§lTeam Information");
        player.sendMessage("");
        player.sendMessage("§eTeam Name: §f" + team.getName());
        player.sendMessage("§eTeam Tag: §f[" + team.getTag() + "]");
        player.sendMessage("§eOwner: §f" + capitaliseFirst(team.getOwnerName()));
        player.sendMessage("§eMembers: §f" + team.getMemberCount() + "/" + team.getMemberLimit());
        player.sendMessage("§eCreated: §f" + createdDate);
        player.sendMessage("");
        player.sendMessage("§eTeam Members:");

        boolean rolesEnabled = plugin.getRoleManager().isRolesEnabled();

        for (String memberName : team.getMembers()) {
            Player member = Bukkit.getPlayerExact(memberName);
            boolean isOnline = (member != null && member.isOnline());
            String status = isOnline ? "§a●" : "§7●";
            String displayName = capitaliseFirst(memberName);

            // Get role display
            String roleDisplay = "";
            if (rolesEnabled) {
                String roleId = team.getMemberRole(memberName);
                RoleDefinition role = plugin.getRoleManager().getRole(roleId);
                if (role != null) {
                    roleDisplay = " " + role.getFormattedRoleBrackets();
                }
            }

            player.sendMessage("  " + status + " §f" + displayName + roleDisplay);
        }

        player.sendMessage("§8§m                                    ");
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
        Team team;

        // If no arguments, show player's own team info
        if (args.length == 0) {
            team = plugin.getCacheManager().getPlayerTeam(playerName);

            if (team == null) {
                player.sendMessage(configManager.getMessage("not-in-team"));
                return true;
            }
        } else {
            // Show info for specified team (async lookup)
            String targetTeamName = args[0];

            // Try cache first (if any member is online)
            team = findTeamInCache(targetTeamName);

            if (team != null) {
                displayTeamInfo(player, team);
                return true;
            }

            // Not in cache - query database
            plugin.getAsyncTaskManager().supplyAsync(() -> {
                return plugin.getTeamRepository().getTeamByName(targetTeamName);
            }).thenAccept(foundTeam -> {
                plugin.getAsyncTaskManager().runSync(() -> {
                    if (foundTeam == null) {
                        player.sendMessage(configManager.getMessage("team-not-found", "{team}", targetTeamName));
                    } else {
                        displayTeamInfo(player, foundTeam);
                    }
                });
            }).exceptionally(throwable -> {
                plugin.getLogger().severe("Error fetching team info: " + throwable.getMessage());
                throwable.printStackTrace();
                player.sendMessage("§cAn error occurred while fetching team information.");
                return null;
            });

            return true;
        }

        // Display info for cached team
        displayTeamInfo(player, team);
        return true;
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
