package ru.nyansus.mc.ethos.enderman;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Enderman;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import ru.nyansus.mc.ethos.Ethos;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public class EndermanManager extends BukkitRunnable implements Listener {

    private final Ethos plugin;
    private final EndermanEffects effects;
    private YamlConfiguration config;
    private final Map<UUID, Long> markedUntil = new HashMap<>();
    private final Map<UUID, Long> blinkCooldowns = new HashMap<>();
    private final Map<UUID, Long> towerCooldowns = new HashMap<>();
    private final Map<UUID, Long> dragCooldowns = new HashMap<>();
    private final Set<UUID> pendingBlink = new HashSet<>();
    private final Set<UUID> pendingTower = new HashSet<>();
    private final Set<UUID> pendingDrag = new HashSet<>();

    public EndermanManager(Ethos plugin) {
        this.plugin = plugin;
        this.effects = new EndermanEffects(plugin);
        reload();
    }

    public final void reload() {
        File file = new File(plugin.getDataFolder(), "endermen.yml");
        if (!file.exists()) {
            plugin.saveResource("endermen.yml", false);
        }
        config = YamlConfiguration.loadConfiguration(file);
        try (var stream = plugin.getResource("endermen.yml")) {
            if (stream != null) {
                var reader = new InputStreamReader(stream, StandardCharsets.UTF_8);
                config.setDefaults(YamlConfiguration.loadConfiguration(reader));
            }
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to load default endermen.yml: " + e.getMessage());
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        if (event.getEntityType() == EntityType.ENDERMAN) {
            boostEnderman((Enderman) event.getEntity());
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityTarget(EntityTargetLivingEntityEvent event) {
        if (!(event.getEntity() instanceof Enderman enderman)) {
            return;
        }
        boostEnderman(enderman);
        if (config.getBoolean("void-mark.trigger-on-target", false)
                && event.getTarget() instanceof Player player && canAffect(player)) {
            mark(player);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Enderman enderman) {
            boostEnderman(enderman);
            if (config.getBoolean("void-mark.trigger-on-damage", true)
                    && event.getEntity() instanceof Player player && canAffect(player)) {
                mark(player);
            }
        }
        if (event.getDamager() instanceof Player player
                && event.getEntity() instanceof Enderman enderman
                && canAffect(player)) {
            boostEnderman(enderman);
            if (config.getBoolean("void-mark.trigger-on-damage", true)) {
                mark(player);
            }
            enderman.setTarget(player);
        }
    }

    @EventHandler
    public void onPlayerGameModeChange(PlayerGameModeChangeEvent event) {
        if (event.getNewGameMode() != GameMode.SURVIVAL) {
            clearPlayerPressure(event.getPlayer());
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        clearPlayerPressure(event.getPlayer());
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        clearPlayerPressure(event.getEntity());
    }

    @Override
    public void run() {
        if (!enabled()) {
            return;
        }
        long now = System.currentTimeMillis();
        markedUntil.entrySet().removeIf(entry -> entry.getValue() <= now);
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            if (!canAffect(player)) {
                clearPlayerPressure(player);
                continue;
            }
            if (isMarked(player)) {
                handleMarkedPlayer(player, now);
            }
        }
    }

    private void handleMarkedPlayer(Player player, long now) {
        double radius = getVoidMarkRadius(player.getWorld());
        int maxAggro = config.getInt("void-mark.max-assist-endermen", 3);
        int aggroCount = countAggroEndermen(player, radius);
        for (Entity entity : player.getNearbyEntities(radius, radius, radius)) {
            if (!(entity instanceof Enderman enderman)) {
                continue;
            }
            boostEnderman(enderman);
            if (enderman.getTarget() == null && aggroCount < maxAggro) {
                enderman.setTarget(player);
                aggroCount++;
            }
            if (player.equals(enderman.getTarget())) {
                tryBlinkStrike(enderman, player, now);
                tryAntiTower(enderman, player, now);
                tryVoidDrag(enderman, player, now);
            }
        }
    }

    private int countAggroEndermen(Player player, double radius) {
        int count = 0;
        for (Entity entity : player.getNearbyEntities(radius, radius, radius)) {
            if (entity instanceof Enderman enderman && player.equals(enderman.getTarget())) {
                count++;
            }
        }
        return count;
    }

    private double getVoidMarkRadius(World world) {
        double radius = config.getDouble("void-mark.aggro-radius", 24.0);
        if (world.getEnvironment() == Environment.THE_END) {
            radius *= config.getDouble("void-mark.the-end-radius-multiplier", 0.35);
        }
        return Math.max(0, radius);
    }

    private void boostEnderman(Enderman enderman) {
        if (!enabled()) {
            return;
        }
        double maxHealth = config.getDouble("max-health", 60.0);
        scaleMaxHealth(enderman, maxHealth);
        setAttribute(enderman, Attribute.ATTACK_DAMAGE, config.getDouble("attack-damage", 10.0));
    }

    private void scaleMaxHealth(Enderman enderman, double maxHealth) {
        AttributeInstance health = enderman.getAttribute(Attribute.MAX_HEALTH);
        if (health == null || maxHealth <= 0) {
            return;
        }
        double previousMax = health.getBaseValue();
        if (Double.compare(previousMax, maxHealth) == 0) {
            return;
        }
        double ratio = Math.max(0.0, Math.min(1.0, enderman.getHealth() / previousMax));
        health.setBaseValue(maxHealth);
        enderman.setHealth(Math.max(1.0, Math.min(maxHealth, maxHealth * ratio)));
    }

    private void setAttribute(LivingEntity entity, Attribute attribute, double value) {
        AttributeInstance instance = entity.getAttribute(attribute);
        if (instance != null && value > 0) {
            instance.setBaseValue(value);
        }
    }

    private void mark(Player player) {
        if (!config.getBoolean("void-mark.enabled", true)) {
            return;
        }
        int seconds = config.getInt("void-mark.duration-seconds", 15);
        markedUntil.put(player.getUniqueId(), System.currentTimeMillis() + seconds * 1000L);
        player.getWorld().spawnParticle(Particle.PORTAL, player.getLocation().add(0, 1.0, 0),
                20, 0.4, 0.6, 0.4, 0.03);
    }

    private boolean isMarked(Player player) {
        return markedUntil.getOrDefault(player.getUniqueId(), 0L) > System.currentTimeMillis();
    }

    private void clearPlayerPressure(Player player) {
        markedUntil.remove(player.getUniqueId());
        clearEndermanTargets(player, Math.max(32.0, getVoidMarkRadius(player.getWorld())));
    }

    private void clearEndermanTargets(Player player, double radius) {
        for (Entity entity : player.getNearbyEntities(radius, radius, radius)) {
            if (entity instanceof Enderman enderman && player.equals(enderman.getTarget())) {
                enderman.setTarget(null);
                UUID uuid = enderman.getUniqueId();
                pendingBlink.remove(uuid);
                pendingTower.remove(uuid);
                pendingDrag.remove(uuid);
            }
        }
    }

    private void tryBlinkStrike(Enderman enderman, Player player, long now) {
        if (!config.getBoolean("blink-strike.enabled", true)) {
            return;
        }
        UUID uuid = enderman.getUniqueId();
        if (pendingBlink.contains(uuid) || blinkCooldowns.getOrDefault(uuid, 0L) > now) {
            return;
        }
        double distance = enderman.getLocation().distance(player.getLocation());
        double minDistance = config.getDouble("blink-strike.min-distance", 5.0);
        double maxDistance = config.getDouble("blink-strike.max-distance", 18.0);
        double chance = config.getDouble("blink-strike.chance", 0.35);
        if (distance < minDistance || distance > maxDistance
                || ThreadLocalRandom.current().nextDouble() > chance) {
            return;
        }

        Location destination = findBehindPlayer(player);
        if (destination == null) {
            return;
        }
        pendingBlink.add(uuid);
        playBlinkTelegraph(enderman, player, destination);
        long delay = config.getLong("blink-strike.telegraph-ticks", 12L);
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            pendingBlink.remove(uuid);
            if (!enderman.isValid() || !player.isOnline() || !canAffect(player)) {
                return;
            }
            teleportBehind(enderman, player, destination);
        }, delay);
        long cooldown = config.getLong("blink-strike.cooldown-seconds", 8L);
        blinkCooldowns.put(uuid, now + cooldown * 1000L);
    }

    private void playBlinkTelegraph(Enderman enderman, Player player, Location destination) {
        playTelegraph(enderman.getLocation(), "blink-strike.telegraph",
                Particle.REVERSE_PORTAL, Sound.ENTITY_ENDERMAN_TELEPORT);
        playTelegraph(destination, "blink-strike.destination-telegraph",
                Particle.DRAGON_BREATH, Sound.BLOCK_AMETHYST_BLOCK_CHIME);
        spawnParticleLine(enderman.getLocation().add(0, 1.0, 0),
                destination.clone().add(0, 1.0, 0),
                effects.parseEnum(Particle.class, config.getString(
                        "blink-strike.line-telegraph.particle", "PORTAL"), Particle.PORTAL),
                config.getInt("blink-strike.line-telegraph.points", 12));
        player.playSound(player.getLocation(), effects.parseSound(config.getString(
                "blink-strike.player-warning-sound"), Sound.ENTITY_ENDERMAN_STARE),
                (float) config.getDouble("blink-strike.player-warning-volume", 0.45),
                (float) config.getDouble("blink-strike.player-warning-pitch", 1.4));
    }

    private void spawnParticleLine(Location from, Location to, Particle particle, int points) {
        if (!from.getWorld().equals(to.getWorld()) || points <= 0) {
            return;
        }
        Vector step = to.toVector().subtract(from.toVector()).multiply(1.0 / points);
        Location cursor = from.clone();
        for (int i = 0; i <= points; i++) {
            from.getWorld().spawnParticle(particle, cursor, 1, 0.02, 0.02, 0.02, 0.0);
            cursor.add(step);
        }
    }

    private void teleportBehind(Enderman enderman, Player player, Location destination) {
        if (!isOpenSpace(destination)) {
            destination = findBehindPlayer(player);
        }
        if (destination == null || !isOpenSpace(destination)) {
            return;
        }
        World world = destination.getWorld();
        enderman.teleport(destination);
        enderman.setTarget(player);
        world.spawnParticle(Particle.PORTAL, destination.clone().add(0, 1.0, 0),
                35, 0.4, 0.8, 0.4, 0.04);
        world.playSound(destination, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.2f);
    }

    private Location findBehindPlayer(Player player) {
        Location base = player.getLocation();
        Vector behind = base.getDirection().setY(0).normalize().multiply(-2.0);
        Location destination = base.clone().add(behind);
        destination.setY(base.getY());
        destination.setYaw(base.getYaw());
        destination.setPitch(0);
        if (isOpenSpace(destination)) {
            return destination;
        }
        return null;
    }

    private boolean isOpenSpace(Location location) {
        return location.getBlock().isPassable()
                && location.clone().add(0, 1, 0).getBlock().isPassable();
    }

    private void tryAntiTower(Enderman enderman, Player player, long now) {
        if (!config.getBoolean("anti-tower.enabled", true)) {
            return;
        }
        UUID uuid = enderman.getUniqueId();
        if (pendingTower.contains(uuid) || towerCooldowns.getOrDefault(uuid, 0L) > now) {
            return;
        }
        double minHeight = config.getDouble("anti-tower.min-height-difference", 3.0);
        double heightDiff = player.getLocation().getY() - enderman.getLocation().getY();
        double maxHeight = config.getDouble("anti-tower.max-height-difference", 8.0);
        if (heightDiff < minHeight || heightDiff > maxHeight) {
            return;
        }
        pendingTower.add(uuid);
        playTelegraph(player.getLocation(), "anti-tower.telegraph",
                Particle.REVERSE_PORTAL, Sound.ENTITY_ENDERMAN_SCREAM);
        long delay = config.getLong("anti-tower.telegraph-ticks", 10L);
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            pendingTower.remove(uuid);
            if (!enderman.isValid() || !player.isOnline() || !canAffect(player)) {
                return;
            }
            double currentHeightDiff = player.getLocation().getY() - enderman.getLocation().getY();
            if (currentHeightDiff >= minHeight && currentHeightDiff <= maxHeight) {
                pushFromTower(enderman, player);
            }
        }, delay);
        long cooldown = config.getLong("anti-tower.cooldown-seconds", 10L);
        towerCooldowns.put(uuid, now + cooldown * 1000L);
    }

    private void pushFromTower(Enderman enderman, Player player) {
        Vector push = buildHorizontalPush(enderman, player)
                .multiply(config.getDouble("anti-tower.push-strength", 1.1));
        push.setY(config.getDouble("anti-tower.vertical-strength", 0.25));
        player.setVelocity(push);
        player.getWorld().spawnParticle(Particle.PORTAL,
                player.getLocation().add(0, 1.0, 0), 20, 0.4, 0.5, 0.4, 0.03);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT,
                0.9f, 0.55f);
    }

    private Vector buildHorizontalPush(Enderman enderman, Player player) {
        Vector push = player.getLocation().toVector().subtract(enderman.getLocation().toVector());
        push.setY(0);
        if (push.lengthSquared() < 0.01) {
            push = player.getLocation().getDirection();
            push.setY(0);
            if (push.lengthSquared() < 0.01) {
                push = new Vector(1, 0, 0);
            }
        }
        return push.normalize();
    }

    private void tryVoidDrag(Enderman enderman, Player player, long now) {
        if (!config.getBoolean("void-drag.enabled", true)) {
            return;
        }
        UUID uuid = enderman.getUniqueId();
        if (pendingDrag.contains(uuid) || dragCooldowns.getOrDefault(uuid, 0L) > now) {
            return;
        }
        double maxDistance = config.getDouble("void-drag.max-distance", 12.0);
        if (enderman.getLocation().distance(player.getLocation()) > maxDistance
                || !isUnderLowCeiling(player)) {
            return;
        }

        pendingDrag.add(uuid);
        long delay = config.getLong("void-drag.telegraph-ticks", 16L);
        playDragChannel(player, delay);
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            pendingDrag.remove(uuid);
            if (!enderman.isValid() || !player.isOnline() || !canAffect(player)
                    || !isUnderLowCeiling(player)) {
                return;
            }
            dragPlayer(enderman, player);
        }, delay);
        long cooldown = config.getLong("void-drag.cooldown-seconds", 12L);
        dragCooldowns.put(uuid, now + cooldown * 1000L);
    }

    private void playDragChannel(Player player, long delay) {
        long interval = Math.max(1L, config.getLong("void-drag.telegraph.pulse-interval-ticks", 4L));
        for (long tick = 0; tick <= delay; tick += interval) {
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                if (player.isOnline() && canAffect(player) && isUnderLowCeiling(player)) {
                    playTelegraph(player.getLocation(), "void-drag.telegraph",
                            Particle.WITCH, Sound.BLOCK_RESPAWN_ANCHOR_CHARGE);
                }
            }, tick);
        }
    }

    private boolean isUnderLowCeiling(Player player) {
        int height = config.getInt("void-drag.low-ceiling-height", 2);
        Location base = player.getLocation();
        for (int y = 2; y <= height; y++) {
            if (!base.clone().add(0, y, 0).getBlock().isPassable()) {
                return true;
            }
        }
        return false;
    }

    private void dragPlayer(Enderman enderman, Player player) {
        Location destination = findDragDestination(enderman);
        if (destination == null) {
            return;
        }
        player.teleport(destination);
        player.getWorld().spawnParticle(Particle.PORTAL,
                destination.clone().add(0, 1.0, 0), 45, 0.5, 0.8, 0.5, 0.04);
        player.getWorld().playSound(destination, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 0.6f);
    }

    private Location findDragDestination(Enderman enderman) {
        int radius = config.getInt("void-drag.search-radius", 4);
        Location origin = enderman.getLocation();
        for (int r = 1; r <= radius; r++) {
            for (int x = -r; x <= r; x++) {
                for (int z = -r; z <= r; z++) {
                    Location candidate = origin.clone().add(x, 0, z);
                    if (isSafeDestination(candidate)) {
                        return new Location(candidate.getWorld(), candidate.getBlockX() + 0.5,
                                candidate.getBlockY(), candidate.getBlockZ() + 0.5,
                                candidate.getYaw(), candidate.getPitch());
                    }
                }
            }
        }
        return null;
    }

    private boolean isSafeDestination(Location location) {
        return location.getBlock().isPassable()
                && location.clone().add(0, 1, 0).getBlock().isPassable()
                && !location.clone().subtract(0, 1, 0).getBlock().isPassable();
    }

    private boolean canAffect(Player player) {
        return enabled() && player.getGameMode() == GameMode.SURVIVAL;
    }

    private void playTelegraph(Location location, String path, Particle fallbackParticle,
                               Sound fallbackSound) {
        effects.playTelegraph(config, location, path, fallbackParticle, fallbackSound);
    }

    private boolean enabled() {
        return config.getBoolean("enabled", true);
    }
}
