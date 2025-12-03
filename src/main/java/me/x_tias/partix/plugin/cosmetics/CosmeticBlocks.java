/*
 * Decompiled with CFR 0.152.
 *
 * Could not load the following classes:
 *  net.kyori.adventure.text.Component
 *  org.bukkit.Material
 *  org.bukkit.inventory.ItemStack
 *  org.bukkit.inventory.meta.ItemMeta
 */
package me.x_tias.partix.plugin.cosmetics;

import me.x_tias.partix.util.Colour;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public class CosmeticBlocks
        extends CosmeticHolder {
    private final Material[] materials;
    private final Component desc;
    private final Material gui;

    public CosmeticBlocks(String permission, Material gui, String name, String description, CosmeticRarity rarity, String key, Material... set) {
        super(name, permission, gui, rarity, key);
        this.materials = set;
        this.gui = gui;
        this.desc = Component.text(description).color(Colour.premiumText());
    }

    public Material[] get() {
        return this.materials.clone();
    }

    @Override
    public ItemStack getGUIItem() {
        ItemStack itemStack = new ItemStack(this.gui);
        ItemMeta itemMeta = itemStack.getItemMeta();
        itemMeta.displayName(Component.text(this.getRarity().getTitle() + " " + this.getName()));
        itemMeta.lore(List.of(Component.text("§r§8Cosmetic").color(Colour.border()), Component.text("   "), this.desc, Component.text("§r§ePrice: §6" + this.getRarity().getCost() + " Coins")));
        itemStack.setItemMeta(itemMeta);
        return itemStack;
    }
}

