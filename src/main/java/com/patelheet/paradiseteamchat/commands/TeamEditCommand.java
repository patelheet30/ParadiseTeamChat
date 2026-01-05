package com.patelheet.paradiseteamchat.commands;

import com.patelheet.paradiseteamchat.ParadiseTeamChatPlugin;
import com.patelheet.paradiseteamchat.models.Team;
import com.patelheet.paradiseteamchat.utils.InputValidator.ValidationResult;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Command to edit team properties (name and tag).
 * Only team owners can edit their team.
 */
public class TeamEditCommand extends BaseCommand {

    /**
     * Constructor for TeamEditCommand.
     * 
     * @param plugin The main plugin instance.
     */
    public TeamEditCommand(ParadiseTeamChatPlugin plugin) {
        super(plugin);
    }

    @Override
    public String getPermission() {
        return "teamchat.team.edit";
    }

    @Override
    public String getUsage() {
        return "/team edit <name|tag> <newValue>";
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

        if (!configManager.isEditingAllowed()) {
            player.sendMessage(configManager.getMessage("editing-disabled"));
            return true;
        }

        // Check arguments
        if (args.length < 2) {
            sendUsage(sender);
            return true;
        }

        String playerName = player.getName().toLowerCase();
        String editType = args[0].toLowerCase();
        String newValue = args[1];

        // Check if player is in a team
        Team team = plugin.getCacheManager().getPlayerTeam(playerName);
        if (team == null) {
            player.sendMessage(configManager.getMessage("not-in-team"));
            return true;
        }

        // Check if player is the team owner
        if (!team.isOwner(playerName)) {
            player.sendMessage(configManager.getMessage("not-owner"));
            return true;
        }

        // Route to appropriate edit method
        switch (editType) {
            case "name":
                editTeamName(player, team, newValue);
                break;
            case "tag":
                editTeamTag(player, team, newValue);
                break;
            default:
                player.sendMessage("§cInvalid edit type. Use §e/team edit <name|tag> <newValue>");
                break;
        }

        return true;
    }

    /**
     * Edit the team's name.
     * 
     * @param player  The player requesting the edit.
     * @param team    The team to edit.
     * @param newName The new team name.
     */
    private void editTeamName(Player player, Team team, String newName) {
        // Validate new name
        ValidationResult nameResult = plugin.getInputValidator().validateTeamName(newName);
        if (!nameResult.isSuccess()) {
            player.sendMessage(configManager.getMessage("prefix") + nameResult.getErrorMessage());
            return;
        }

        String sanitisedName = nameResult.getValue();
        String oldName = team.getName();

        // Check if name is the same
        if (sanitisedName.equalsIgnoreCase(oldName)) {
            player.sendMessage("§eYour team already has this name!");
            return;
        }

        int teamId = team.getId();

        // Check for duplicate name asynchronously
        plugin.getAsyncTaskManager().supplyAsync(() -> {
            Team existingTeam = plugin.getTeamRepository().getTeamByName(sanitisedName);
            return existingTeam == null ? "ok" : "exists";
        }).thenAccept(result -> {
            plugin.getAsyncTaskManager().runSync(() -> {
                if (result.equals("exists")) {
                    player.sendMessage(configManager.getMessage("team-exists"));
                    return;
                }

                // Update database
                plugin.getAsyncTaskManager().supplyAsync(() -> {
                    try {
                        java.sql.Connection conn = plugin.getDatabaseManager().getConnection();
                        java.sql.PreparedStatement pstmt = conn.prepareStatement(
                                "UPDATE teams SET name = ?, name_lower = ? WHERE id = ?");
                        pstmt.setString(1, sanitisedName);
                        pstmt.setString(2, sanitisedName.toLowerCase());
                        pstmt.setInt(3, teamId);

                        int rowsAffected = pstmt.executeUpdate();
                        pstmt.close();

                        return rowsAffected > 0;
                    } catch (java.sql.SQLException e) {
                        plugin.getLogger().severe("Error updating team name: " + e.getMessage());
                        e.printStackTrace();
                        return false;
                    }
                }).thenAccept(success -> {
                    plugin.getAsyncTaskManager().runSync(() -> {
                        if (success) {
                            // Create updated team object
                            Team updatedTeam = new Team(
                                    team.getId(),
                                    sanitisedName,
                                    team.getTag(),
                                    team.getOwnerName(),
                                    team.getCreatedDate(),
                                    team.getMemberLimit());

                            // Copy members
                            for (String member : team.getMembers()) {
                                if (!member.equals(team.getOwnerName())) {
                                    updatedTeam.addMember(member);
                                }
                            }

                            // Update cache
                            plugin.getCacheManager().updateTeamCache(updatedTeam);

                            // Notify all team members
                            String notification = "§aTeam name changed from §e" + oldName +
                                    "§a to §e" + sanitisedName + "§a!";
                            for (String memberName : updatedTeam.getMembers()) {
                                Player member = Bukkit.getPlayerExact(memberName);
                                if (member != null && member.isOnline()) {
                                    member.sendMessage(notification);
                                }
                            }

                            plugin.logDebug("Team ID " + teamId + " name changed: " + oldName +
                                    " -> " + sanitisedName);
                        } else {
                            player.sendMessage("§cFailed to update team name. Please try again.");
                        }
                    });
                });
            });
        }).exceptionally(throwable -> {
            plugin.getLogger().severe("Error checking duplicate team name: " + throwable.getMessage());
            throwable.printStackTrace();
            player.sendMessage("§cAn error occurred while checking the team name.");
            return null;
        });
    }

