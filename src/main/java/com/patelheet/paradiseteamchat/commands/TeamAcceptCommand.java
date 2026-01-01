package com.patelheet.paradiseteamchat.commands;

import com.patelheet.paradiseteamchat.ParadiseTeamChatPlugin;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Command to accept a team invitation.
 * Players can use this command to join a team they have been invited to.
 */
public class TeamAcceptCommand extends BaseCommand {

    /**
     * Constructor for TeamAcceptCommand.
     * 
     * @param plugin The main plugin instance.
     */
    public TeamAcceptCommand(ParadiseTeamChatPlugin plugin) {
        super(plugin);
    }

    @Override
    public String getPermission() {
        return "teamchat.team.use";
    }

    @Override
    public String getUsage() {
        return "/team accept";
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

        // Delegate to InviteManager (handles all validation and database operations)
        plugin.getInviteManager().acceptInvite(player);

        return true;
    }
}
