package ru.nyansus.mc.domya_fate.karma;

import org.bukkit.configuration.file.FileConfiguration;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class AntiFarmManager {

    private final long pvpCooldownMs;
    private final long mutualKillWindowMs;
    private final double spawnerKarmaMultiplier;
    private final boolean spawnerCountsForTitles;

    private final Map<UUID, Map<UUID, Long>> pvpKills = new HashMap<>();
    private final Map<UUID, Map<UUID, Long>> mutualKills = new HashMap<>();

    public AntiFarmManager(FileConfiguration config) {
        this.pvpCooldownMs = config.getLong("anti-farm.pvp-cooldown-minutes", 30) * 60_000L;
        this.mutualKillWindowMs = config.getLong("anti-farm.mutual-kill-window-minutes", 5) * 60_000L;
        this.spawnerKarmaMultiplier = config.getDouble("anti-farm.spawner-karma-multiplier", 0.5);
        this.spawnerCountsForTitles = config.getBoolean("anti-farm.spawner-counts-for-titles", false);
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
    }

    public int applySpawnerMultiplier(int karma) {
        return (int) (karma * spawnerKarmaMultiplier);
    }

    public boolean doesSpawnerCountForTitles() {
        return spawnerCountsForTitles;
    }
}
