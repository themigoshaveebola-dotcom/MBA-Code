/*
 * Decompiled with CFR 0.152.
 *
 * Could not load the following classes:
 *  net.kyori.adventure.text.Component
 *  net.kyori.adventure.text.TextComponent
 *  net.kyori.adventure.text.format.TextColor
 *  org.bukkit.Bukkit
 *  org.bukkit.Location
 *  org.bukkit.Material
 *  org.bukkit.Particle
 *  org.bukkit.Particle$DustOptions
 *  org.bukkit.entity.Player
 *  org.bukkit.inventory.ItemStack
 *  org.bukkit.inventory.meta.ItemMeta
 *  org.bukkit.plugin.Plugin
 */
package me.x_tias.partix.plugin.cosmetics;

import me.x_tias.partix.Partix;
import me.x_tias.partix.util.Colour;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;
import java.util.Random;

public class CosmeticParticle
        extends CosmeticHolder {
    public final CosmeticRarity rarity;
    private final ParticleSet[] particles;
    private final Material gui;
    private final String name;
    private final String permission;

    public CosmeticParticle(String permission, Material gui, String name, CosmeticRarity rarity, ParticleSet... set) {
        super(name, permission, gui, rarity, name.toLowerCase());
        this.permission = permission;
        this.gui = gui != null ? gui : Material.BARRIER;
        this.rarity = rarity;
        this.particles = set.length > 0 ? set : null;
        this.name = name;
    }

    public void trail(Location l) {
        if (this.particles != null) {
            for (int i = 0; i < 3; ++i) {
                ParticleSet random = this.particles[new Random().nextInt(this.particles.length)];
                if (random.getParticle() == null) continue;
                if (random.getDustOptions() != null) {
                    l.getWorld().spawnParticle(random.getParticle(), l, 1, 0.125, 0.125, 0.125, 0.0, (Object) random.getDustOptions(), false);
                    continue;
                }
                l.getWorld().spawnParticle(random.getParticle(), l, 1, 0.125, 0.125, 0.125, 0.0, null, false);
            }
        }
    }

    public void celebrate(Player player) {
        Location l = player.getLocation();
        if (this.particles != null) {
            for (int i = 0; i < 5; ++i) {
                ParticleSet random = this.particles[new Random().nextInt(this.particles.length)];
                if (random.getParticle() == null) continue;
                Particle.DustOptions dustOptions = new Particle.DustOptions(random.getDustOptions().getColor(), 2.0f);
                l.getWorld().spawnParticle(random.getParticle(), l, 30, 1.5, 2.25, 1.5, 0.0, (Object) dustOptions, false);
            }
        }
    }

    public void explode(Location l, int intensity) {
        if (this.particles != null) {
            for (int i = 0; i < intensity; ++i) {
                int finalI = i;
                Bukkit.getScheduler().runTaskLater(Partix.getInstance(), () -> {
                    for (ParticleSet p : this.particles) {
                        if (p.getParticle() == null) continue;
                        double size = 0.4 + 0.35 * (double) finalI + (Math.random() - Math.random()) / 2.0;
                        int count = (int) ((1.0 + size) * 7.0);
                        if (p.getDustOptions() != null) {
                            Particle.DustOptions dustOptions = new Particle.DustOptions(p.getDustOptions().getColor(), 5.0f);
                            l.getWorld().spawnParticle(p.getParticle(), l, count, size, size, size, 0.0, (Object) dustOptions, false);
                            continue;
                        }
                        l.getWorld().spawnParticle(p.getParticle(), l, count, size, size, size, 0.0, null, false);
                    }
                }, Math.max(1, i * 3));
            }
        }
    }

    public void largeExplosion(Location l) {
        this.explode(l, 7);
    }

    public void mediumExplosion(Location l) {
        this.explode(l, 6);
    }

    public void smallExplosion(Location l) {
        this.explode(l, 5);
    }

    @Override
    public ItemStack getGUIItem() {
        ItemStack itemStack = new ItemStack(this.gui);
        ItemMeta itemMeta = itemStack.getItemMeta();
        TextComponent colors = Component.empty();
        if (this.particles != null) {
            for (ParticleSet set : this.particles) {
                if (set == null || set.getParticle() == null) continue;
                colors = set.getDustOptions() != null ? colors.append(Component.text("■").color(TextColor.color(set.getDustOptions().getColor().asRGB()))) : colors.append(this.getParticleSymbol(set.getParticle()));
            }
        }
        itemMeta.displayName(Component.text(this.rarity.getTitle() + " " + this.name));
        itemMeta.lore(List.of(Component.text("§r§8Cosmetic").color(Colour.border()), Component.text("   "), Component.text("§r§7Particles (").append(colors).append(Component.text("§7)")), Component.text("§r§ePrice: §6" + this.rarity.getCost() + " Coins")));
        itemStack.setItemMeta(itemMeta);
        return itemStack;
    }

    private Component getParticleSymbol(Particle particle) {
        int color = switch (particle) {
            case Particle.TOTEM_OF_UNDYING -> 10354525;
            case Particle.SOUL_FIRE_FLAME -> 6160378;
            case Particle.FLAME -> 16562241;
            case Particle.ENCHANT -> 0xFFDDFA;
            case Particle.NAUTILUS -> 2705043;
            case Particle.NOTE -> 896512;
            case Particle.HEART -> 12788736;
            case Particle.SOUL -> 4926727;
            default -> 0xFFFFFF;
        };
        return Component.text("♦").color(TextColor.color(color));
    }
}

