package me.x_tias.partix.command;
import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import me.x_tias.partix.plugin.cosmetics.CrateInventory;
import me.x_tias.partix.plugin.cosmetics.CratePurchaseGUI;
import org.bukkit.entity.Player;

@CommandAlias("crateshop|crates")
public class CrateShopCommand extends BaseCommand {

    @Subcommand("trail")
    @Description("Open trail crate shop")
    public void onTrailShop(Player player) {
        new CratePurchaseGUI(player, CrateInventory.CrateType.TRAIL);
    }

    @Subcommand("explosion")
    @Description("Open explosion crate shop")
    public void onExplosionShop(Player player) {
        new CratePurchaseGUI(player, CrateInventory.CrateType.EXPLOSION);
    }

    @Subcommand("greensound")
    @Description("Open green sound crate shop")
    public void onGreenSoundShop(Player player) {
        new CratePurchaseGUI(player, CrateInventory.CrateType.GREEN_SOUND);
    }

    @Subcommand("balltrail")
    @Description("Open ball trail crate shop")
    public void onBallTrailShop(Player player) {
        new CratePurchaseGUI(player, CrateInventory.CrateType.BALL_TRAIL);
    }
}