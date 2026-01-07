package com.patelheet.paradiseteamchat.commands;

import com.patelheet.paradiseteamchat.ParadiseTeamChatPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.*;

/**
 * Main command handler for the /team command.
 * Delegates subcommands to their respective handlers.
 */
public class TeamCommand implements CommandExecutor, TabCompleter {
    private final ParadiseTeamChatPlugin plugin;
    private final Map<String, BaseCommand> subcommands;

    public TeamCommand(ParadiseTeamChatPlugin plugin) {
        this.plugin = plugin;
        this.subcommands = new HashMap<>();

        // Register all subcommands
        registerSubcommands();
    }

    private void registerSubcommands() {
        subcommands.put("create", new TeamCreateCommand(plugin));
        subcommands.put("disband", new TeamDisbandCommand(plugin));
        subcommands.put("invite", new TeamInviteCommand(plugin));
        subcommands.put("kick", new TeamKickCommand(plugin));
        subcommands.put("accept", new TeamAcceptCommand(plugin));
        subcommands.put("leave", new TeamLeaveCommand(plugin));
        subcommands.put("info", new TeamInfoCommand(plugin));
        subcommands.put("edit", new TeamEditCommand(plugin));
        subcommands.put("role", new TeamRoleCommand(plugin));
        subcommands.put("promote", new TeamPromoteCommand(plugin));
        subcommands.put("demote", new TeamDemoteCommand(plugin));
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelpMessage(sender);
            return true;
        }

        String subcommand = args[0].toLowerCase();

        BaseCommand handler = subcommands.get(subcommand);

        if (handler == null) {
            sender.sendMessage("§cUnknown subcommand: " + subcommand);
            return true;
        }

        // Extract sub-arguments (exclude the subcommand itself)
        String[] subArgs = Arrays.copyOfRange(args, 1, args.length);

        return handler.execute(sender, subArgs);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            String input = args[0].toLowerCase();

            for (String subcommand : subcommands.keySet()) {
                if (subcommand.startsWith(input)) {
                    completions.add(subcommand);
                }
            }

            if ("help".startsWith(input)) {
                completions.add("help");
            }

            return completions;
        }

        if (args.length >= 2) {
            String subcommand = args[0].toLowerCase();
            BaseCommand handler = subcommands.get(subcommand);

            if (handler instanceof TabCompleter) {
                String[] subArgs = Arrays.copyOfRange(args, 1, args.length);
                return ((TabCompleter) handler).onTabComplete(sender, command, alias, subArgs);
            }
        }

        return completions;
    }

    private void sendHelpMessage(CommandSender sender) {
        sender.sendMessage("§8§m                                    ");
        sender.sendMessage("§b§lParadiseTeamChat Commands");
        sender.sendMessage("");
        sender.sendMessage("§e/team create <name> <tag> §7- Create a new team");
        sender.sendMessage("§e/team disband §7- Disband your team");
        sender.sendMessage("§e/team invite <player> §7- Invite a player to your team");
        sender.sendMessage("§e/team kick <player> §7- Kick a player from your team");
        sender.sendMessage("§e/team accept §7- Accept a team invitation");
        sender.sendMessage("§e/team leave §7- Leave your current team");
        sender.sendMessage("§e/team info [team] §7- View team information");

        if (plugin.getConfigManager().isEditingAllowed()) {
            sender.sendMessage("§e/team edit <name|tag> <value> §7- Edit team name or tag");
        }

        sender.sendMessage("§8§m                                    ");
    }
}
