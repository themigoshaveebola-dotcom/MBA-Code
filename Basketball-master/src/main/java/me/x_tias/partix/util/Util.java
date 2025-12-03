/*
 * Decompiled with CFR 0.152.
 */
package me.x_tias.partix.util;

import me.x_tias.partix.plugin.athlete.Athlete;

import java.util.*;

public class Util {
    public static UUID getHighest(HashMap<UUID, Double> map) {
        Map.Entry<UUID, Double> maxEntry = null;
        for (Map.Entry<UUID, Double> entry : map.entrySet()) {
            if (maxEntry != null && entry.getValue().compareTo(maxEntry.getValue()) <= 0) continue;
            maxEntry = entry;
        }
        return maxEntry != null ? maxEntry.getKey() : null;
    }

    public static HashMap<Integer, UUID> getRankedUUIDs(HashMap<UUID, Double> map) {
        LinkedList<Map.Entry<UUID, Double>> list = new LinkedList<>(map.entrySet());
        list.sort((o1, o2) -> o2.getValue().compareTo(o1.getValue()));
        LinkedHashMap<Integer, UUID> sortedMap = new LinkedHashMap<>();
        int rank = 1;
        for (Map.Entry<UUID, Double> entry : list) {
            sortedMap.put(rank++, entry.getKey());
        }
        return sortedMap;
    }

    public static HashMap<Integer, Athlete> getRankedAthletes(HashMap<Athlete, Double> map) {
        LinkedList<Map.Entry<Athlete, Double>> list = new LinkedList<>(map.entrySet());
        list.sort((o1, o2) -> o2.getValue().compareTo(o1.getValue()));
        LinkedHashMap<Integer, Athlete> sortedMap = new LinkedHashMap<>();
        int rank = 1;
        for (Map.Entry<Athlete, Double> entry : list) {
            sortedMap.put(rank++, entry.getKey());
        }
        return sortedMap;
    }
}

