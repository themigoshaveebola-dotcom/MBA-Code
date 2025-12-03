/*
 * Decompiled with CFR 0.152.
 *
 * Could not load the following classes:
 *  net.kyori.adventure.text.Component
 *  net.kyori.adventure.title.Title$Times
 *  net.kyori.adventure.title.TitlePart
 *  org.bukkit.Bukkit
 *  org.bukkit.Sound
 *  org.bukkit.SoundCategory
 *  org.bukkit.boss.BarColor
 *  org.bukkit.boss.BarFlag
 *  org.bukkit.boss.BarStyle
 *  org.bukkit.boss.BossBar
 *  org.bukkit.entity.Player
 *  org.bukkit.inventory.ItemStack
 *  org.bukkit.potion.PotionEffect
 *  org.bukkit.potion.PotionEffectType
 */
package me.x_tias.partix.server;

import lombok.Getter;
import me.x_tias.partix.plugin.athlete.Athlete;
import me.x_tias.partix.server.specific.Game;
import me.x_tias.partix.util.Message;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import net.kyori.adventure.title.TitlePart;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public abstract class Place {
    @Getter private final UUID uniqueId = UUID.randomUUID();
    private final BossBar bossBar = Bukkit.createBossBar("Loading..", BarColor.YELLOW, BarStyle.SOLID);
    private List<Athlete> players = new ArrayList<>();

    public Place() {
        PlaceLoader.create(this);
    }

    public abstract void onTick();

    public abstract void onJoin(Athlete... var1);

    public abstract void onQuit(Athlete... var1);

    public void join(Athlete... athletes) {
        if (this.players == null) {
            this.players = new ArrayList<>();
        }
        for (Athlete athlete : athletes) {
            if (athlete == null) continue;
            if (athlete.getPlace() != null) {
                athlete.getPlace().quit(athletes);
            }
            athlete.setPlace(this);
            Player player = athlete.getPlayer();
            this.sendMessage(Message.joinServer(player));
            this.bossBar.addPlayer(player);
            this.players.add(athlete);
            player.stopAllSounds();
            player.getActivePotionEffects().clear();
            for (PotionEffect effect : player.getActivePotionEffects()) {
                player.removePotionEffect(effect.getType());
            }
            player.getScoreboardTags().clear();
            player.getInventory().clear();
        }
        this.onJoin(athletes);
    }

    public void quit(Athlete... athletes) {
        for (Athlete athlete : athletes) {
            Player player = athlete.getPlayer();
            this.bossBar.removePlayer(player);
            this.players.remove(athlete);
            player.getActivePotionEffects().clear();
            athlete.setPlace(null);
            athlete.setSpectator(false);
            if (!(this instanceof Game)) continue;
            this.sendMessage(Message.quitServer(player));
        }
        this.onQuit(athletes);
    }

    public void updateBossBar(String title) {
        this.bossBar.setTitle(title);
    }

    public void updateBossBar(String title, double progress) {
        this.bossBar.setTitle(title);
        this.bossBar.setProgress(progress);
    }

    public List<Athlete> getAthletes() {
        ArrayList<Athlete> copy = new ArrayList<>(this.players);
        this.players = copy.stream().filter(athlete -> athlete.getPlayer().isOnline()).collect(Collectors.toList());
        return this.players;
    }

    public List<Player> getPlayers() {
        ArrayList<Athlete> copy = new ArrayList<>(this.players);
        return copy.stream().map(Athlete::getPlayer).collect(Collectors.toList());
    }

    public void sendMessage(Component c) {
        this.getPlayers().forEach(player -> player.sendMessage(c));
    }

    public void sendTitle(Component c) {
        this.players.forEach(a -> {
            Player player = a.getPlayer();
            player.sendTitlePart(TitlePart.TITLE, Component.text("  "));
            player.sendTitlePart(TitlePart.SUBTITLE, c);
            player.sendTitlePart(TitlePart.TIMES, Title.Times.times(Duration.ofMillis(350L), Duration.ofMillis(1450L), Duration.ofMillis(350L)));
        });
    }

    public void addPotionEffects(PotionEffect... effects) {
        this.getPlayers().forEach(player -> {
            for (PotionEffect effect : effects) {
                player.addPotionEffect(effect);
            }
        });
    }

    public void removePotionEffects(PotionEffectType... effectTypes) {
        this.getPlayers().forEach(player -> {
            for (PotionEffectType effect : effectTypes) {
                player.removePotionEffect(effect);
            }
        });
    }

    public void playSound(Sound sound, SoundCategory category, float volume, float pitch) {
        this.getPlayers().forEach(player -> player.playSound(player.getLocation(), sound, category, volume, pitch));
    }

    public abstract void clickItem(Player var1, ItemStack var2);
}

