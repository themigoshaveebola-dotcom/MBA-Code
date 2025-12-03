/*
 * Decompiled with CFR 0.152.
 *
 * Could not load the following classes:
 *  net.kyori.adventure.text.Component
 *  org.bukkit.Bukkit
 *  org.bukkit.entity.Player
 */
package me.x_tias.partix.plugin.party;

import me.x_tias.partix.plugin.athlete.Athlete;
import me.x_tias.partix.plugin.athlete.AthleteManager;
import me.x_tias.partix.util.Message;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class Party {
    public int id = -1;
    public UUID leader;
    public boolean locked;

    public Party(Player leader) {
        this.leader = leader.getUniqueId();
        this.locked = false;
    }

    public List<Athlete> toList() {
        return Bukkit.getOnlinePlayers().stream().filter(player -> AthleteManager.get(player.getUniqueId()).getParty() == this.id).map(player -> AthleteManager.get(player.getUniqueId())).toList();
    }

    public List<Player> toPlayerList() {
        return Bukkit.getOnlinePlayers().stream().filter(player -> AthleteManager.get(player.getUniqueId()).getParty() == this.id).collect(Collectors.toList());
    }

    public int count() {
        return this.toList().size();
    }

    public void sendMessage(String message) {
        Component c = Message.partyChat(message);
        this.toPlayerList().forEach(player -> player.sendMessage(c));
    }

    public void sendChat(Player sender, String message) {
        Component c = Message.partyChat(sender.getName(), message);
        this.toPlayerList().forEach(player -> player.sendMessage(c));
    }
}

