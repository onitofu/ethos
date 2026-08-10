package ru.nyansus.mc.ethos.buff;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityTameEvent;
import ru.nyansus.mc.ethos.Ethos;

public class BlockTamingListener implements Listener {

    private final Ethos plugin;

    public BlockTamingListener(Ethos plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOW)
    public void onEntityTame(EntityTameEvent event) {
        if (!(event.getOwner() instanceof Player player)) {
            return;
        }
        if (!plugin.areKarmaEffectsEnabled(player)) {
            return;
        }
        int karma = plugin.getKarmaManager().getKarma(player.getUniqueId());
        if (plugin.getBuffConfig().hasEffect(karma, EffectType.BLOCK_TAMING)) {
            event.setCancelled(true);
        }
    }
}
