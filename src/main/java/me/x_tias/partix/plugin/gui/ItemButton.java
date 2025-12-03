/*
 * Decompiled with CFR 0.152.
 *
 * Could not load the following classes:
 *  org.bukkit.entity.Player
 *  org.bukkit.event.inventory.ClickType
 *  org.bukkit.inventory.ItemStack
 */
package me.x_tias.partix.plugin.gui;

import lombok.Getter;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

@Getter
public class ItemButton {
    private final int slot;
    private final ItemStack item;
    private final Consumer<Player> runnable;
    private final BiConsumer<Player, ClickType> biRunnable;

    public ItemButton(int slot, ItemStack item, Consumer<Player> runnable) {
        this.slot = slot;
        this.item = item;
        this.runnable = runnable;
        this.biRunnable = null;
    }

    public ItemButton(int slot, ItemStack item, BiConsumer<Player, ClickType> biRunnable) {
        this.slot = slot;
        this.item = item;
        this.biRunnable = biRunnable;
        this.runnable = null;
    }

    public void run(Player player, ClickType click) {
        if (this.biRunnable != null && click != null) {
            this.biRunnable.accept(player, click);
        } else if (this.runnable != null) {
            this.runnable.accept(player);
        }
    }
}

