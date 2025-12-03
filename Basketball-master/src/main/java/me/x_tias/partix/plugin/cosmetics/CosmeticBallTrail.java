/*
 * Decompiled with CFR 0.152.
 *
 * Could not load the following classes:
 *  net.kyori.adventure.text.Component
 *  org.bukkit.Color
 *  org.bukkit.Location
 *  org.bukkit.Material
 *  org.bukkit.Particle
 *  org.bukkit.Particle$DustOptions
 *  org.bukkit.World
 *  org.bukkit.inventory.ItemStack
 *  org.bukkit.inventory.meta.ItemMeta
 */
package me.x_tias.partix.plugin.cosmetics;

import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public class CosmeticBallTrail
        extends CosmeticHolder {
    private final String effectIdentifier;

    public CosmeticBallTrail(String permission, Material gui, String name, CosmeticRarity rarity, String effectIdentifier) {
        super(name, permission, gui, rarity, name.toLowerCase());
        this.effectIdentifier = effectIdentifier;
    }

    public void applyEffect(Location location) {
        World world = location.getWorld();
        if (world == null) {
            return;
        }
        switch (this.effectIdentifier) {
            case "balltrail.stars": {
                world.spawnParticle(Particle.FIREWORK, location, 5, 0.15, 0.15, 0.15, 0.02);
                break;
            }
            case "balltrail.fire": {
                world.spawnParticle(Particle.FLAME, location, 5, 0.15, 0.15, 0.15, 0.02);
                break;
            }
            case "balltrail.water": {
                world.spawnParticle(Particle.UNDERWATER, location, 7, 0.15, 0.15, 0.15, 0.05);
                break;
            }
            case "balltrail.sparkle": {
                world.spawnParticle(Particle.END_ROD, location, 6, 0.15, 0.15, 0.15, 0.02);
                break;
            }
            case "balltrail.portal": {
                world.spawnParticle(Particle.PORTAL, location, 12, 0.3, 0.3, 0.3, 0.03);
                break;
            }
            case "balltrail.bubble": {
                world.spawnParticle(Particle.BUBBLE_POP, location, 10, 0.2, 0.2, 0.2, 0.03);
                break;
            }
            case "balltrail.rainbow": {
                Color[] rainbowColors;
                for (Color c : new Color[]{Color.fromRGB(255, 0, 0), Color.fromRGB(255, 127, 0), Color.fromRGB(255, 255, 0), Color.fromRGB(0, 255, 0), Color.fromRGB(0, 0, 255), Color.fromRGB(75, 0, 130), Color.fromRGB(148, 0, 211)}) {
                    Particle.DustOptions options = new Particle.DustOptions(c, 0.7f);
                    world.spawnParticle(Particle.DUST, location, 4, 0.15, 0.15, 0.15, (Object) options);
                }
                break;
            }
            case "balltrail.meteor": {
                world.spawnParticle(Particle.FLAME, location, 5, 0.25, 0.25, 0.25, 0.03);
                world.spawnParticle(Particle.SMOKE, location, 3, 0.25, 0.25, 0.25, 0.03);
                break;
            }
            case "balltrail.smoke": {
                world.spawnParticle(Particle.LARGE_SMOKE, location, 1, 0.03, 0.03, 0.03, 0.005);
                break;
            }
            case "balltrail.vip": {
                Particle.DustOptions vipOptions = new Particle.DustOptions(Color.fromRGB(255, 215, 0), 1.0f);
                world.spawnParticle(Particle.DUST, location, 2, 0.1, 0.1, 0.1, (Object) vipOptions);
                world.spawnParticle(Particle.END_ROD, location, 1, 0.2, 0.2, 0.2, 0.01);
                break;
            }
            case "balltrail.pro": {
                world.spawnParticle(Particle.ENCHANT, location, 2, 0.1, 0.1, 0.1);
                world.spawnParticle(Particle.END_ROD, location, 2, 0.1, 0.1, 0.1, 0.01);
                break;
            }
            case "balltrail.dust": {
                Particle.DustOptions dustOptions = new Particle.DustOptions(Color.fromRGB(120, 120, 120), 1.0f);
                world.spawnParticle(Particle.DUST, location, 8, 0.15, 0.15, 0.15, (Object) dustOptions);
                break;
            }
            case "balltrail.electric": {
                world.spawnParticle(Particle.CRIT, location, 6, 0.2, 0.2, 0.2, 0.02);
                break;
            }
            case "balltrail.ice": {
                Particle.DustOptions iceOptions = new Particle.DustOptions(Color.fromRGB(150, 220, 255), 1.0f);
                world.spawnParticle(Particle.DUST, location, 8, 0.2, 0.2, 0.2, (Object) iceOptions);
                break;
            }
            case "balltrail.red": {
                Particle.DustOptions redOptions = new Particle.DustOptions(Color.fromRGB(255, 0, 0), 1.0f);
                world.spawnParticle(Particle.DUST, location, 5, 0.15, 0.2, 0.15, (Object) redOptions);
                break;
            }
            case "balltrail.orange": {
                Particle.DustOptions orangeOptions = new Particle.DustOptions(Color.fromRGB(255, 127, 0), 1.0f);
                world.spawnParticle(Particle.DUST, location, 5, 0.15, 0.2, 0.15, (Object) orangeOptions);
                break;
            }
            case "balltrail.yellow": {
                Particle.DustOptions yellowOptions = new Particle.DustOptions(Color.fromRGB(255, 255, 0), 1.0f);
                world.spawnParticle(Particle.DUST, location, 5, 0.15, 0.2, 0.15, (Object) yellowOptions);
                break;
            }
            case "balltrail.green": {
                Particle.DustOptions greenOptions = new Particle.DustOptions(Color.fromRGB(0, 255, 0), 1.0f);
                world.spawnParticle(Particle.DUST, location, 5, 0.15, 0.2, 0.15, (Object) greenOptions);
                break;
            }
            case "balltrail.blue": {
                Particle.DustOptions blueOptions = new Particle.DustOptions(Color.fromRGB(0, 0, 255), 1.0f);
                world.spawnParticle(Particle.DUST, location, 5, 0.2, 0.15, 0.15, (Object) blueOptions);
                break;
            }
            case "balltrail.america": {
                Color[] americaColors;
                for (Color c : new Color[]{Color.fromRGB(255, 0, 0), Color.fromRGB(255, 255, 255), Color.fromRGB(0, 0, 255)}) {
                    Particle.DustOptions options = new Particle.DustOptions(c, 1.0f);
                    world.spawnParticle(Particle.DUST, location, 3, 0.1, 0.1, 0.1, (Object) options);
                }
                break;
            }
            case "balltrail.canada": {
                Color[] canadaColors;
                for (Color c : new Color[]{Color.fromRGB(255, 0, 0), Color.fromRGB(255, 255, 255)}) {
                    Particle.DustOptions options = new Particle.DustOptions(c, 1.0f);
                    world.spawnParticle(Particle.DUST, location, 3, 0.1, 0.1, 0.1, (Object) options);
                }
                break;
            }
            case "balltrail.xp": {
                Particle.DustOptions xpOptions = new Particle.DustOptions(Color.fromRGB(85, 255, 85), 1.0f);
                world.spawnParticle(Particle.DUST, location, 8, 0.1, 0.1, 0.1, (Object) xpOptions);
                break;
            }
            case "balltrail.vice": {
                Color[] viceColors;
                for (Color c : new Color[]{Color.fromRGB(255, 105, 180), Color.fromRGB(0, 255, 255)}) {
                    Particle.DustOptions viceOptions = new Particle.DustOptions(c, 1.0f);
                    world.spawnParticle(Particle.DUST, location, 3, 0.2, 0.2, 0.2, (Object) viceOptions);
                }
                break;
            }
            case "balltrail.black": {
                Particle.DustOptions blackOptions = new Particle.DustOptions(Color.fromRGB(0, 0, 0), 1.0f);
                world.spawnParticle(Particle.DUST, location, 5, 0.15, 0.15, 0.15, (Object) blackOptions);
                break;
            }
            case "balltrail.mystic": {
                world.spawnParticle(Particle.ENTITY_EFFECT, location, 8, 0.1, 0.1, 0.1, 0.02);
                break;
            }
            case "balltrail.blueorange": {
                Color[] blueOrangeColors;
                for (Color c : new Color[]{Color.fromRGB(0, 0, 255), Color.fromRGB(255, 165, 0)}) {
                    Particle.DustOptions options = new Particle.DustOptions(c, 1.0f);
                    world.spawnParticle(Particle.DUST, location, 3, 0.1, 0.1, 0.1, (Object) options);
                }
                break;
            }
            case "balltrail.phantom": {
                world.spawnParticle(Particle.SMOKE, location, 8, 0.2, 0.2, 0.2, 0.04);
                break;
            }
            default: {
                break;
            }
            case "balltrail.lime": {
                Particle.DustOptions limeOptions = new Particle.DustOptions(Color.fromRGB(191, 255, 0), 1.0f);
                world.spawnParticle(Particle.DUST, location, 5, 0.15, 0.2, 0.15, (Object) limeOptions);
                break;
            }
            case "balltrail.forest": {
                Particle.DustOptions forestOptions = new Particle.DustOptions(Color.fromRGB(34, 139, 34), 1.0f);
                world.spawnParticle(Particle.DUST, location, 5, 0.15, 0.2, 0.15, (Object) forestOptions);
                break;
            }
            case "balltrail.neon": {
                Particle.DustOptions neonOptions = new Particle.DustOptions(Color.fromRGB(57, 255, 20), 1.0f);
                world.spawnParticle(Particle.DUST, location, 5, 0.15, 0.2, 0.15, (Object) neonOptions);
                break;
            }
            case "balltrail.note": {
                double noteValue = 0.5;
                world.spawnParticle(Particle.NOTE, location, 4, 0.15, 0.15, 0.15, 1.0);
                break;
            }
            case "balltrail.witch": {
                world.spawnParticle(Particle.WITCH, location, 4, 0.15, 0.15, 0.15, 0.02);
                break;
            }
            case "balltrail.dragon": {
                world.spawnParticle(Particle.DRAGON_BREATH, location, 2, 0.2, 0.2, 0.2, 0.03);
                break;
            }
            case "balltrail.happy": {
                world.spawnParticle(Particle.HAPPY_VILLAGER, location, 8, 0.2, 0.2, 0.2, 0.03);
            }
        }
    }

    @Override
    public ItemStack getGUIItem() {
        ItemStack item = new ItemStack(this.getMaterial());
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(this.getRarity().getTitle() + " " + this.getName()));
        meta.lore(List.of(Component.text("§7Effect: " + this.effectIdentifier), Component.text("§7Price: §6" + this.getRarity().getCost() + " Coins")));
        item.setItemMeta(meta);
        return item;
    }
}

