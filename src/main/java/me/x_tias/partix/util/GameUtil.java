/*
 * Decompiled with CFR 0.152.
 *
 * Could not load the following classes:
 *  org.bukkit.Location
 *  org.bukkit.Material
 */
package me.x_tias.partix.util;

import org.bukkit.Location;
import org.bukkit.Material;

import java.util.Random;

public class GameUtil {
    public static void createWall(Location loc1, Location loc2, double centerX, Material[] home, Material[] away) {
        Material material;
        int z;
        int x;
        int topBlockX = Math.max(loc1.getBlockX(), loc2.getBlockX());
        int bottomBlockX = Math.min(loc1.getBlockX(), loc2.getBlockX());
        int topBlockY = Math.max(loc1.getBlockY(), loc2.getBlockY());
        int bottomBlockY = Math.min(loc1.getBlockY(), loc2.getBlockY());
        int topBlockZ = Math.max(loc1.getBlockZ(), loc2.getBlockZ());
        int bottomBlockZ = Math.min(loc1.getBlockZ(), loc2.getBlockZ());
        Random r = new Random();
        for (x = bottomBlockX; x <= topBlockX; ++x) {
            for (z = bottomBlockZ; z <= topBlockZ; ++z) {
                material = x == (int) centerX ? Material.BLACK_CONCRETE : ((double) x > centerX ? home[r.nextInt(home.length)] : away[r.nextInt(away.length)]);
                loc1.getWorld().getBlockAt(x, bottomBlockY, z).setType(material);
                loc1.getWorld().getBlockAt(x, topBlockY, z).setType(material);
            }
        }
        for (x = bottomBlockX; x <= topBlockX; ++x) {
            for (int y = bottomBlockY; y <= topBlockY; ++y) {
                material = x == (int) centerX ? Material.BLACK_CONCRETE : ((double) x > centerX ? home[r.nextInt(home.length)] : away[r.nextInt(away.length)]);
                loc1.getWorld().getBlockAt(x, y, bottomBlockZ).setType(material);
                loc1.getWorld().getBlockAt(x, y, topBlockZ).setType(material);
            }
        }
        for (int y = bottomBlockY; y <= topBlockY; ++y) {
            for (z = bottomBlockZ; z <= topBlockZ; ++z) {
                loc1.getWorld().getBlockAt(bottomBlockX, y, z).setType((double) bottomBlockX > centerX ? home[r.nextInt(home.length)] : away[r.nextInt(away.length)]);
                loc1.getWorld().getBlockAt(topBlockX, y, z).setType((double) topBlockX > centerX ? home[r.nextInt(home.length)] : away[r.nextInt(away.length)]);
            }
        }
    }

    public static void createWall(Location center, Material[] home, Material[] away, int x, int y, int z) {
        Location top = center.clone().toBlockLocation().add(x, y, z);
        Location bottom = center.clone().toBlockLocation().subtract(x + 1, 2.0, z);
        GameUtil.createWall(top, bottom, center.getX(), home, away);
    }
}

