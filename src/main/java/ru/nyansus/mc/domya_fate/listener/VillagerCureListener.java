package ru.nyansus.mc.domya_fate.listener;

import org.bukkit.OfflinePlayer;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityTransformEvent;
import ru.nyansus.mc.domya_fate.DomyaFate;

public class VillagerCureListener implements Listener {

    private final DomyaFate plugin;

    public VillagerCureListener(DomyaFate plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityTransform(EntityTransformEvent event) {
        if (event.getTransformReason() != EntityTransformEvent.TransformReason.CURED) {
            return;
        }

        var entity = event.getEntity();
        if (entity instanceof org.bukkit.entity.ZombieVillager zombieVillager) {
            OfflinePlayer converter = zombieVillager.getConversionPlayer();
            if (converter != null) {
                int karmaChange = plugin.getConfig().getInt("karma-actions.cure-zombie-villager", 30);
                plugin.getKarmaManager().addKarma(converter.getUniqueId(), karmaChange);
                plugin.getStatsStorage().incrementStat(converter.getUniqueId(),
                        ru.nyansus.mc.domya_fate.util.StatKeys.ZOMBIE_VILLAGER_CURES);
            }
        }
    }
}
