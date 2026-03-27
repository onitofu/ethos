package ru.nyansus.mc.domya_fate.buff;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityTameEvent;
import ru.nyansus.mc.domya_fate.DomyaFate;

public class BlockTamingListener implements Listener {

    private final DomyaFate plugin;

    public BlockTamingListener(DomyaFate plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityTame(EntityTameEvent event) {
        if (!(event.getOwner() instanceof Player player)) {
            return;
        }
        int karma = plugin.getKarmaManager().getKarma(player.getUniqueId());
        if (plugin.getBuffConfig().hasEffect(karma, EffectType.BLOCK_TAMING)) {
            event.setCancelled(true);
        }
    }
}
