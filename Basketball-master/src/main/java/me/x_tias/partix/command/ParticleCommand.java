/*
 * Decompiled with CFR 0.152.
 *
 * Could not load the following classes:
 *  org.bukkit.command.CommandSender
 *  org.bukkit.entity.Player
 */
package me.x_tias.partix.command;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.Default;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@CommandAlias(value = "particle")
public class ParticleCommand
        extends BaseCommand {
    @Default
    public void onParticleCommand(CommandSender sender) {
        if (sender instanceof Player player) {
            player.sendMessage("No.");
        }
    }
}

