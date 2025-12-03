/*
 * Decompiled with CFR 0.152.
 *
 * Could not load the following classes:
 *  net.kyori.adventure.text.Component
 *  net.kyori.adventure.text.TextComponent
 *  net.kyori.adventure.text.format.TextColor
 *  org.bukkit.Bukkit
 *  org.bukkit.GameMode
 *  org.bukkit.entity.Player
 *  org.bukkit.potion.PotionEffect
 *  org.bukkit.potion.PotionEffectType
 *  org.bukkit.scoreboard.Scoreboard
 *  org.bukkit.scoreboard.Team
 */
package me.x_tias.partix.plugin.athlete;

import lombok.Getter;
import lombok.Setter;
import me.x_tias.partix.database.PlayerDb;
import me.x_tias.partix.mini.factories.Hub;
import me.x_tias.partix.plugin.cosmetics.CosmeticParticle;
import me.x_tias.partix.plugin.cosmetics.CosmeticSound;
import me.x_tias.partix.plugin.cosmetics.Cosmetics;
import me.x_tias.partix.server.Place;
import me.x_tias.partix.server.rank.Ranks;
import me.x_tias.partix.util.Colour;
import me.x_tias.partix.util.Message;
import me.x_tias.partix.util.Text;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.Map;

@Getter
public class Athlete {
    private final Player player;
    @Setter
    private int party;
    @Setter
    private Place place;
    @Setter
    private RenderType renderType;
    private CosmeticParticle explosion;
    private CosmeticParticle trail;
    private CosmeticSound greenSound;
    private CosmeticSound winSong;

    public Athlete(Player p) {
        this.player = p;
        this.party = -1;
        this.updateCosmetics();
        this.updateRank();
        this.renderType = RenderType.PARTICLE;
        Hub.hub.join(this);
    }

