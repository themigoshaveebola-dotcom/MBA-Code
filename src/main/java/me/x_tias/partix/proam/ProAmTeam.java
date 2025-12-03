/*
 * Decompiled with CFR 0.152.
 *
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.OfflinePlayer
 *  org.bukkit.configuration.ConfigurationSection
 *  org.bukkit.configuration.file.YamlConfiguration
 *  org.bukkit.entity.Player
 */
package me.x_tias.partix.proam;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.util.*;

public class ProAmTeam {
    @Getter
    private final String name;
    @Getter
    private final UUID leader;
    @Getter
    private final Set<UUID> members = new HashSet<>();
    private final Set<UUID> pendingInvites = new HashSet<>();
    private final Map<Integer, Set<UUID>> queuedPlayers = new HashMap<>();
    @Setter
    @Getter
    private int elo = 1000;
    @Getter
    private String chestplateColor = "RED";
    @Setter
    @Getter
    private String leggingsColor = "WHITE";
    @Setter
    @Getter
    private String bootsColor = "WHITE";
    @Setter
    @Getter
    private boolean queued = false;
    @Setter
    @Getter
    private int queuedType = 0;

    public ProAmTeam(String name, UUID leader) {
        this.name = name;
        this.leader = leader;
        this.members.add(leader);
        this.queuedPlayers.put(1, new HashSet<>());
        this.queuedPlayers.put(3, new HashSet<>());
        this.queuedPlayers.put(4, new HashSet<>());
    }

    public ProAmTeam(String name, YamlConfiguration config) {
        this.name = name;
        ConfigurationSection section = config.getConfigurationSection(name);
        this.leader = UUID.fromString(section.getString("leader"));
        this.elo = section.getInt("elo", 1000);
        for (String uuid : section.getStringList("members")) {
            this.members.add(UUID.fromString(uuid));
        }
        for (String uuid : section.getStringList("pendingInvites")) {
            this.pendingInvites.add(UUID.fromString(uuid));
        }
        if (section.contains("chestplateColor")) {
            this.chestplateColor = section.getString("chestplateColor");
        }
        if (section.contains("leggingsColor")) {
            this.leggingsColor = section.getString("leggingsColor");
        }
        if (section.contains("bootsColor")) {
            this.bootsColor = section.getString("bootsColor");
        }
        this.queuedPlayers.put(1, new HashSet<>());
        this.queuedPlayers.put(3, new HashSet<>());
        this.queuedPlayers.put(4, new HashSet<>());
    }

    public void save(YamlConfiguration config) {
        ConfigurationSection section = config.createSection(this.name);
        section.set("leader", this.leader.toString());
        section.set("elo", this.elo);
        ArrayList<String> memberStrings = new ArrayList<>();
        for (UUID member : this.members) {
            memberStrings.add(member.toString());
        }
        section.set("members", memberStrings);
        ArrayList<String> inviteStrings = new ArrayList<>();
        for (UUID invite : this.pendingInvites) {
            inviteStrings.add(invite.toString());
        }
        section.set("pendingInvites", inviteStrings);
        section.set("chestplateColor", this.chestplateColor);
        section.set("leggingsColor", this.leggingsColor);
        section.set("bootsColor", this.bootsColor);
    }

    public boolean isLeader(UUID uuid) {
        return this.leader.equals(uuid);
    }

    public boolean isFull(int requiredSize) {
        return this.members.size() >= requiredSize;
    }

    public void invite(UUID player) {
        this.pendingInvites.add(player);
    }

    public boolean hasInvite(UUID player) {
        return this.pendingInvites.contains(player);
    }

    public void addMember(UUID player) {
        this.pendingInvites.remove(player);
        this.members.add(player);
    }

    public void kick(Player player) {
        this.kick(player.getUniqueId());
    }

    public void kick(UUID playerUuid) {
        this.members.remove(playerUuid);
        this.pendingInvites.remove(playerUuid);
        if (this.queuedPlayers.containsKey(1)) {
            this.queuedPlayers.get(1).remove(playerUuid);
        }
        if (this.queuedPlayers.containsKey(3)) {
            this.queuedPlayers.get(3).remove(playerUuid);
        }
        if (this.queuedPlayers.containsKey(4)) {
            this.queuedPlayers.get(4).remove(playerUuid);
        }
    }

    public void addElo(int amount) {
        this.elo += amount;
    }

    public void setChestplateColor(String chestplateColor) {
        this.chestplateColor = chestplateColor.equalsIgnoreCase("WHITE") ? "RED" : chestplateColor;
    }

    public void addToQueue(int mode, UUID playerUuid) {
        if (this.queuedPlayers.containsKey(mode)) {
            this.queuedPlayers.get(mode).add(playerUuid);
        }
    }

    public void removeFromQueue(int mode, UUID playerUuid) {
        if (this.queuedPlayers.containsKey(mode)) {
            this.queuedPlayers.get(mode).remove(playerUuid);
        }
    }

    public boolean hasQueued(int mode) {
        return this.queuedPlayers.containsKey(mode) && !this.queuedPlayers.get(mode).isEmpty();
    }

    public int getQueueCount(int mode) {
        return this.queuedPlayers.containsKey(mode) ? this.queuedPlayers.get(mode).size() : 0;
    }

    public List<String> getQueuedMemberNames(int mode) {
        ArrayList<String> names = new ArrayList<>();
        if (this.queuedPlayers.containsKey(mode)) {
            for (UUID uuid : this.queuedPlayers.get(mode)) {
                OfflinePlayer player = Bukkit.getOfflinePlayer(uuid);
                names.add(player.getName() != null ? player.getName() : "Unknown");
            }
        }
        return names;
    }

    public void clearQueue(int mode) {
        if (this.queuedPlayers.containsKey(mode)) {
            this.queuedPlayers.get(mode).clear();
        }
    }

    public List<String> getMemberNames() {
        ArrayList<String> names = new ArrayList<>();
        for (UUID uuid : this.members) {
            OfflinePlayer player = Bukkit.getOfflinePlayer(uuid);
            names.add(player.getName() != null ? player.getName() : "Unknown");
        }
        return names;
    }

    public List<Player> getOnlineMembers() {
        ArrayList<Player> online = new ArrayList<>();
        for (UUID uuid : this.members) {
            Player p = Bukkit.getPlayer(uuid);
            if (p == null) continue;
            online.add(p);
        }
        return online;
    }

    public void removeMember(UUID uuid) {
        this.members.remove(uuid);
    }

    public Set<UUID> getQueuedPlayers(int mode) {
        return this.queuedPlayers.getOrDefault(mode, new HashSet<>());
    }
}