    /**
     * Edit the team's tag.
     * 
     * @param player The player requesting the edit.
     * @param team   The team to edit.
     * @param newTag The new team tag.
     */
    private void editTeamTag(Player player, Team team, String newTag) {
        // Validate new tag
        ValidationResult tagResult = plugin.getInputValidator().validateTeamTag(newTag);
        if (!tagResult.isSuccess()) {
            player.sendMessage(configManager.getMessage("prefix") + tagResult.getErrorMessage());
            return;
        }

        String sanitisedTag = tagResult.getValue();
        String oldTag = team.getTag();

        // Check if tag is the same
        if (sanitisedTag.equalsIgnoreCase(oldTag)) {
            player.sendMessage("§eYour team already has this tag!");
            return;
        }

        int teamId = team.getId();

        // Check for duplicate tag asynchronously
        plugin.getAsyncTaskManager().supplyAsync(() -> {
            for (Team t : plugin.getTeamRepository().getAllTeams()) {
                if (t.getTag().equalsIgnoreCase(sanitisedTag)) {
                    return "exists";
                }
            }
            return "ok";
        }).thenAccept(result -> {
            plugin.getAsyncTaskManager().runSync(() -> {
                if (result.equals("exists")) {
                    player.sendMessage(configManager.getMessage("team-exists"));
                    return;
                }

                // Update database
                plugin.getAsyncTaskManager().supplyAsync(() -> {
                    try {
                        java.sql.Connection conn = plugin.getDatabaseManager().getConnection();
                        java.sql.PreparedStatement pstmt = conn.prepareStatement(
                                "UPDATE teams SET tag = ? WHERE id = ?");
                        pstmt.setString(1, sanitisedTag);
                        pstmt.setInt(2, teamId);

                        int rowsAffected = pstmt.executeUpdate();
                        pstmt.close();

                        return rowsAffected > 0;
                    } catch (java.sql.SQLException e) {
                        plugin.getLogger().severe("Error updating team tag: " + e.getMessage());
                        e.printStackTrace();
                        return false;
                    }
                }).thenAccept(success -> {
                    plugin.getAsyncTaskManager().runSync(() -> {
                        if (success) {
                            // Create updated team object
                            Team updatedTeam = new Team(
                                    team.getId(),
                                    team.getName(),
                                    sanitisedTag,
                                    team.getOwnerName(),
                                    team.getCreatedDate(),
                                    team.getMemberLimit());

                            // Copy members
                            for (String member : team.getMembers()) {
                                if (!member.equals(team.getOwnerName())) {
                                    updatedTeam.addMember(member);
                                }
                            }

                            // Update cache
                            plugin.getCacheManager().updateTeamCache(updatedTeam);

                            // Notify all team members
                            String notification = "§aTeam tag changed from §e[" + oldTag +
                                    "]§a to §e[" + sanitisedTag + "]§a!";
                            for (String memberName : updatedTeam.getMembers()) {
                                Player member = Bukkit.getPlayerExact(memberName);
                                if (member != null && member.isOnline()) {
                                    member.sendMessage(notification);
                                }
                            }

                            plugin.logDebug("Team ID " + teamId + " tag changed: " + oldTag +
                                    " -> " + sanitisedTag);
                        } else {
                            player.sendMessage("§cFailed to update team tag. Please try again.");
                        }
                    });
                });
            });
        }).exceptionally(throwable -> {
            plugin.getLogger().severe("Error checking duplicate team tag: " + throwable.getMessage());
            throwable.printStackTrace();
            player.sendMessage("§cAn error occurred while checking the team tag.");
            return null;
        });
    }
}