package me.x_tias.partix.command;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.Default;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

@CommandAlias("hattest")
public class HatTestCommand extends BaseCommand {
    
    @Default
    public void onHatTest(Player sender) {
        ItemStack hat = new ItemStack(Material.LEATHER_HELMET);
        ItemMeta meta = hat.getItemMeta();
        
        // This is the key line - sets CustomModelData
        meta.setCustomModelData(1);
        
        meta.displayName(Component.text("§6Cool Custom Hat"));
        meta.lore(java.util.List.of(
            Component.text("§7Custom Model ID: 1"),
            Component.text("§8Cosmetic Hat")
        ));
        
        hat.setItemMeta(meta);
        sender.getInventory().setHelmet(hat);
        
        sender.sendMessage(Component.text("✓ Equipped custom hat!", NamedTextColor.GREEN));
        sender.sendMessage(Component.text("Note: You need a resource pack to see it!", NamedTextColor.GRAY));
    }
}
