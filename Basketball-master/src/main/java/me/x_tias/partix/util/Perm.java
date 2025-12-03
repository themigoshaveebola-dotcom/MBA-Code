/*
 * Decompiled with CFR 0.152.
 *
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.command.CommandSender
 *  org.bukkit.entity.Player
 */
package me.x_tias.partix.util;

import me.x_tias.partix.plugin.athlete.Athlete;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class Perm {
    public static void add(Athlete athlete, String permission) {
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "lp user " + athlete.getPlayer().getName() + " permission set " + permission + " true");
    }

    public static void add(Player player, String permission) {
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "lp user " + player.getName() + " permission set " + permission + " true");
    }
}

