/*
 * Decompiled with CFR 0.152.
 *
 * Could not load the following classes:
 *  net.kyori.adventure.text.Component
 *  net.kyori.adventure.text.format.NamedTextColor
 *  net.kyori.adventure.text.format.TextColor
 *  org.bukkit.Bukkit
 *  org.bukkit.Location
 *  org.bukkit.Material
 *  org.bukkit.OfflinePlayer
 *  org.bukkit.World
 *  org.bukkit.configuration.file.YamlConfiguration
 *  org.bukkit.entity.Entity
 *  org.bukkit.entity.EntityType
 *  org.bukkit.entity.Player
 *  org.bukkit.entity.Villager
 *  org.bukkit.entity.Villager$Profession
 */
package me.x_tias.partix.proam;

import lombok.Getter;
import me.x_tias.partix.Partix;
import me.x_tias.partix.plugin.gui.GUI;
import me.x_tias.partix.plugin.gui.ItemButton;
import me.x_tias.partix.util.Items;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.*;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class ProAmManager {
    @Getter
    public final Map<Integer, List<ProAmTeam>> queuedTeams = new HashMap<>();
    private final Partix plugin;
    private final File teamFile;
    private final YamlConfiguration teamConfig;
    private final Map<UUID, String> playerTeams = new HashMap<>();
    private final Map<String, ProAmTeam> teams = new HashMap<>();
    private final Location npcLocation = new Location(Bukkit.getWorlds().getFirst(), -132.5, -60.0, 29.5);
    private final Location leaderboardLocation = new Location(Bukkit.getWorld("world"), -129.552, -60.0, 26.287);
    private final int leaderboardDisplayRange = 48;
    private final int leaderboardUpdateInterval = 20;
    private final String leaderboardHoloId = "proam_leaderboard";

    public ProAmManager(Partix plugin) {
        this.plugin = plugin;
        this.teamFile = new File(plugin.getDataFolder(), "proam_teams.yml");
        this.teamConfig = YamlConfiguration.loadConfiguration(this.teamFile);
        this.loadTeams();
        this.queuedTeams.put(1, new ArrayList<>());
        this.queuedTeams.put(3, new ArrayList<>());
        this.queuedTeams.put(4, new ArrayList<>());
        this.spawnNPC();
    }

    private void loadTeams() {
        for (String teamName : this.teamConfig.getKeys(false)) {
            ProAmTeam team = new ProAmTeam(teamName, this.teamConfig);
            this.teams.put(teamName.toLowerCase(), team);
            for (UUID member : team.getMembers()) {
                this.playerTeams.put(member, teamName);
            }
        }
    }

    public void saveTeams() {
        for (String key : new ArrayList<>(this.teamConfig.getKeys(false))) {
            this.teamConfig.set(key, null);
        }
        for (ProAmTeam team : this.teams.values()) {
            team.save(this.teamConfig);
        }
        try {
            this.teamConfig.save(this.teamFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public ProAmTeam getTeam(Player player) {
        String teamName = this.playerTeams.get(player.getUniqueId());
        return teamName != null ? this.teams.get(teamName.toLowerCase()) : null;
    }

    public Collection<ProAmTeam> getAllTeams() {
        return this.teams.values();
    }

    public void createTeam(Player leader, String teamName) {
        if (this.getTeam(leader) != null) {
            leader.sendMessage("§cYou are already on a team. Leave it before creating a new one.");
            return;
        }
        if (teamName.length() > 16) {
            leader.sendMessage("§cTeam name must be at most 16 characters.");
            return;
        }
        if (!teamName.matches("[A-Za-z]+")) {
            leader.sendMessage("§cTeam name can only contain letters.");
            return;
        }
        if (this.teams.containsKey(teamName.toLowerCase())) {
            leader.sendMessage("§cA team with that name already exists.");
            return;
        }
        if (this.containsBannedWord(teamName)) {
            leader.sendMessage("§cThat team name is not allowed.");
            return;
        }
        ProAmTeam newTeam = new ProAmTeam(teamName, leader.getUniqueId());
        this.teams.put(teamName.toLowerCase(), newTeam);
        this.playerTeams.put(leader.getUniqueId(), teamName);
        this.saveTeams();
        leader.sendMessage("§aPro Am Team created!");
    }

    public void invitePlayer(Player inviter, Player invitee) {
        ProAmTeam team = this.getTeam(inviter);
        if (team == null || !team.isLeader(inviter.getUniqueId())) {
            inviter.sendMessage("§cYou must be the team leader to invite players.");
            return;
        }
        if (inviter.getUniqueId().equals(invitee.getUniqueId())) {
            inviter.sendMessage("§cYou cannot invite yourself.");
            return;
        }
        team.invite(invitee.getUniqueId());
        invitee.sendMessage("§aYou have been invited to join " + team.getName() + ". Use /proam accept " + team.getName() + " to join.");
        this.saveTeams();
    }

    public void acceptInvite(Player player, String teamName) {
        ProAmTeam currentTeam = this.getTeam(player);
        if (currentTeam != null && currentTeam.isLeader(player.getUniqueId())) {
            player.sendMessage("§cYou are the leader of your team. To change teams, disband your current team.");
            return;
        }
        ProAmTeam team = this.teams.get(teamName.toLowerCase());
        if (team != null && team.hasInvite(player.getUniqueId())) {
            team.addMember(player.getUniqueId());
            this.playerTeams.put(player.getUniqueId(), team.getName());
            player.sendMessage("§aYou joined " + team.getName() + "!");
            this.saveTeams();
        } else {
            player.sendMessage("§cNo invite found for team " + teamName);
        }
    }

    public void kickPlayer(Player kicker, Player target) {
        if (kicker.getUniqueId().equals(target.getUniqueId())) {
            kicker.sendMessage("§cYou cannot kick yourself. Use /proam leave if you wish to leave.");
            return;
        }
        ProAmTeam team = this.getTeam(kicker);
        if (team == null || !team.isLeader(kicker.getUniqueId())) {
            kicker.sendMessage("§cYou must be the team leader to kick players.");
            return;
        }
        team.kick(target.getUniqueId());
        this.playerTeams.remove(target.getUniqueId());
        this.saveTeams();
        kicker.sendMessage("§aYou have kicked " + target.getName());
    }

    public void disbandTeam(Player player) {
        ProAmTeam team = this.getTeam(player);
        if (team == null || !team.isLeader(player.getUniqueId())) {
            player.sendMessage("§cOnly the team leader can disband the team.");
            return;
        }
        this.teams.remove(team.getName().toLowerCase());
        for (UUID member : team.getMembers()) {
            this.playerTeams.remove(member);
        }
        this.saveTeams();
        player.sendMessage("§cTeam disbanded.");
    }

    public void adminDisbandTeam(String teamName, Player sender) {
        ProAmTeam team = this.teams.get(teamName.toLowerCase());
        if (team == null) {
            sender.sendMessage("§cNo team with name " + teamName + " found.");
            return;
        }
        this.teams.remove(teamName.toLowerCase());
        for (UUID member : team.getMembers()) {
            this.playerTeams.remove(member);
            Player p = Bukkit.getPlayer(member);
            if (p == null) continue;
            p.sendMessage("§cYour team " + team.getName() + " has been disbanded by an admin.");
        }
        this.saveTeams();
        sender.sendMessage("§aTeam " + team.getName() + " disbanded successfully.");
    }

    public void leaveTeam(Player player) {
        ProAmTeam team = this.getTeam(player);
        if (team == null) {
            player.sendMessage("§cYou are not in a Pro Am team.");
            return;
        }
        if (team.isLeader(player.getUniqueId())) {
            player.sendMessage("§cAs the team leader, you cannot leave your team. Disband your team instead.");
            return;
        }
        team.removeMember(player.getUniqueId());
        this.playerTeams.remove(player.getUniqueId());
        team.getQueuedPlayers(1).remove(player.getUniqueId());
        team.getQueuedPlayers(3).remove(player.getUniqueId());
        team.getQueuedPlayers(4).remove(player.getUniqueId());
        this.saveTeams();
        player.sendMessage("§aYou have left your Pro Am team.");
        if (team.isQueued()) {
            int queuedType = team.getQueuedType();
            this.queuedTeams.get(queuedType).remove(team);
            team.setQueued(false);
            for (Player p : team.getOnlineMembers()) {
                p.sendMessage("Your team queue has been canceled because a member left.");
            }
        }
    }

    public void removeFromQueueOnLeave(Player player) {
        ProAmTeam team = this.getTeam(player);
        if (team != null) {
            team.getQueuedPlayers(1).remove(player.getUniqueId());
            team.getQueuedPlayers(3).remove(player.getUniqueId());
            team.getQueuedPlayers(4).remove(player.getUniqueId());
        }
    }

    public void cancelTeamQueue(ProAmTeam team, Player player) {
        if (!team.isLeader(player.getUniqueId())) {
            team.getQueuedPlayers(3).remove(player.getUniqueId());
            team.getQueuedPlayers(4).remove(player.getUniqueId());
            player.sendMessage(Component.text("You have left the queue.", NamedTextColor.RED));
        } else {
            team.getQueuedPlayers(3).clear();
            team.getQueuedPlayers(4).clear();
            team.setQueued(false);
            this.queuedTeams.get(3).remove(team);
            this.queuedTeams.get(4).remove(team);
            player.sendMessage(Component.text("Your team's queued status has been canceled.", NamedTextColor.RED));
            for (Player p : team.getOnlineMembers()) {
                p.sendMessage(Component.text("Team queue canceled by owner.", NamedTextColor.RED));
            }
        }
    }

    private void spawnNPC() {
        World world = this.npcLocation.getWorld();
        if (world == null) {
            return;
        }
        for (Entity entity : world.getNearbyEntities(this.npcLocation, 1.0, 1.0, 1.0)) {
            Villager villager;
            if (!(entity instanceof Villager) || !"§6Pro Am Manager".equals((villager = (Villager) entity).getCustomName()))
                continue;
            System.out.println("REMOVE ENTITY AM");
            entity.remove();
        }
        Location spawnLocation = this.npcLocation.clone();
        spawnLocation.setYaw(270.0f);
        Villager villager = (Villager) world.spawnEntity(spawnLocation, EntityType.VILLAGER);
        villager.setCustomName("§6Pro Am Manager");
        villager.setCustomNameVisible(true);
        try {
            villager.setAI(false);
            villager.setCollidable(false);
        } catch (NoSuchMethodError noSuchMethodError) {
            // empty catch block
        }
        villager.setInvulnerable(true);
        villager.setSilent(true);
        villager.setProfession(Villager.Profession.NONE);
    }

    public boolean containsBannedWord(String name) {
        List<String> bannedWords = Arrays.asList("badword1", "nigga", "fuck", "shit", "cunt", "bitch");
        for (String w : bannedWords) {
            if (!name.toLowerCase().contains(w)) continue;
            return true;
        }
        return false;
    }

    public void showProAmGUI(Player player) {
        ProAmTeam team = this.getTeam(player);
        if (team == null) {
            player.sendMessage("§cYou are not part of a Pro Am team.");
            return;
        }
        if (team.isQueued()) {
            player.sendMessage("§eYour team is currently queued for a match and cannot open the menu.");
            return;
        }
        GUI gui = new GUI("Pro Am Team - " + team.getName(), 5, false);
        gui.addButton(new ItemButton(4, Items.get(Component.text("Team Members"), Material.PLAYER_HEAD), btn -> {
        }));
        int slot = 9;
        for (UUID uuid : team.getMembers()) {
            OfflinePlayer offp = Bukkit.getOfflinePlayer(uuid);
            gui.addButton(new ItemButton(slot, Items.getSkull(offp), btn -> {
            }));
            if (++slot < 18) continue;
            break;
        }
        String queuedNames3 = String.join(", ", team.getQueuedMemberNames(3));
        int queueCount3 = team.getQueueCount(3);
        String queueStatus3 = "§l3v3 Queue\n§e" + queueCount3 + "/3\n§7" + (queuedNames3.isEmpty() ? "None" : queuedNames3);
        gui.addButton(new ItemButton(20, Items.get(Component.text(queueStatus3), Material.EMERALD), btn -> {
            if (team.getMembers().size() < 3) {
                player.sendMessage("§cYour team must have at least 3 players to queue for 3v3.");
            } else {
                Partix.getInstance().getProAmGameManager().joinQueue(team, 3, player);
            }
            this.showProAmGUI(player);
        }));
        if (queueCount3 == 3) {
            if (team.isLeader(player.getUniqueId())) {
                String leaderName = Bukkit.getOfflinePlayer(team.getLeader()).getName();
                String confirmLabel = team.getName() + " - " + leaderName + " - [" + team.getElo() + "]";
                gui.addButton(new ItemButton(21, Items.get(Component.text("Confirm 3v3 Queue\n" + confirmLabel).color(NamedTextColor.GOLD), Material.GREEN_WOOL), btn -> {
                    team.setQueuedType(3);
                    team.setQueued(true);
                    Partix.getInstance().getProAmGameManager().confirmQueue(team, 3);
                    player.closeInventory();
                }));
                gui.addButton(new ItemButton(22, Items.get(Component.text("Cancel 3v3 Queue").color(NamedTextColor.RED), Material.BARRIER), btn -> {
                    Partix.getInstance().getProAmGameManager().cancelTeamQueue(team, player);
                    this.showProAmGUI(player);
                }));
            } else {
                gui.addButton(new ItemButton(21, Items.get(Component.text("Cancel 3v3 Queue").color(NamedTextColor.RED), Material.BARRIER), btn -> {
                    Partix.getInstance().getProAmGameManager().cancelQueue(team, 3, player);
                    this.showProAmGUI(player);
                }));
            }
        }
        String queuedNames4 = String.join(", ", team.getQueuedMemberNames(4));
        int queueCount4 = team.getQueueCount(4);
        String queueStatus4 = "§l4v4 Queue\n§e" + queueCount4 + "/4\n§7" + (queuedNames4.isEmpty() ? "None" : queuedNames4);
        gui.addButton(new ItemButton(23, Items.get(Component.text(queueStatus4), Material.DIAMOND), btn -> {
            if (team.getMembers().size() < 4) {
                player.sendMessage("§cYour team must have at least 4 players to queue for 4v4.");
            } else {
                Partix.getInstance().getProAmGameManager().joinQueue(team, 4, player);
            }
            this.showProAmGUI(player);
        }));
        if (queueCount4 == 4) {
            if (team.isLeader(player.getUniqueId())) {
                String leaderName = Bukkit.getOfflinePlayer(team.getLeader()).getName();
                String confirmLabel4 = team.getName() + " - " + leaderName + " - [" + team.getElo() + "]";
                gui.addButton(new ItemButton(24, Items.get(Component.text("Confirm 4v4 Queue\n" + confirmLabel4).color(NamedTextColor.GOLD), Material.GREEN_WOOL), btn -> {
                    team.setQueuedType(4);
                    team.setQueued(true);
                    Partix.getInstance().getProAmGameManager().confirmQueue(team, 4);
                    player.closeInventory();
                }));
                gui.addButton(new ItemButton(25, Items.get(Component.text("Cancel 4v4 Queue").color(NamedTextColor.RED), Material.BARRIER), btn -> {
                    Partix.getInstance().getProAmGameManager().cancelTeamQueue(team, player);
                    this.showProAmGUI(player);
                }));
            } else {
                gui.addButton(new ItemButton(24, Items.get(Component.text("Cancel 4v4 Queue").color(NamedTextColor.RED), Material.BARRIER), btn -> {
                    Partix.getInstance().getProAmGameManager().cancelQueue(team, 4, player);
                    this.showProAmGUI(player);
                }));
            }
        }
        gui.addButton(new ItemButton(36, Items.get(Component.text("Edit Jerseys").color(NamedTextColor.AQUA), Material.LEATHER_CHESTPLATE), btn -> this.openJerseyMenu(player)));
        gui.addButton(new ItemButton(37, Items.get(Component.text("View Leaderboard").color(NamedTextColor.GOLD), Material.PAPER), btn -> this.showLeaderboard(player)));
        gui.openInventory(player);
    }

    public void openJerseyMenu(Player player) {
        ProAmTeam team = this.getTeam(player);
        if (team == null || !team.isLeader(player.getUniqueId())) {
            player.sendMessage("§cOnly your team leader can change jerseys.");
            return;
        }
        GUI jerseyGui = new GUI("Edit Team Jerseys", 3, false);
        jerseyGui.addButton(new ItemButton(4, Items.get(Component.text("Select Jersey Colors").color(NamedTextColor.GREEN), Material.WARPED_DOOR), btn -> {
        }));
        jerseyGui.addButton(new ItemButton(10, Items.armor(Material.LEATHER_CHESTPLATE, this.fromName(team.getChestplateColor()), "Chestplate", "Home jersey (non‑white)"), btn -> {
            this.cycleColor(team, "chestplate");
            this.saveTeams();
            this.openJerseyMenu(player);
        }));
        jerseyGui.addButton(new ItemButton(11, Items.armor(Material.LEATHER_LEGGINGS, this.fromName(team.getLeggingsColor()), "Leggings", "Team Leggings"), btn -> {
            this.cycleColor(team, "leggings");
            this.saveTeams();
            this.openJerseyMenu(player);
        }));
        jerseyGui.addButton(new ItemButton(12, Items.armor(Material.LEATHER_BOOTS, this.fromName(team.getBootsColor()), "Boots", "Team Boots"), btn -> {
            this.cycleColor(team, "boots");
            this.saveTeams();
            this.openJerseyMenu(player);
        }));
        jerseyGui.openInventory(player);
    }

    private void cycleColor(ProAmTeam team, String piece) {
        List<String> colors = Arrays.asList("RED", "BLUE", "GREEN", "YELLOW", "ORANGE", "PURPLE", "PINK", "CYAN", "LIME", "GRAY", "MAGENTA", "BLACK", "WHITE");
        int idx = colors.indexOf((switch (piece) {
            case "chestplate" -> team.getChestplateColor();
            case "leggings" -> team.getLeggingsColor();
            case "boots" -> team.getBootsColor();
            default -> "WHITE";
        }).toUpperCase());
        if (idx < 0) {
            idx = 0;
        }
        String nextColor = colors.get((idx + 1) % colors.size());
        if (piece.equals("chestplate") && nextColor.equalsIgnoreCase("WHITE")) {
            idx = (idx + 1) % colors.size();
            nextColor = colors.get(idx);
        }
        switch (piece) {
            case "chestplate": {
                team.setChestplateColor(nextColor);
                break;
            }
            case "leggings": {
                team.setLeggingsColor(nextColor);
                break;
            }
            case "boots": {
                team.setBootsColor(nextColor);
            }
        }
    }

    public void showLeaderboard(Player player) {
        ArrayList<ProAmTeam> sorted = new ArrayList<>(this.teams.values());
        sorted.sort((a, b) -> Integer.compare(b.getElo(), a.getElo()));
        List<ProAmTeam> top = sorted.size() > 10 ? sorted.subList(0, 10) : sorted;
        GUI gui = new GUI("Pro Am Leaderboard", 3, false);
        int slot = 0;
        for (ProAmTeam t : top) {
            String abbr = t.getName().length() >= 3 ? t.getName().substring(0, 3).toUpperCase() : t.getName().toUpperCase();
            String leaderName = Bukkit.getOfflinePlayer(t.getLeader()).getName();
            String line = "§6#" + (slot + 1) + " §e" + t.getName() + " - " + leaderName + " - [" + t.getElo() + " Elo]";
            gui.addButton(new ItemButton(slot, Items.get(Component.text(line), Material.PAPER), btn -> {
            }));
            if (++slot < 27) continue;
            break;
        }
        gui.openInventory(player);
    }

    public void updateLeaderboardHologram() {
        ArrayList<ProAmTeam> sorted = new ArrayList<>(this.teams.values());
        sorted.sort((a, b) -> Integer.compare(b.getElo(), a.getElo()));
        List<ProAmTeam> top = sorted.size() > 10 ? sorted.subList(0, 10) : sorted;
        ArrayList<String> lines = new ArrayList<>();
        lines.add("§6Pro Am Leaderboard");
        lines.add("§eTop 10 Teams by Elo:");
        int rank = 1;
        for (ProAmTeam proAmTeam : top) {
            String leaderName = Bukkit.getOfflinePlayer(proAmTeam.getLeader()).getName();
            lines.add(rank + ". " + proAmTeam.getName() + " - " + leaderName + " - [" + proAmTeam.getElo() + " Elo]");
            ++rank;
        }
        Bukkit.getLogger().info("[ProAm] Updating Leaderboard Hologram...");
        for (String string : lines) {
            Bukkit.getLogger().info(string);
        }
    }

    private int fromName(String colorName) {
        String c;
        return switch (c = colorName.toUpperCase()) {
            case "RED" -> 0xFF0000;
            case "BLUE" -> 255;
            case "GREEN" -> 65280;
            case "YELLOW" -> 0xFFFF00;
            case "ORANGE" -> 16753920;
            case "PURPLE" -> 0x800080;
            case "PINK" -> 16761035;
            case "CYAN" -> 65535;
            case "LIME" -> 3329330;
            case "GRAY" -> 0x808080;
            case "MAGENTA" -> 0xFF00FF;
            case "BLACK" -> 0;
            default -> 0xFFFFFF;
        };
    }

    public void cancelQueue(ProAmTeam team, int mode, Player player) {
        if (team.getQueuedPlayers(mode).contains(player.getUniqueId())) {
            team.getQueuedPlayers(mode).remove(player.getUniqueId());
            player.sendMessage(Component.text("You have been removed from the " + mode + "v" + mode + " queue.", NamedTextColor.RED));
        }
    }
}

