package ru.nyansus.mc.ethos.listener;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityTameEvent;
import org.bukkit.event.entity.PiglinBarterEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import ru.nyansus.mc.ethos.Ethos;
import ru.nyansus.mc.ethos.karma.StatsStorage;
import ru.nyansus.mc.ethos.util.StatKeys;

import java.util.UUID;

public class StatsListener implements Listener {

    private final Ethos plugin;

    public StatsListener(Ethos plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        StatsStorage stats = plugin.getStatsStorage();

        var cause = player.getLastDamageCause();
        if (cause == null) {
            return;
        }

        switch (cause.getCause()) {
            case FALL -> stats.incrementStat(player.getUniqueId(), StatKeys.FALL_DEATHS);
            case LAVA -> stats.incrementStat(player.getUniqueId(), StatKeys.LAVA_DEATHS);
            case STARVATION -> stats.incrementStat(player.getUniqueId(),
                    StatKeys.STARVATION_DEATHS);
            case DROWNING -> stats.incrementStat(player.getUniqueId(),
                    StatKeys.DROWNING_DEATHS);
            default -> { }
        }

        stats.setStat(player.getUniqueId(), StatKeys.LAST_DEATH_TIME,
                System.currentTimeMillis());
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        plugin.getStatsStorage().incrementStat(
                event.getPlayer().getUniqueId(), StatKeys.BLOCKS_PLACED);
    }

    @EventHandler(ignoreCancelled = true)
    public void onCraftItem(CraftItemEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        plugin.getStatsStorage().incrementStat(player.getUniqueId(), StatKeys.ITEMS_CRAFTED);
    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();
        StatsStorage stats = plugin.getStatsStorage();
        UUID uuid = player.getUniqueId();

        String env = player.getWorld().getEnvironment().name().toLowerCase();
        String key = StatKeys.VISITED_PREFIX + env;

        if (stats.getStat(uuid, key) == 0) {
            stats.setStat(uuid, key, 1);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPiglinBarter(PiglinBarterEvent event) {
        var piglin = event.getEntity();
        var location = piglin.getLocation();
        Player closest = null;
        double closestDist = Double.MAX_VALUE;
        for (Player player : piglin.getWorld().getNearbyPlayers(location, 10)) {
            double dist = player.getLocation().distanceSquared(location);
            if (dist < closestDist) {
                closestDist = dist;
                closest = player;
            }
        }
        if (closest != null) {
            plugin.getStatsStorage().incrementStat(
                    closest.getUniqueId(), StatKeys.PIGLIN_BARTERS);
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onEntityTame(EntityTameEvent event) {
        if (!(event.getOwner() instanceof Player player)) {
            return;
        }
        StatsStorage stats = plugin.getStatsStorage();
        String entityType = event.getEntityType().name().toLowerCase();
        stats.incrementStat(player.getUniqueId(), StatKeys.TAMED_PREFIX + entityType);
        stats.incrementStat(player.getUniqueId(), StatKeys.TAMED_TOTAL);
    }
}
