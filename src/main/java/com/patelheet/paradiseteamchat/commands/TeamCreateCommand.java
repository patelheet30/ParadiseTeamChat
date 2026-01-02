package com.patelheet.paradiseteamchat.commands;

import com.patelheet.paradiseteamchat.ParadiseTeamChatPlugin;
import com.patelheet.paradiseteamchat.models.Team;
import com.patelheet.paradiseteamchat.utils.InputValidator.ValidationResult;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Command handler for the /team create command.
 * Allows players to create new teams.
 */
public class TeamCreateCommand extends BaseCommand {

    /**
     * Constructor for TeamCreateCommand.
     * 
     * @param plugin The main plugin instance.
     */
    public TeamCreateCommand(ParadiseTeamChatPlugin plugin) {
        super(plugin);
    }

    @Override
    public String getPermission() {
        return "teamchat.team.create";
    }

    @Override
    public String getUsage() {
        return "/team create <name> <tag>";
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

        // Validate arguments
        if (args.length < 2) {
            sendUsage(sender);
            return true;
        }

        String teamName = args[0];
        String teamTag = args[1];
        String playerName = player.getName().toLowerCase();

        // Check if player is already in a team
        Team existingTeam = plugin.getCacheManager().getPlayerTeam(playerName);
        if (existingTeam != null) {
            player.sendMessage(configManager.getMessage("already-in-team"));
            return true;
        }

        // Validate team name and tag
        ValidationResult nameResult = plugin.getInputValidator().validateTeamName(teamName);
        if (!nameResult.isSuccess()) {
            player.sendMessage(configManager.getMessage("prefix") + nameResult.getErrorMessage());
            return true;
        }

        ValidationResult tagResult = plugin.getInputValidator().validateTeamTag(teamTag);
        if (!tagResult.isSuccess()) {
            player.sendMessage(configManager.getMessage("prefix") + tagResult.getErrorMessage());
            return true;
        }

        // Sanitisied inputs
        String sanitisedName = nameResult.getValue();
        String sanitisedTag = tagResult.getValue();

        // Check for existing team name or tag asynchronously
        plugin.getAsyncTaskManager().supplyAsync(() -> {
            // Check for existing team name (run in async to avoid blocking)
            Team nameCheck = plugin.getTeamRepository().getTeamByName(sanitisedName);
            if (nameCheck != null) {
                return "name_exists";
            }

            // Check for existing team tag (need to check all teams, since we don't have a
            // direct lookup)
            for (Team team : plugin.getTeamRepository().getAllTeams()) {
                if (team.getTag().equalsIgnoreCase(sanitisedTag)) {
                    return "tag_exists";
                }
            }

            return "ok";
        }).thenAccept(result -> {
            // Switch back to main thread to send messages and create team
            plugin.getAsyncTaskManager().runSync(() -> {
                if (result.equals("name_exists")) {
                    player.sendMessage(configManager.getMessage("team-exists"));
                    return;
                }

                if (result.equals("tag_exists")) {
                    player.sendMessage(configManager.getMessage("team-exists"));
                    return;
                }

                // Create the team
                int maxMembers = configManager.getMaxMembers();
                Team newTeam = new Team(
                        -1,
                        sanitisedName,
                        sanitisedTag,
                        playerName,
                        System.currentTimeMillis(),
                        maxMembers);

                // Insert team into database asynchronously
                plugin.getAsyncTaskManager().supplyAsync(() -> {
                    return plugin.getTeamRepository().createTeam(newTeam);
                }).thenAccept(teamId -> {
                    // Switch back to main thread to update cache and notify player
                    plugin.getAsyncTaskManager().runSync(() -> {
                        if (teamId == -1) {
                            player.sendMessage("§cFailed to create team. Please try again.");
                            return;
                        }

                        // Update team ID and load into cache
                        Team createdTeam = new Team(
                                teamId,
                                sanitisedName,
                                sanitisedTag,
                                playerName,
                                System.currentTimeMillis(),
                                maxMembers);
                        plugin.getCacheManager().loadPlayerTeam(playerName, createdTeam);

                        // Notify player of successful creation
                        player.sendMessage(configManager.getMessage("team-created"));
                        player.sendMessage("§aTeam Name: §e" + sanitisedName);
                        player.sendMessage("§aTeam Tag: §e[" + sanitisedTag + "]");

                        plugin.logDebug("Player " + playerName + " created team '" + sanitisedName
                                + "' with tag [" + sanitisedTag + "].");
                    });
                });
            });

        }).exceptionally(throwable -> {
            plugin.getLogger().severe("Error creating team: " + throwable.getMessage());
            throwable.printStackTrace();
            player.sendMessage("§cAn error occurred while creating the team.");
            return null;
        });

        return true;
    }
}
