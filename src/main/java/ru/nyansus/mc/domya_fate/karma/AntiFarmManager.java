package ru.nyansus.mc.domya_fate.karma;

import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class AntiFarmManager {

    private final long pvpCooldownMs;
    private final long mutualKillWindowMs;
    private final double spawnerKarmaMultiplier;
    private final boolean spawnerCountsForTitles;
    private final long afkThresholdMs;
    private final double afkMoveDistance;
    private final int mobStreakLimit;

    private final Map<UUID, Map<UUID, Long>> pvpKills = new HashMap<>();
    private final Map<UUID, Map<UUID, Long>> mutualKills = new HashMap<>();
    private final Map<UUID, LocationSnapshot> lastPositions = new HashMap<>();
    private final Map<UUID, EntityType> lastMobType = new HashMap<>();
    private final Map<UUID, Integer> mobStreak = new HashMap<>();

    public AntiFarmManager(FileConfiguration config) {
        this.pvpCooldownMs = config.getLong("anti-farm.pvp-cooldown-minutes", 30) * 60_000L;
        this.mutualKillWindowMs = config.getLong("anti-farm.mutual-kill-window-minutes", 5) * 60_000L;
        this.spawnerKarmaMultiplier = config.getDouble("anti-farm.spawner-karma-multiplier", 0.5);
        this.spawnerCountsForTitles = config.getBoolean("anti-farm.spawner-counts-for-titles", false);
        this.afkThresholdMs = config.getLong("anti-farm.afk-threshold-seconds", 120) * 1000L;
        this.afkMoveDistance = config.getDouble("anti-farm.afk-move-distance", 5.0);
        this.mobStreakLimit = config.getInt("anti-farm.mob-streak-limit", 5);
    }

    public boolean isMobStreakExceeded(UUID player, EntityType mobType) {
        EntityType last = lastMobType.get(player);
        if (last != null && last == mobType) {
            int streak = mobStreak.merge(player, 1, Integer::sum);
            return streak > mobStreakLimit;
        }
        lastMobType.put(player, mobType);
        mobStreak.put(player, 1);
        return false;
    }

    public boolean isAfk(Player player) {
        UUID uuid = player.getUniqueId();
        Location current = player.getLocation();
        long now = System.currentTimeMillis();

        LocationSnapshot last = lastPositions.get(uuid);
        if (last == null) {
            lastPositions.put(uuid, new LocationSnapshot(current, now));
            return false;
        }

        if (last.location().getWorld().equals(current.getWorld())
                && last.location().distance(current) < afkMoveDistance) {
            return now - last.timestamp() > afkThresholdMs;
        }

        lastPositions.put(uuid, new LocationSnapshot(current, now));
        return false;
    }

    public void updatePosition(Player player) {
        UUID uuid = player.getUniqueId();
        Location current = player.getLocation();
        long now = System.currentTimeMillis();

        LocationSnapshot last = lastPositions.get(uuid);
        if (last == null || !last.location().getWorld().equals(current.getWorld())
                || last.location().distance(current) >= afkMoveDistance) {
            lastPositions.put(uuid, new LocationSnapshot(current, now));
        }
    }

    public boolean isPvpOnCooldown(UUID killer, UUID victim) {
        Map<UUID, Long> victims = pvpKills.get(killer);
        if (victims == null) {
            return false;
        }
        Long timestamp = victims.get(victim);
        if (timestamp == null) {
            return false;
        }
        return System.currentTimeMillis() - timestamp < pvpCooldownMs;
    }

    public boolean isMutualKill(UUID killer, UUID victim) {
        Map<UUID, Long> killerVictims = mutualKills.get(victim);
        if (killerVictims == null) {
            return false;
        }
        Long timestamp = killerVictims.get(killer);
        if (timestamp == null) {
            return false;
        }
        return System.currentTimeMillis() - timestamp < mutualKillWindowMs;
    }

    public void recordPvpKill(UUID killer, UUID victim) {
        pvpKills.computeIfAbsent(killer, k -> new HashMap<>()).put(victim, System.currentTimeMillis());
        mutualKills.computeIfAbsent(killer, k -> new HashMap<>()).put(victim, System.currentTimeMillis());
    }

    public void clearPlayer(UUID player) {
        pvpKills.remove(player);
        lastPositions.remove(player);
        lastMobType.remove(player);
        mobStreak.remove(player);
    }

    public int applySpawnerMultiplier(int karma) {
        return (int) (karma * spawnerKarmaMultiplier);
    }

    public boolean doesSpawnerCountForTitles() {
        return spawnerCountsForTitles;
    }

    private record LocationSnapshot(Location location, long timestamp) {
    }
}
