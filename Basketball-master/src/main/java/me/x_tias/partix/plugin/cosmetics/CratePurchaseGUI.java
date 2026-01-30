package me.x_tias.partix.plugin.cosmetics;

import me.x_tias.partix.Partix;
import me.x_tias.partix.database.PlayerDb;
import me.x_tias.partix.plugin.gui.GUI;
import me.x_tias.partix.plugin.gui.ItemButton;
import me.x_tias.partix.util.Colour;
import me.x_tias.partix.util.Items;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class CratePurchaseGUI {

    private static final int COMMON_PRICE = 300;
    private static final int RARE_PRICE = 800;
    private static final int EPIC_PRICE = 1900;
    private static final int LEGENDARY_PRICE = 2600;

    public CratePurchaseGUI(Player player, CrateInventory.CrateType type) {
        ItemButton[] buttons = new ItemButton[27];

        // Fill with glass panes
        for (int i = 0; i < 27; ++i) {
            buttons[i] = new ItemButton(i, Items.get(Component.text(" "), Material.BLACK_STAINED_GLASS_PANE, 1, " "), p -> {});
        }

        // Common Crate
        buttons[10] = createCrateButton(type, CosmeticRarity.COMMON, COMMON_PRICE, 10);

        // Rare Crate
        buttons[12] = createCrateButton(type, CosmeticRarity.RARE, RARE_PRICE, 12);

        // Epic Crate
        buttons[14] = createCrateButton(type, CosmeticRarity.EPIC, EPIC_PRICE, 14);

        // Legendary Crate
        buttons[16] = createCrateButton(type, CosmeticRarity.LEGENDARY, LEGENDARY_PRICE, 16);

        new GUI(type.getDisplayName() + " Crate Shop", 3, false, buttons).openInventory(player);
    }

    private ItemButton createCrateButton(CrateInventory.CrateType type, CosmeticRarity rarity, int price, int slot) {
        Material material = switch(rarity) {
            case COMMON -> Material.LIGHT_GRAY_SHULKER_BOX;
            case RARE -> Material.LIGHT_BLUE_SHULKER_BOX;
            case EPIC -> Material.PURPLE_SHULKER_BOX;
            case LEGENDARY -> Material.YELLOW_SHULKER_BOX;
        };

        String displayName = rarity.getColor() + rarity.name() + " " + type.getDisplayName() + " Crate";

        ItemStack item = Items.get(
                Component.text(displayName),
                material,
                1,
                "§7Contains " + rarity.name().toLowerCase() + " " + type.getDisplayName().toLowerCase() + "s",
                "§ePrice: §6" + price + " Coins",
                "",
                "§aClick to purchase!"
        );

        return new ItemButton(slot, item, p -> purchaseCrate(p, type, rarity, price));
    }

    private void purchaseCrate(Player player, CrateInventory.CrateType type, CosmeticRarity rarity, int price) {
        PlayerDb.get(player.getUniqueId(), PlayerDb.Stat.COINS).thenAccept(coins -> {
            if (coins < price) {
                player.sendMessage("§cYou don't have enough coins! Need: §6" + price + " §cCoins");
                return;
            }

            PlayerDb.set(player.getUniqueId(), PlayerDb.Stat.COINS, coins - price);
            CrateInventory.addCrate(player.getUniqueId(), type, rarity).thenRun(() -> {
                Bukkit.getScheduler().runTask(Partix.getInstance(), () -> {
                    player.sendMessage("§aPurchased §6" + rarity.name() + " " + type.getDisplayName() + " Crate§a!");
                    player.sendMessage("§7Open it from §ecosmetics §7→ §eCrate Inventory");
                    player.closeInventory();
                });
            });
        });
    }
}