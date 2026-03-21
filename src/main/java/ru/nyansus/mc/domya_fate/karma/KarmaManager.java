package ru.nyansus.mc.domya_fate.karma;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.UUID;
import java.util.function.BiConsumer;

public class KarmaManager {

    private static final long MS_PER_HOUR = 3_600_000L;

    private final KarmaStorage storage;
    private final int minKarma;
    private final int maxKarma;
    private final boolean decayEnabled;
    private final double decayPercentPerHour;
    private final int decayMinThreshold;
    private BiConsumer<UUID, Integer> onKarmaChange;

    public KarmaManager(JavaPlugin plugin, KarmaStorage storage) {
        this.storage = storage;
        FileConfiguration config = plugin.getConfig();
        this.minKarma = config.getInt("karma.min", -1000);
        this.maxKarma = config.getInt("karma.max", 1000);
        this.decayEnabled = config.getBoolean("decay.enabled", true);
        this.decayPercentPerHour = config.getDouble("decay.percent-per-hour", 1.0);
        this.decayMinThreshold = config.getInt("decay.min-threshold", 100);
    }

    public void setOnKarmaChange(BiConsumer<UUID, Integer> listener) {
        this.onKarmaChange = listener;
    }

    public int getKarma(UUID uuid) {
        int stored = storage.getKarma(uuid);
        if (!decayEnabled || Math.abs(stored) < decayMinThreshold) {
            return stored;
        }
        long lastUpdate = storage.getLastUpdate(uuid);
        long now = System.currentTimeMillis();
        long elapsed = now - lastUpdate;
        if (elapsed < MS_PER_HOUR) {
            return stored;
        }
        double hours = (double) elapsed / MS_PER_HOUR;
        double factor = Math.pow(1.0 - decayPercentPerHour / 100.0, hours);
        int effective = (int) (stored * factor);
        storage.setKarma(uuid, effective, now);
        return effective;
    }

    public void addKarma(UUID uuid, int amount) {
        int current = getKarma(uuid);
        int newKarma = clamp(current + amount);
        storage.setKarma(uuid, newKarma, System.currentTimeMillis());
        fireChange(uuid, newKarma);
    }

    public void setKarma(UUID uuid, int value) {
        int clamped = clamp(value);
        storage.setKarma(uuid, clamped, System.currentTimeMillis());
        fireChange(uuid, clamped);
    }

    public void saveAll() {
        storage.save();
    }

    private void fireChange(UUID uuid, int newKarma) {
        if (onKarmaChange != null) {
            onKarmaChange.accept(uuid, newKarma);
        }
    }

    private int clamp(int value) {
        return Math.max(minKarma, Math.min(maxKarma, value));
    }
}
