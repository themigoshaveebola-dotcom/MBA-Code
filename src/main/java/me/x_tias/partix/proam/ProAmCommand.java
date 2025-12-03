/*
 * Decompiled with CFR 0.152.
 *
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.entity.Player
 */
package me.x_tias.partix.proam;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Description;
import co.aikar.commands.annotation.Subcommand;
import me.x_tias.partix.Partix;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

@CommandAlias(value = "proam")
public class ProAmCommand
        extends BaseCommand {
    private final ProAmManager proAmManager;

    public ProAmCommand(Partix plugin) {
        this.proAmManager = plugin.getProAmManager();
    }

    @Subcommand(value = "create")
    @Description(value = "Create a new Pro Am team.")
    public void onCreate(Player player, String teamName) {
        this.proAmManager.createTeam(player, teamName);
    }

    @Subcommand(value = "invite")
    @Description(value = "Invite a player to your Pro Am team.")
    public void onInvite(Player player, String targetName) {
        Player target = Bukkit.getPlayerExact(targetName);
        if (target == null) {
            player.sendMessage("§cPlayer " + targetName + " is not online.");
            return;
        }
        if (player.equals(target)) {
            player.sendMessage("§cYou cannot invite yourself.");
            return;
        }
        this.proAmManager.invitePlayer(player, target);
    }

    @Subcommand(value = "accept")
    @Description(value = "Accept an invite to a Pro Am team. Usage: /proam accept [teamname]")
    public void onAccept(Player player, String teamName) {
        this.proAmManager.acceptInvite(player, teamName);
    }

    @Subcommand(value = "kick")
    @Description(value = "Kick a player from your Pro Am team. Usage: /proam kick [player]")
    public void onKick(Player player, Player target) {
        this.proAmManager.kickPlayer(player, target);
    }

    @Subcommand(value = "disband")
    @Description(value = "Disband your Pro Am team. (Team leader only)")
    public void onDisband(Player player) {
        this.proAmManager.disbandTeam(player);
    }

    @Subcommand(value = "leave")
    @Description(value = "Leave your Pro Am team. (Non-leaders only)")
    public void onLeave(Player player) {
        this.proAmManager.leaveTeam(player);
    }

    @Subcommand(value = "elo")
    @Description(value = "Check your team's current ELO.")
    public void onElo(Player player) {
        ProAmTeam team = this.proAmManager.getTeam(player);
        if (team != null) {
            player.sendMessage("§aYour team (" + team.getName() + ") has §e" + team.getElo() + " ELO§a.");
        } else {
            player.sendMessage("§cYou are not part of a Pro Am team.");
        }
    }

    @Subcommand(value = "queue cancel")
    @Description(value = "Cancel your queue entry. (Leader cancels entire team; non-leaders cancel their own entry)")
    public void onQueueCancel(Player player) {
        ProAmTeam team = this.proAmManager.getTeam(player);
        if (team == null) {
            player.sendMessage("§cYou are not in a Pro Am team.");
            return;
        }
        int queuedType = team.getQueuedType();
        if (!team.isQueued() && team.getQueueCount(queuedType) == 0) {
            player.sendMessage("§cYou are not currently queued.");
            return;
        }
        if (team.isLeader(player.getUniqueId())) {
            team.clearQueue(queuedType);
            team.setQueued(false);
            this.proAmManager.getQueuedTeams().get(queuedType).remove(team);
            player.sendMessage("§aYour team queue has been cancelled.");
        } else {
            team.removeFromQueue(queuedType, player.getUniqueId());
            player.sendMessage("§aYou have left the queue.");
        }
        this.proAmManager.showProAmGUI(player);
    }

    @Subcommand(value = "admindisband")
    @CommandPermission(value = "proam.admin.disband")
    @Description(value = "Admin: Disband a Pro Am team by its name.")
    public void onAdminDisband(Player player, String teamName) {
        this.proAmManager.adminDisbandTeam(teamName, player);
    }

    @Subcommand(value = "test")
    @CommandPermission(value = "proam.test")
    @Description(value = "Test a 4v4 match using your team's jerseys and armor. (Only requires 1 player for testing)")
    public void onTest(Player player, String mode) {
        if (!mode.equalsIgnoreCase("4v4")) {
            player.sendMessage("§cOnly 4v4 test mode is supported.");
            return;
        }
        ProAmTeam team = this.proAmManager.getTeam(player);
        if (team == null) {
            player.sendMessage("§cYou are not in a Pro Am team.");
            return;
        }
        Partix.getInstance().getProAmGameManager().startMatch(team, team, 4);
    }
}