    public void setSpectator(boolean b) {
        this.player.setGameMode(GameMode.ADVENTURE);
        if (this.player.isOnline()) {
            if (b) {
                this.player.setAllowFlight(true);
                this.player.setFlying(true);
                this.player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, Integer.MAX_VALUE, 255, true, false));
            } else {
                this.player.setFlying(false);
                this.player.setAllowFlight(false);
                this.player.removePotionEffect(PotionEffectType.INVISIBILITY);
            }
        }
    }

    public boolean isSpectating() {
        return this.player.getAllowFlight() && this.player.hasPotionEffect(PotionEffectType.INVISIBILITY);
    }

    public void setExplosion(int index) {
        this.explosion = Cosmetics.explosions.get(index);
        PlayerDb.set(this.player.getUniqueId(), PlayerDb.Stat.EXPLOSION, index);
    }

    public void setTrail(int index) {
        this.trail = Cosmetics.trails.get(index);
        PlayerDb.set(this.player.getUniqueId(), PlayerDb.Stat.TRAIL, index);
    }

    public void setGreenSound(int soundKey) {
        this.greenSound = Cosmetics.greenSounds.get(soundKey);
    }

    public void setGreenSound(CosmeticSound sound) {
        this.greenSound = sound;
    }

    public void equipGreenSound(CosmeticSound sound) {
        if (sound != null && !this.player.hasPermission("cosmetic.green." + sound.getName().toLowerCase().replace(" ", "_"))) {
            this.player.sendMessage(Component.text("You do not have permission to equip this Green Sound.").color(Colour.deny()));
            return;
        }
        this.greenSound = sound;
        int soundSlot = sound == null ? 0 : Cosmetics.greenSounds.entrySet().stream().filter(entry -> entry.getValue().equals(sound)).map(Map.Entry::getKey).findFirst().orElse(0);
        PlayerDb.set(this.player.getUniqueId(), PlayerDb.Stat.GREEN_SOUND, soundSlot);
        String message = sound == null ? "No Green Sound equipped." : "Green Sound equipped: " + sound.getName();
        this.player.sendMessage(Component.text(message).color(Colour.allow()));
    }

    public void updateCosmetics() {
        PlayerDb.get(this.player.getUniqueId(), PlayerDb.Stat.EXPLOSION).thenAccept(explosion -> this.explosion = Cosmetics.explosions.get(explosion));
        PlayerDb.get(this.player.getUniqueId(), PlayerDb.Stat.TRAIL).thenAccept(trail -> this.trail = Cosmetics.trails.get(trail));
        PlayerDb.get(this.player.getUniqueId(), PlayerDb.Stat.WINSONG).thenAccept(winsong -> this.winSong = Cosmetics.winSongs.get(winsong));
        PlayerDb.get(this.player.getUniqueId(), PlayerDb.Stat.GREEN_SOUND).thenAccept(greenSoundId -> this.greenSound = Cosmetics.greenSounds.getOrDefault(greenSoundId, Cosmetics.greenSounds.get(0)));
    }

    public void updateRank() {
        Scoreboard s = Bukkit.getScoreboardManager().getMainScoreboard();
        this.player.playerListName(this.getName());
        for (Team team : s.getTeams()) {
            if (!team.hasEntry(this.player.getName())) continue;
            team.removeEntry(this.player.getName());
        }
        if (this.player.hasPermission("rank.admin")) {
            Ranks.getAdmin().addEntry(this.player.getName());
            this.player.sendMessage("✅ Added to Admin team!");
        } else if (this.player.hasPermission("rank.mod")) {
            Ranks.getMod().addEntry(this.player.getName());
            this.player.sendMessage("✅ Added to Mod team!");
        } else if (this.player.hasPermission("rank.media")) {
            Ranks.getMedia().addEntry(this.player.getName());
            this.player.sendMessage("✅ Added to Media team!");
        } else if (this.player.hasPermission("rank.pro")) {
            Ranks.getPro().addEntry(this.player.getName());
            this.player.sendMessage("✅ Added to PRO team!");
        } else if (this.player.hasPermission("rank.vip")) {
            Ranks.getVip().addEntry(this.player.getName());
            this.player.sendMessage("✅ Added to VIP team!");
        } else {
            Ranks.getDefault().addEntry(this.player.getName());
            this.player.sendMessage("✅ Added to Default team!");
        }
        Bukkit.getLogger().info("[Ranks] " + this.player.getName() + " has been assigned to a team!");
    }

    public Component getName() {
        if (this.player.hasPermission("rank.admin")) {
            return Text.gradient("ADMIN " + this.player.getName(), TextColor.fromHexString("#c24528"), TextColor.fromHexString("#c8682a"), false).append(Component.text(" ", Colour.adminText()));
        }
        if (this.player.hasPermission("rank.mod")) {
            return Text.gradient("MOD " + this.player.getName(), TextColor.fromHexString("#1478cc"), TextColor.fromHexString("#47b0b4"), false).append(Component.text(" ", Colour.adminText()));
        }
        if (this.player.hasPermission("rank.pro")) {
            return Text.gradient("PRO " + this.player.getName(), TextColor.fromHexString("#FFD700"), TextColor.fromHexString("#FF8C00"), false).append(Component.text(" ", Colour.premiumText()));
        }
        if (this.player.hasPermission("rank.vip")) {
            return Text.gradient("VIP " + this.player.getName(), TextColor.fromHexString("#00FF00"), TextColor.fromHexString("#008000"), false).append(Component.text(" ", Colour.vipText()));
        }
        return Component.text(this.player.getName()).color(TextColor.color(0xBEBEBE)).append(Component.text(" ", Colour.text()));
    }

    public void giveCoins(int amount, boolean multiply) {
        int amt = amount;
        if (multiply) {
            if (this.player.hasPermission("rank.pro")) {
                amt = amount * 7;
            } else if (this.player.hasPermission("rank.vip")) {
                amt = amount * 4;
            }
        }
        PlayerDb.add(this.player.getUniqueId(), PlayerDb.Stat.COINS, amt);
        this.player.sendMessage(Message.receiveCoins(amt));
    }

}

