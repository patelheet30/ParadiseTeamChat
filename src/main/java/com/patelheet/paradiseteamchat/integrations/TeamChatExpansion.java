package com.patelheet.paradiseteamchat.integrations;

import com.patelheet.paradiseteamchat.ParadiseTeamChatPlugin;
import com.patelheet.paradiseteamchat.models.Team;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class TeamChatExpansion extends PlaceholderExpansion {
    private final ParadiseTeamChatPlugin plugin;

    public TeamChatExpansion(ParadiseTeamChatPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    @NotNull
    public String getIdentifier() {
        return "ptc";
    }

    @Override
    @NotNull
    public String getAuthor() {
        return "patelheet30";
    }

    @Override
    @NotNull
    public String getVersion() {
        return plugin.getPluginMeta().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onPlaceholderRequest(Player player, @NotNull String params) {
        if (player == null) {
            return "";
        }

        Team team = plugin.getCacheManager().getPlayerTeam(player.getName().toLowerCase());

        if (params.equalsIgnoreCase("tag")) {
            if (team != null) {
                return "§8[§b" + team.getTag() + "§8]§r ";
            }
            return "";
        }

        if (params.equalsIgnoreCase("tag_raw")) {
            if (team != null) {
                return team.getTag();
            }
            return "";
        }

        if (params.equalsIgnoreCase("name")) {
            if (team != null) {
                return team.getName();
            }
            return "";
        }

        if (params.equalsIgnoreCase("members")) {
            if (team != null) {
                return String.valueOf(team.getMemberCount());
            }
            return "0";
        }

        if (params.equalsIgnoreCase("has_team")) {
            return team != null ? "true" : "false";
        }

        return null;
    }

}
