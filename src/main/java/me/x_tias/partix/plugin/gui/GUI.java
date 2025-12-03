/*
 * Decompiled with CFR 0.152.
 *
 * Could not load the following classes:
 *  net.kyori.adventure.text.Component
 *  org.bukkit.Bukkit
 *  org.bukkit.entity.HumanEntity
 *  org.bukkit.entity.Player
 *  org.bukkit.event.EventHandler
 *  org.bukkit.event.HandlerList
 *  org.bukkit.event.Listener
 *  org.bukkit.event.inventory.InventoryClickEvent
 *  org.bukkit.event.inventory.InventoryCloseEvent
 *  org.bukkit.inventory.Inventory
 *  org.bukkit.inventory.ItemStack
 *  org.bukkit.plugin.Plugin
 *  org.bukkit.scheduler.BukkitRunnable
 *  org.bukkit.scheduler.BukkitTask
 */
package me.x_tias.partix.plugin.gui;

import me.x_tias.partix.Partix;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.function.Consumer;

public class GUI
        implements Listener {
    private final HashMap<Integer, Consumer<Player>> map = new HashMap<>();
    private final Inventory i;
    private BukkitTask runnable;

    public GUI(String title, int rows, boolean refresh, ItemButton... buttons) {
        Partix pl = Partix.getInstance();
        pl.getServer().getPluginManager().registerEvents(this, pl);
        this.i = Bukkit.createInventory(null, rows * 9, Component.text(title));
        for (ItemButton button : buttons) {
            if (button == null) continue;
            int slot = button.getSlot();
            if (slot >= this.i.getSize()) {
                Bukkit.getLogger().warning("GUI: Slot " + slot + " is out of bounds for inventory of size " + this.i.getSize() + ". Skipping button: " + button);
                continue;
            }
            this.map.put(slot, button.getRunnable());
            this.i.setItem(slot, button.getItem());
        }
        if (refresh) {
            final ItemButton[] finalButtons = buttons.clone();
            this.runnable = new BukkitRunnable() {

                public void run() {
                    GUI.this.i.clear();
                    for (ItemButton button : finalButtons) {
                        if (button == null) continue;
                        int slot = button.getSlot();
                        if (slot >= GUI.this.i.getSize()) {
                            Bukkit.getLogger().warning("GUI (refresh): Slot " + slot + " is out of bounds for inventory of size " + GUI.this.i.getSize() + ". Skipping button: " + button);
                            continue;
                        }
                        GUI.this.map.put(slot, button.getRunnable());
                        GUI.this.i.setItem(slot, button.getItem());
                    }
                }
            }.runTaskTimer(Partix.getInstance(), 1L, 60L);
        }
    }

    public GUI(Component title, int rows, boolean refresh) {
        Partix pl = Partix.getInstance();
        pl.getServer().getPluginManager().registerEvents(this, pl);
        this.i = Bukkit.createInventory(null, rows * 9, title);
    }

    public void openInventory(Player player) {
        player.openInventory(this.i);
    }

    @EventHandler
    public void onEdit(InventoryClickEvent e) {
        HumanEntity humanEntity;
        if (e.getInventory().equals(this.i) && (humanEntity = e.getWhoClicked()) instanceof Player) {
            Player player = (Player) humanEntity;
            int slot = e.getSlot();
            Consumer<Player> consumer = this.map.get(slot);
            if (consumer != null) {
                consumer.accept(player);
            }
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent e) {
        if (e.getInventory().equals(this.i)) {
            if (this.runnable != null) {
                this.runnable.cancel();
                this.runnable = null;
            }
            HandlerList.unregisterAll(this);
        }
    }

    public void addButton(ItemButton button) {
        if (button != null) {
            int slot = button.getSlot();
            if (slot >= this.i.getSize()) {
                Bukkit.getLogger().warning("GUI addButton: Slot " + slot + " is out of bounds for inventory of size " + this.i.getSize() + ". Button not added: " + button);
                return;
            }
            this.map.put(slot, button.getRunnable());
            this.i.setItem(slot, button.getItem());
        }
    }

    public void clearButtons() {
        this.map.clear();
        this.i.clear();
    }

    public void setTitle(String newTitle) {
        throw new UnsupportedOperationException("Changing inventory titles dynamically is not supported by Bukkit.");
    }

    public ItemStack getItem(int slot) {
        return this.i.getItem(slot);
    }
}

