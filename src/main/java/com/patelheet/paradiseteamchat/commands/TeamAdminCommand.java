package com.patelheet.paradiseteamchat.commands;

import com.patelheet.paradiseteamchat.ParadiseTeamChatPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.*;

/**
 * Main command handler for /teamadmin command.
 * Routes to subcommands like reload, disband, transfer, info.
 * Admin-only command group.
 */
public class TeamAdminCommand implements CommandExecutor, TabCompleter {
    private final ParadiseTeamChatPlugin plugin;
    private final Map<String, BaseCommand> subcommands;

    /**
     * Constructor for TeamAdminCommand.
     * 
     * @param plugin The main plugin instance.
     */
    public TeamAdminCommand(ParadiseTeamChatPlugin plugin) {
        this.plugin = plugin;
        this.subcommands = new HashMap<>();

        registerSubcommands();
    }

    private void registerSubcommands() {
        subcommands.put("reload", new AdminReloadCommand(plugin));
        subcommands.put("disband", new AdminDisbandCommand(plugin));
        subcommands.put("transfer", new TransferOwnerCommand(plugin));
        subcommands.put("info", new AdminInfoCommand(plugin));
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Check admin permission
        if (!sender.hasPermission("teamchat.admin")) {
            sender.sendMessage(plugin.getConfigManager().getMessage("no-permission"));
            return true;
        }

        if (args.length == 0) {
            sendHelpMessage(sender);
            return true;
        }

        String subcommand = args[0].toLowerCase();
        BaseCommand handler = subcommands.get(subcommand);

        if (handler == null) {
            sender.sendMessage("§cUnknown subcommand: " + subcommand);
            sendHelpMessage(sender);
            return true;
        }

        // Extract sub-arguments (exclude the subcommand itself)
        String[] subArgs = Arrays.copyOfRange(args, 1, args.length);

        return handler.execute(sender, subArgs);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        // Check permission first
        if (!sender.hasPermission("teamchat.admin")) {
            return completions;
        }

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
        sender.sendMessage("§c§lTeamChat Admin Commands");
        sender.sendMessage("");
        sender.sendMessage("§e/teamadmin reload §7- Reload configuration");
        sender.sendMessage("§e/teamadmin disband <team> §7- Force disband any team");
        sender.sendMessage("§e/teamadmin transfer <team> <newOwner> §7- Transfer team ownership");
        sender.sendMessage("§e/teamadmin info <team> §7- View detailed team info");
        sender.sendMessage("§8§m                                    ");
    }
}
