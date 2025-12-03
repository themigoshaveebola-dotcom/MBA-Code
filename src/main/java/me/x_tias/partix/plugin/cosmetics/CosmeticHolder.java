/*
 * Decompiled with CFR 0.152.
 *
 * Could not load the following classes:
 *  net.kyori.adventure.text.Component
 *  org.bukkit.Material
 *  org.bukkit.entity.Player
 *  org.bukkit.inventory.ItemStack
 *  org.bukkit.inventory.meta.ItemMeta
 */
package me.x_tias.partix.plugin.cosmetics;

import lombok.Getter;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public abstract class CosmeticHolder {
    @Getter
    private final String name;
    @Getter
    private final String permission;
    @Getter
    private final CosmeticRarity rarity;
    @Getter
    private final String key;
    private Material block;

    protected CosmeticHolder(String name, String permission, Material block, CosmeticRarity rarity, String key) {
        this.name = name != null ? name : "Unnamed Cosmetic";
        this.permission = permission != null ? permission : "";
        this.block = block != null ? block : Material.BARRIER;
        this.rarity = rarity != null ? rarity : CosmeticRarity.COMMON;
        this.key = key != null ? key : "";
    }

    public Material getMaterial() {
        return this.block;
    }

    public void setMaterial(Material material) {
        this.block = material != null ? material : Material.BARRIER;
    }

    public boolean hasPermission(Player player) {
        return this.permission.isEmpty() || player.hasPermission(this.permission);
    }

    public abstract ItemStack getGUIItem();

    protected ItemStack createGUIItem() {
        ItemStack item = new ItemStack(this.block);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(this.getRarityColor(this.rarity) + this.name));
            meta.lore(List.of(Component.text("§7Rarity: " + this.getRarityColor(this.rarity) + this.rarity.getTitle())));
            item.setItemMeta(meta);
        }
        return item;
    }

    private String getRarityColor(CosmeticRarity rarity) {
        if (rarity == null) {
            return "§7";
        }
        return switch (rarity) {
            case CosmeticRarity.COMMON -> "§7";
            case CosmeticRarity.RARE -> "§9";
            case CosmeticRarity.EPIC -> "§5";
            case CosmeticRarity.LEGENDARY -> "§6";
        };
    }
}

