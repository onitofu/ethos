package ru.nyansus.mc.domya_fate.karma;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.UUID;
import java.util.function.BiConsumer;

public class KarmaManager {

    private static final long MS_PER_DAY = 86_400_000L;

    private final KarmaStorage storage;
    private final int minKarma;
    private final int maxKarma;
    private final boolean decayEnabled;
    private final int decayPerDay;
    private BiConsumer<UUID, Integer> onKarmaChange;

    public KarmaManager(JavaPlugin plugin, KarmaStorage storage) {
        this.storage = storage;
        FileConfiguration config = plugin.getConfig();
        this.minKarma = config.getInt("karma.min", -10000);
        this.maxKarma = config.getInt("karma.max", 10000);
        this.decayEnabled = config.getBoolean("decay.enabled", true);
        this.decayPerDay = config.getInt("decay.points-per-day", 5);
    }

    public void setOnKarmaChange(BiConsumer<UUID, Integer> listener) {
        this.onKarmaChange = listener;
    }

    public int getKarma(UUID uuid) {
        int stored = storage.getKarma(uuid);
        if (!decayEnabled || stored == 0 || decayPerDay <= 0) {
            return stored;
        }
        long lastUpdate = storage.getLastUpdate(uuid);
        long now = System.currentTimeMillis();
        long elapsed = now - lastUpdate;
        if (elapsed < MS_PER_DAY) {
            return stored;
        }

        long days = elapsed / MS_PER_DAY;
        int totalDecay = (int) (days * decayPerDay);

        int effective;
        if (stored > 0) {
            effective = Math.max(0, stored - totalDecay);
        } else {
            effective = Math.min(0, stored + totalDecay);
        }

        storage.setKarma(uuid, effective, now);
        if (effective != stored) {
            fireChange(uuid, effective);
        }
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
