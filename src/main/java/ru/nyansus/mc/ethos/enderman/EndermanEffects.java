package ru.nyansus.mc.ethos.enderman;

import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Registry;
import org.bukkit.Sound;
import org.bukkit.configuration.file.YamlConfiguration;
import ru.nyansus.mc.ethos.Ethos;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

class EndermanEffects {

    private final Ethos plugin;
    private final Set<String> warnedConfigValues = new HashSet<>();

    EndermanEffects(Ethos plugin) {
        this.plugin = plugin;
    }

    void playTelegraph(YamlConfiguration config, Location location, String path,
                       Particle fallbackParticle, Sound fallbackSound) {
        Particle particle = parseEnum(Particle.class,
                config.getString(path + ".particle", fallbackParticle.name()), fallbackParticle);
        Sound sound = parseSound(config.getString(path + ".sound"), fallbackSound);
        int count = config.getInt(path + ".particle-count", 30);
        double spread = config.getDouble(path + ".particle-spread", 0.5);
        double speed = config.getDouble(path + ".particle-speed", 0.03);
        float volume = (float) config.getDouble(path + ".volume", 0.8);
        float pitch = (float) config.getDouble(path + ".pitch", 0.8);

        spawnParticleSafely(location, particle, fallbackParticle, count, spread, speed);
        location.getWorld().playSound(location, sound, volume, pitch);
    }

    <T extends Enum<T>> T parseEnum(Class<T> type, String value, T fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        try {
            return Enum.valueOf(type, value.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            warnOnce("Unknown endermen.yml value " + value + " for " + type.getSimpleName());
            return fallback;
        }
    }

    Sound parseSound(String value, Sound fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        String normalized = value.toLowerCase(Locale.ROOT);
        if (normalized.indexOf('.') < 0) {
            normalized = normalized.replace('_', '.');
        }
        Sound sound = Registry.SOUNDS.get(NamespacedKey.minecraft(normalized));
        if (sound == null) {
            warnOnce("Unknown endermen.yml sound: " + value);
            return fallback;
        }
        return sound;
    }

    private void spawnParticleSafely(Location location, Particle particle, Particle fallback,
                                     int count, double spread, double speed) {
        Location at = location.clone().add(0, 1.0, 0);
        try {
            location.getWorld().spawnParticle(particle, at, count, spread, spread, spread, speed);
        } catch (IllegalArgumentException e) {
            warnOnce("Particle " + particle + " needs extra data, using " + fallback + " instead");
            location.getWorld().spawnParticle(fallback, at, count, spread, spread, spread, speed);
        }
    }

    private void warnOnce(String message) {
        if (warnedConfigValues.add(message)) {
            plugin.getLogger().warning(message);
        }
    }
}
