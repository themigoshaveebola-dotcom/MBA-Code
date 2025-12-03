/*
 * Decompiled with CFR 0.152.
 *
 * Could not load the following classes:
 *  net.kyori.adventure.text.Component
 *  net.kyori.adventure.text.TextComponent
 *  org.bukkit.Material
 *  org.bukkit.inventory.ItemStack
 *  org.bukkit.inventory.meta.ItemMeta
 */
package me.x_tias.partix.plugin.cosmetics;

import lombok.Getter;
import me.x_tias.partix.util.Colour;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

@Getter
public class CosmeticSound
        extends CosmeticHolder {
    public static final CosmeticSound NO_SOUND = new CosmeticSound("no_sound", Material.BARRIER, "No Sound", "No Green Sound selected.", CosmeticRarity.COMMON, "minecraft:entity.ambient.cave", "partix.sound.no_sound");
    private final String soundIdentifier;
    private final String description;

    public CosmeticSound(String key, Material guiMaterial, String name, String description, CosmeticRarity rarity, String soundIdentifier, String permission) {
        super(name, permission, guiMaterial, rarity, key);
        this.soundIdentifier = soundIdentifier;
        this.description = description != null ? description : "No description provided.";
    }

    public static CosmeticSound empty() {
        return NO_SOUND;
    }

    public Material getGui() {
        return this.getMaterial();
    }

    @Override
    public ItemStack getGUIItem() {
        ItemStack itemStack = new ItemStack(this.getMaterial());
        ItemMeta itemMeta = itemStack.getItemMeta();
        if (itemMeta != null) {
            itemMeta.displayName(Component.text("§l§a" + this.getName()));
            itemMeta.lore(List.of(Component.text("§8Cosmetic").color(Colour.border()), Component.text(" "), Component.text("§7Description: ").append(Component.text(this.description)), Component.text("§7Sound: ").append(Component.text(this.soundIdentifier != null ? this.soundIdentifier : "None")).color(Colour.partix()), Component.text("§7Permission: ").append(Component.text(this.getPermission()))));
            itemStack.setItemMeta(itemMeta);
        }
        return itemStack;
    }
}

