/*
 * Decompiled with CFR 0.152.
 *
 * Could not load the following classes:
 *  net.kyori.adventure.text.Component
 *  org.bukkit.Bukkit
 *  org.bukkit.ChatColor
 *  org.bukkit.Color
 *  org.bukkit.Material
 *  org.bukkit.OfflinePlayer
 *  org.bukkit.inventory.ItemStack
 *  org.bukkit.inventory.meta.ItemMeta
 *  org.bukkit.inventory.meta.LeatherArmorMeta
 *  org.bukkit.inventory.meta.SkullMeta
 */
package me.x_tias.partix.util;

import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.Arrays;
import java.util.UUID;

public class Items {
    public static String colouredText(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }

    public static ItemStack get(Component component, Material material) {
        ItemStack itemStack = new ItemStack(material);
        ItemMeta meta = itemStack.getItemMeta();
        meta.displayName(component);
        itemStack.setItemMeta(meta);
        return itemStack;
    }

    public static ItemStack get(Component component, Material material, int amount) {
        ItemStack itemStack = new ItemStack(material, amount);
        ItemMeta meta = itemStack.getItemMeta();
        meta.displayName(component);
        itemStack.setItemMeta(meta);
        return itemStack;
    }

    public static ItemStack get(Component component, Material material, int amount, String... lore) {
        ItemStack itemStack = new ItemStack(material, amount);
        ItemMeta meta = itemStack.getItemMeta();
        meta.displayName(component);
        meta.lore(Arrays.stream(lore).map(Component::text).toList());
        itemStack.setItemMeta(meta);
        return itemStack;
    }

    public static ItemStack create(Material material, Component name, String... lore) {
        ItemStack itemStack = new ItemStack(material, 1);
        ItemMeta itemMeta = itemStack.getItemMeta();
        itemMeta.lore(Arrays.stream(lore).map(Component::text).toList());
        itemMeta.displayName(name);
        itemStack.setItemMeta(itemMeta);
        return itemStack;
    }

    public static ItemStack armor(Material material, int color, String name, String... lore) {
        ItemStack itemStack = new ItemStack(material, 1);
        LeatherArmorMeta itemMeta = (LeatherArmorMeta) itemStack.getItemMeta();
        itemMeta.lore(Arrays.stream(lore).map(Component::text).toList());
        itemMeta.setColor(Color.fromRGB(color));
        itemMeta.displayName(Component.text(name));
        itemStack.setItemMeta(itemMeta);
        return itemStack;
    }

    public static ItemStack getPlayerHead(UUID uuid) {
        OfflinePlayer player = Bukkit.getOfflinePlayer(uuid);
        if (player.hasPlayedBefore()) {
            ItemStack playerHead = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta skullMeta = (SkullMeta) playerHead.getItemMeta();
            skullMeta.setOwningPlayer(player);
            playerHead.setItemMeta(skullMeta);
            return playerHead;
        }
        return null;
    }

    public static ItemStack getPlayerHead(UUID uuid, String title, String... lore) {
        OfflinePlayer player = Bukkit.getOfflinePlayer(uuid);
        if (player.hasPlayedBefore()) {
            ItemStack playerHead = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta skullMeta = (SkullMeta) playerHead.getItemMeta();
            skullMeta.displayName(Component.text(title).color(Colour.partix()));
            skullMeta.lore(Arrays.stream(lore).map(Component::text).toList());
            skullMeta.setOwningPlayer(player);
            playerHead.setItemMeta(skullMeta);
            return playerHead;
        }
        return null;
    }

    public static ItemStack getSkull(OfflinePlayer player) {
        ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) skull.getItemMeta();
        if (meta != null) {
            meta.setOwningPlayer(player);
            meta.displayName(Component.text(player.getName()).color(Colour.partix()));
            skull.setItemMeta(meta);
        }
        return skull;
    }
}

