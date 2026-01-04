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
 * Admin command to transfer team ownership to another player.
 * Allows admins to change team owners without the current owner's permission.
 */
public class TransferOwnerCommand extends BaseCommand implements TabCompleter {

    /**
     * Constructor for TransferOwnerCommand.
     * 
     * @param plugin The main plugin instance.
     */
    public TransferOwnerCommand(ParadiseTeamChatPlugin plugin) {
        super(plugin);
    }

    @Override
    public String getPermission() {
        return "teamchat.admin";
    }

    @Override
    public String getUsage() {
        return "/teamadmin transfer <team> <newOwner>";
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        // Check permission
        if (!checkPermission(sender)) {
            return true;
        }

        // Validate arguments
        if (args.length < 2) {
            sendUsage(sender);
            return true;
        }

        String targetTeamName = args[0];
        String newOwnerName = args[1];

        // Try to find team in cache first
        Team team = findTeamInCache(targetTeamName);

        if (team != null) {
            // Team found in cache - perform transfer
            performTransfer(sender, team, newOwnerName);
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
                    performTransfer(sender, foundTeam, newOwnerName);
                }
            });
        }).exceptionally(throwable -> {
            plugin.getLogger().severe("Error fetching team for transfer: " + throwable.getMessage());
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
     * Performs the ownership transfer.
     * 
     * @param sender       The command sender (admin).
     * @param team         The team to transfer.
     * @param newOwnerName The name of the new owner.
     */
    private void performTransfer(CommandSender sender, Team team, String newOwnerName) {
        String newOwnerLower = newOwnerName.toLowerCase();

        // Validate new owner is a team member
        if (!team.isMember(newOwnerLower)) {
            sender.sendMessage("§c" + newOwnerName + " is not a member of team '" + team.getName() + "'.");
            sender.sendMessage("§7They must be a team member to become owner.");
            return;
        }

        // Check if already owner
        if (team.isOwner(newOwnerLower)) {
            sender.sendMessage("§e" + newOwnerName + " is already the owner of this team.");
            return;
        }

        String oldOwner = team.getOwnerName();
        int teamId = team.getId();
        String teamName = team.getName();

        // Update database (async)
        plugin.getAsyncTaskManager().supplyAsync(() -> {
            // SQL: UPDATE teams SET owner_name = ? WHERE id = ?
            try {
                java.sql.Connection conn = plugin.getDatabaseManager().getConnection();
                java.sql.PreparedStatement pstmt = conn.prepareStatement(
                        "UPDATE teams SET owner_name = ? WHERE id = ?");
                pstmt.setString(1, newOwnerLower);
                pstmt.setInt(2, teamId);

                int rowsAffected = pstmt.executeUpdate();
                pstmt.close();

                return rowsAffected > 0;

            } catch (java.sql.SQLException e) {
                plugin.getLogger().severe("Error updating team owner: " + e.getMessage());
                e.printStackTrace();
                return false;
            }
        }).thenAccept(success -> {
            // Back to main thread for cache updates and notifications
            plugin.getAsyncTaskManager().runSync(() -> {
                if (success) {
                    // Create new Team object with updated owner
                    Team updatedTeam = new Team(
                            team.getId(),
                            team.getName(),
                            team.getTag(),
                            newOwnerLower, // New owner
                            team.getCreatedDate(),
                            team.getMemberLimit());

                    // Copy members
                    for (String member : team.getMembers()) {
                        if (!member.equals(newOwnerLower)) {
                            updatedTeam.addMember(member);
                        }
                    }

                    // Update cache with new team object
                    plugin.getCacheManager().updateTeamCache(updatedTeam);

                    // Notify admin
                    sender.sendMessage("§aOwnership of team '§e" + teamName + "§a' transferred.");
                    sender.sendMessage("§7Old owner: §f" + capitaliseFirst(oldOwner));
                    sender.sendMessage("§7New owner: §f" + capitaliseFirst(newOwnerLower));

                    // Notify old owner if online
                    Player oldOwnerPlayer = Bukkit.getPlayerExact(oldOwner);
                    if (oldOwnerPlayer != null && oldOwnerPlayer.isOnline()) {
                        oldOwnerPlayer.sendMessage("§eYou are no longer the owner of team '" + teamName + "'.");
                        oldOwnerPlayer.sendMessage("§7New owner: §f" + capitaliseFirst(newOwnerLower));
                    }

                    // Notify new owner if online
                    Player newOwnerPlayer = Bukkit.getPlayerExact(newOwnerName);
                    if (newOwnerPlayer != null && newOwnerPlayer.isOnline()) {
                        newOwnerPlayer.sendMessage("§aYou are now the owner of team '" + teamName + "'!");
                    }

                    // Notify other team members
                    String notification = "§eTeam ownership transferred to §f" + capitaliseFirst(newOwnerLower) + "§e.";
                    for (String memberName : updatedTeam.getMembers()) {
                        if (!memberName.equals(newOwnerLower) && !memberName.equals(oldOwner)) {
                            Player member = Bukkit.getPlayerExact(memberName);
                            if (member != null && member.isOnline()) {
                                member.sendMessage(notification);
                            }
                        }
                    }

                    // Log action
                    plugin.getLogger().info("Admin " + sender.getName() + " transferred ownership of team '" +
                            teamName + "' from " + oldOwner + " to " + newOwnerLower);
                    plugin.logDebug("Ownership transfer: " + teamName + " -> " + newOwnerLower);

                } else {
                    sender.sendMessage("§cFailed to transfer ownership. Please try again.");
                }
            });
        }).exceptionally(throwable -> {
            plugin.getLogger().severe("Error in ownership transfer: " + throwable.getMessage());
            throwable.printStackTrace();
            sender.sendMessage("§cAn error occurred while transferring ownership.");
            return null;
        });
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
            // Suggest team names
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
        } else if (args.length == 2) {
            // Suggest members of the specified team
            String teamName = args[0];
            String input = args[1].toLowerCase();

            Team team = findTeamInCache(teamName);
            if (team != null) {
                for (String memberName : team.getMembers()) {
                    if (memberName.startsWith(input)) {
                        completions.add(memberName);
                    }
                }
            }
        }

        return completions;
    }
}