package ru.nyansus.mc.domya_fate.listener;

import org.bukkit.entity.Animals;
import org.bukkit.entity.Enemy;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.IronGolem;
import org.bukkit.entity.Player;
import org.bukkit.entity.Tameable;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import ru.nyansus.mc.domya_fate.DomyaFate;

import java.util.Set;

public class MobKillListener implements Listener {

    private static final Set<EntityType> BOSSES = Set.of(
            EntityType.ENDER_DRAGON, EntityType.WITHER
    );

    private static final Set<EntityType> NO_KARMA = Set.of(
            EntityType.SILVERFISH
    );

    private final DomyaFate plugin;

    public MobKillListener(DomyaFate plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        Entity entity = event.getEntity();
        Player killer = event.getEntity().getKiller();
        if (killer == null) {
            return;
        }

        if (plugin.getAntiFarmManager().isAfk(killer)) {
            return;
        }

        if (plugin.getAntiFarmManager().isMobStreakExceeded(
                killer.getUniqueId(), entity.getType())) {
            return;
        }

        int karmaChange = calculateKarmaChange(entity);
        if (karmaChange == 0) {
            return;
        }

        if (entity.fromMobSpawner()) {
            karmaChange = plugin.getAntiFarmManager().applySpawnerMultiplier(karmaChange);
            if (karmaChange == 0) {
                return;
            }
        }

        plugin.getKarmaManager().addKarma(killer.getUniqueId(), karmaChange);
    }

    private int calculateKarmaChange(Entity entity) {
        if (entity instanceof Villager) {
            return getConfig("kill-villager", -25);
        }
        if (entity instanceof IronGolem) {
            return getConfig("kill-golem", -15);
        }
        if (entity instanceof Tameable tameable && tameable.isTamed()) {
            return getConfig("kill-tamed", -20);
        }
        if (BOSSES.contains(entity.getType())) {
            return getConfig("kill-boss", 50);
        }
        if (NO_KARMA.contains(entity.getType())) {
            return 0;
        }
        if (entity instanceof Enemy) {
            return getConfig("kill-hostile", 1);
        }
        if (entity instanceof Animals) {
            return getConfig("kill-passive", -3);
        }
        return 0;
    }

    private int getConfig(String key, int defaultValue) {
        return plugin.getConfig().getInt("karma-actions." + key, defaultValue);
    }
}
