package ru.nyansus.mc.domya_fate.listener;

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
import ru.nyansus.mc.domya_fate.DomyaFate;
import ru.nyansus.mc.domya_fate.karma.StatsStorage;

import java.util.UUID;

public class StatsListener implements Listener {

    private final DomyaFate plugin;

    public StatsListener(DomyaFate plugin) {
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
            case FALL -> stats.incrementStat(player.getUniqueId(), "fall-deaths");
            case LAVA -> stats.incrementStat(player.getUniqueId(), "lava-deaths");
            case STARVATION -> stats.incrementStat(player.getUniqueId(), "starvation-deaths");
            case DROWNING -> stats.incrementStat(player.getUniqueId(), "drowning-deaths");
            default -> { }
        }

        stats.setStat(player.getUniqueId(), "last-death-time", System.currentTimeMillis());
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        plugin.getStatsStorage().incrementStat(event.getPlayer().getUniqueId(), "blocks-placed");
    }

    @EventHandler(ignoreCancelled = true)
    public void onCraftItem(CraftItemEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        plugin.getStatsStorage().incrementStat(player.getUniqueId(), "items-crafted");
    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();
        StatsStorage stats = plugin.getStatsStorage();
        UUID uuid = player.getUniqueId();

        String env = player.getWorld().getEnvironment().name().toLowerCase();
        String key = "visited-" + env;

        if (stats.getStat(uuid, key) == 0) {
            stats.setStat(uuid, key, 1);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPiglinBarter(PiglinBarterEvent event) {
        var piglin = event.getEntity();
        var nearest = piglin.getWorld().getNearbyPlayers(piglin.getLocation(), 10);
        if (nearest.isEmpty()) {
            return;
        }
        Player closest = nearest.iterator().next();
        plugin.getStatsStorage().incrementStat(closest.getUniqueId(), "piglin-barters");
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onEntityTame(EntityTameEvent event) {
        if (!(event.getOwner() instanceof Player player)) {
            return;
        }
        StatsStorage stats = plugin.getStatsStorage();
        String entityType = event.getEntityType().name().toLowerCase();
        stats.incrementStat(player.getUniqueId(), "tamed-" + entityType);
        stats.incrementStat(player.getUniqueId(), "tamed-total");
    }
}
