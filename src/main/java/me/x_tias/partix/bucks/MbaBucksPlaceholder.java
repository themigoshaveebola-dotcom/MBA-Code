/*
 * Decompiled with CFR 0.152.
 *
 * Could not load the following classes:
 *  me.clip.placeholderapi.expansion.PlaceholderExpansion
 *  org.bukkit.entity.Player
 *  org.jetbrains.annotations.NotNull
 *  org.jetbrains.annotations.Nullable
 */
package me.x_tias.partix.bucks;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class MbaBucksPlaceholder extends PlaceholderExpansion {
    @NotNull
    public String getIdentifier() {
        return "mbabucks";
    }

    @NotNull
    public String getAuthor() {
        return "x_tias";
    }

    @NotNull
    public String getVersion() {
        return "1.0";
    }

    public boolean persist() {
        return true;
    }

    public boolean register() {
        return super.register();
    }

    @Nullable
    public String onPlaceholderRequest(Player player, @NotNull String identifier) {
        if (identifier.startsWith("ranked_")) {
            try {
                int rank = Integer.parseInt(identifier.substring("ranked_".length()));
                return MbaBucksLeaderboard.getLine(rank);
            } catch (NumberFormatException e) {
                return "§cInvalid rank format!";
            }
        }
        return "";
    }
}

