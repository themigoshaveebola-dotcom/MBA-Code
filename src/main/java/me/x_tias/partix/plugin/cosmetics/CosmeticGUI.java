/*
 * Decompiled with CFR 0.152.
 *
 * Could not load the following classes:
 *  net.kyori.adventure.text.Component
 *  org.bukkit.Material
 *  org.bukkit.enchantments.Enchantment
 *  org.bukkit.entity.Player
 *  org.bukkit.inventory.ItemFlag
 *  org.bukkit.inventory.ItemStack
 *  org.bukkit.inventory.meta.ItemMeta
 */
package me.x_tias.partix.plugin.cosmetics;

import me.x_tias.partix.Partix;
import me.x_tias.partix.database.PlayerDb;
import me.x_tias.partix.plugin.athlete.Athlete;
import me.x_tias.partix.plugin.athlete.AthleteManager;
import me.x_tias.partix.plugin.gui.GUI;
import me.x_tias.partix.plugin.gui.ItemButton;
import me.x_tias.partix.util.Colour;
import me.x_tias.partix.util.Items;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class CosmeticGUI {
    public CosmeticGUI(Player player) {
        ItemButton[] buttons = new ItemButton[27];
        for (int i = 0; i < buttons.length; ++i) {
            buttons[i] = new ItemButton(i, Items.get(Component.text(" "), Material.BLACK_STAINED_GLASS_PANE, 1, " "), p -> {
            });
        }
        buttons[10] = new ItemButton(10, Items.get(Component.text("Trails").color(Colour.partix()), Material.MELON_SEEDS), p -> this.trails(p, 1));
        buttons[12] = new ItemButton(12, Items.get(Component.text("Explosions").color(Colour.partix()), Material.GUNPOWDER), p -> this.explosions(p, 1));
        buttons[14] = new ItemButton(14, Items.get(Component.text("Green Sounds").color(Colour.partix()), Material.LIME_CONCRETE), p -> this.openGreenSoundMenu(p, 1));
        buttons[16] = new ItemButton(16, Items.get(Component.text("Ball Trails").color(Colour.partix()), Material.SLIME_BALL), p -> this.openBallTrailsMenu(p, 1));
        new GUI("Cosmetics Menu", 3, false, buttons).openInventory(player);
    }

    private void trails(Player player, int page) {
        final int[] index = new int[1];
        List<Map.Entry<Integer, CosmeticParticle>> availableTrails = Cosmetics.trails.entrySet().stream().filter(entry -> player.hasPermission(entry.getValue().getPermission())).toList();
        Athlete athlete = AthleteManager.get(player.getUniqueId());
        PlayerDb.get(player.getUniqueId(), PlayerDb.Stat.TRAIL).thenAccept(equippedTrailKey -> Bukkit.getScheduler().runTask(Partix.getInstance(), () -> {
            int maxItemsPerPage = 45;
            int totalPages = Math.max(1, (int) Math.ceil((double) availableTrails.size() / (double) maxItemsPerPage));
            ItemButton[] buttons = new ItemButton[54];
            for (int i = 0; i < maxItemsPerPage && (index[0] = (page - 1) * maxItemsPerPage + i) < availableTrails.size(); ++i) {
                Map.Entry<Integer, CosmeticParticle> entry2 = availableTrails.get(index[0]);
                int trailKey = entry2.getKey();
                CosmeticParticle trail = entry2.getValue();
                boolean isSelected = equippedTrailKey == trailKey;
                ItemStack item = trail.getGUIItem().clone();
                if (isSelected) {
                    ItemMeta meta = item.getItemMeta();
                    meta.setEnchantmentGlintOverride(true);
                    meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                    item.setItemMeta(meta);
                }
                buttons[i] = new ItemButton(i, item, p -> {
                    athlete.setTrail(trailKey);
                    PlayerDb.set(player.getUniqueId(), PlayerDb.Stat.TRAIL, trailKey);
                    p.sendMessage("§aTrail equipped: §6" + trail.getName());
                    this.trails(p, page);
                });
            }
            if (page > 1) {
                buttons[45] = new ItemButton(45, this.createArrow("§aPrevious Page"), p -> this.trails(p, page - 1));
            }
            if (page < totalPages) {
                buttons[53] = new ItemButton(53, this.createArrow("§aNext Page"), p -> this.trails(p, page + 1));
            }
            new GUI("Select Trail - Page " + page, 6, false, buttons).openInventory(player);
        }));
    }

    private void explosions(Player player, int page) {
        final int[] index = new int[1];
        List<Map.Entry<Integer, CosmeticParticle>> availableExplosions = Cosmetics.explosions.entrySet().stream().filter(entry -> player.hasPermission(entry.getValue().getPermission())).toList();
        Athlete athlete = AthleteManager.get(player.getUniqueId());
        PlayerDb.get(player.getUniqueId(), PlayerDb.Stat.EXPLOSION).thenAccept(equippedExplosionKey -> Bukkit.getScheduler().runTask(Partix.getInstance(), () -> {
            int maxItemsPerPage = 45;
            int totalPages = Math.max(1, (int) Math.ceil((double) availableExplosions.size() / (double) maxItemsPerPage));
            ItemButton[] buttons = new ItemButton[54];
            for (int i = 0; i < maxItemsPerPage && (index[0] = (page - 1) * maxItemsPerPage + i) < availableExplosions.size(); ++i) {
                Map.Entry<Integer, CosmeticParticle> entry2 = availableExplosions.get(index[0]);
                int explosionKey = entry2.getKey();
                CosmeticParticle explosion = entry2.getValue();
                boolean isSelected = equippedExplosionKey == explosionKey;
                ItemStack item = explosion.getGUIItem().clone();
                if (isSelected) {
                    ItemMeta meta = item.getItemMeta();
                    meta.setEnchantmentGlintOverride(true);
                    meta.setEnchantmentGlintOverride(true);
                    meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                    item.setItemMeta(meta);
                }
                buttons[i] = new ItemButton(i, item, p -> {
                    athlete.setExplosion(explosionKey);
                    PlayerDb.set(player.getUniqueId(), PlayerDb.Stat.EXPLOSION, explosionKey);
                    p.sendMessage("§aExplosion equipped: §6" + explosion.getName());
                    this.explosions(p, page);
                });
            }
            if (page > 1) {
                buttons[45] = new ItemButton(45, this.createArrow("§aPrevious Page"), p -> this.explosions(p, page - 1));
            }
            if (page < totalPages) {
                buttons[53] = new ItemButton(53, this.createArrow("§aNext Page"), p -> this.explosions(p, page + 1));
            }
            new GUI("Select Explosion - Page " + page, 6, false, buttons).openInventory(player);
        }));
    }

    public void openGreenSoundMenu(Player player, int page) {
        final int[] index = new int[1];
        List<Map.Entry<Integer, CosmeticSound>> availableSounds = Cosmetics.greenSounds.entrySet().stream().filter(entry -> player.hasPermission(entry.getValue().getPermission())).toList();
        Athlete athlete = AthleteManager.get(player.getUniqueId());
        PlayerDb.get(player.getUniqueId(), PlayerDb.Stat.GREEN_SOUND).thenAccept(equippedSoundKey -> Bukkit.getScheduler().runTask(Partix.getInstance(), () -> {
            int maxItemsPerPage = 45;
            int totalPages = Math.max(1, (int) Math.ceil((double) availableSounds.size() / (double) maxItemsPerPage));
            ItemButton[] buttons = new ItemButton[54];
            for (int i = 0; i < maxItemsPerPage && (index[0] = (page - 1) * maxItemsPerPage + i) < availableSounds.size(); ++i) {
                Map.Entry<Integer, CosmeticSound> entry2 = availableSounds.get(index[0]);
                int soundKey = entry2.getKey();
                CosmeticSound sound = entry2.getValue();
                buttons[i] = new ItemButton(i, sound.getGUIItem(), p -> {
                    athlete.setGreenSound(sound);
                    PlayerDb.set(player.getUniqueId(), PlayerDb.Stat.GREEN_SOUND, soundKey);
                    p.sendMessage("§aGreen Sound equipped: §6" + sound.getName());
                    this.openGreenSoundMenu(p, page);
                });
            }
            if (page > 1) {
                buttons[45] = new ItemButton(45, this.createArrow("§aPrevious Page"), p -> this.openGreenSoundMenu(p, page - 1));
            }
            if (page < totalPages) {
                buttons[53] = new ItemButton(53, this.createArrow("§aNext Page"), p -> this.openGreenSoundMenu(p, page + 1));
            }
            new GUI("Green Sound Selector - Page " + page, 6, false, buttons).openInventory(player);
        }));
    }

    private ItemStack createPlaceholder() {
        ItemStack item = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        item.editMeta(meta -> meta.displayName(Component.text(" ")));
        return item;
    }

    public void openGreenSoundSelectionGUI(Player player, Consumer<CosmeticSound> callback) {
        List<Map.Entry<Integer, CosmeticSound>> availableSounds = Cosmetics.greenSounds.entrySet().stream().filter(entry -> player.hasPermission(entry.getValue().getPermission())).toList();
        Athlete athlete = AthleteManager.get(player.getUniqueId());
        PlayerDb.get(player.getUniqueId(), PlayerDb.Stat.GREEN_SOUND).thenAccept(equippedSoundKey -> Bukkit.getScheduler().runTask(Partix.getInstance(), () -> {
            CosmeticSound equippedSound = Cosmetics.greenSounds.get(equippedSoundKey);
            ItemButton[] buttons = new ItemButton[availableSounds.size()];
            for (int i = 0; i < availableSounds.size(); ++i) {
                Map.Entry<Integer, CosmeticSound> entry2 = availableSounds.get(i);
                int soundKey = entry2.getKey();
                CosmeticSound sound = entry2.getValue();
                boolean isSelected = equippedSound != null && equippedSound.equals(sound);
                ItemStack item = Items.get(Component.text(sound.getName()).color(isSelected ? Colour.allow() : Colour.partix()), sound.getMaterial(), 1, "§7" + sound.getDescription(), isSelected ? "§a✓ Equipped" : "§6Click to equip");
                if (isSelected) {
                    ItemMeta meta = item.getItemMeta();
                    meta.setEnchantmentGlintOverride(true);
                    meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                    item.setItemMeta(meta);
                }
                buttons[i] = new ItemButton(i, item, p -> {
                    athlete.setGreenSound(sound);
                    PlayerDb.set(player.getUniqueId(), PlayerDb.Stat.GREEN_SOUND, soundKey);
                    p.sendMessage("§aGreen Sound set to: §6" + sound.getName());
                    callback.accept(sound);
                });
            }
            new GUI("Select Green Sound", 3, false, buttons).openInventory(player);
        }));
    }

    private void openBallTrailsMenu(Player player, int page) {
        PlayerDb.get(player.getUniqueId(), PlayerDb.Stat.BALL_TRAIL).thenAccept(equippedTrailKey -> Bukkit.getScheduler().runTask(Partix.getInstance(), () -> {
            int index;
            List<Map.Entry<Integer, CosmeticBallTrail>> availableTrails = Cosmetics.ballTrails.entrySet().stream().filter(entry -> player.hasPermission(entry.getValue().getPermission())).toList();
            int maxItemsPerPage = 45;
            int totalPages = Math.max(1, (int) Math.ceil((double) availableTrails.size() / (double) maxItemsPerPage));
            ItemButton[] buttons = new ItemButton[54];
            for (int i = 0; i < maxItemsPerPage && (index = (page - 1) * maxItemsPerPage + i) < availableTrails.size(); ++i) {
                Map.Entry<Integer, CosmeticBallTrail> entry2 = availableTrails.get(index);
                int ballTrailKey = entry2.getKey();
                CosmeticBallTrail ballTrail = entry2.getValue();
                boolean isSelected = equippedTrailKey == ballTrailKey;
                ItemStack item = ballTrail.getGUIItem().clone();
                if (isSelected) {
                    ItemMeta meta = item.getItemMeta();
                    meta.setEnchantmentGlintOverride(true);
                    meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                    item.setItemMeta(meta);
                }
                buttons[i] = new ItemButton(i, item, p -> {
                    PlayerDb.set(player.getUniqueId(), PlayerDb.Stat.BALL_TRAIL, ballTrailKey);
                    p.sendMessage("§aBall Trail equipped: §6" + ballTrail.getName());
                    this.openBallTrailsMenu(p, page);
                });
            }
            if (page > 1) {
                buttons[45] = new ItemButton(45, this.createArrow("§aPrevious Page"), p -> this.openBallTrailsMenu(p, page - 1));
            }
            if (page < totalPages) {
                buttons[53] = new ItemButton(53, this.createArrow("§aNext Page"), p -> this.openBallTrailsMenu(p, page + 1));
            }
            new GUI("Select Ball Trail - Page " + page, 6, false, buttons).openInventory(player);
        }));
    }

    private <T extends CosmeticHolder> void openPaginatedOwnedCosmeticGUI(Player player, List<Map.Entry<Integer, T>> ownedCosmetics, int page, String title, BiConsumer<Player, Integer> equipFunction) {
        int totalItems = ownedCosmetics.size();
        int itemsPerPage = 45;
        int maxPages = (int) Math.ceil((double) totalItems / (double) itemsPerPage);
        if (page < 1) {
            page = 1;
        }
        if (page > maxPages) {
            page = maxPages;
        }
        ItemButton[] buttons = new ItemButton[54];
        int startIndex = (page - 1) * itemsPerPage;
        int endIndex = Math.min(startIndex + itemsPerPage, totalItems);
        int currentPage = page;
        int i = startIndex;
        int slot = 0;
        while (i < endIndex) {
            Map.Entry<Integer, T> entry = ownedCosmetics.get(i);
            int cosmeticKey = entry.getKey();
            CosmeticHolder cosmetic = entry.getValue();
            buttons[slot] = new ItemButton(slot, cosmetic.getGUIItem(), p -> {
                equipFunction.accept(p, cosmeticKey);
                this.openPaginatedOwnedCosmeticGUI(p, ownedCosmetics, currentPage, title, equipFunction);
            });
            ++i;
            ++slot;
        }
        if (currentPage > 1) {
            buttons[45] = new ItemButton(45, this.createArrow("§aPrevious Page"), p -> this.openPaginatedOwnedCosmeticGUI(p, ownedCosmetics, currentPage - 1, title, equipFunction));
        }
        if (currentPage < maxPages) {
            buttons[53] = new ItemButton(53, Cosmetics.createNavigationButton("Next Page", Material.ARROW), p -> this.openPaginatedOwnedCosmeticGUI(p, ownedCosmetics, currentPage + 1, title, equipFunction));
        }
        new GUI(title + " (Page " + currentPage + ")", 6, false, buttons).openInventory(player);
    }

    private ItemStack createArrow(String name) {
        ItemStack item = new ItemStack(Material.ARROW);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(name));
        item.setItemMeta(meta);
        return item;
    }
}

