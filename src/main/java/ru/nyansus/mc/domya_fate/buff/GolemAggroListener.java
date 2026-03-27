package ru.nyansus.mc.domya_fate.buff;

import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityTargetEvent;
import ru.nyansus.mc.domya_fate.DomyaFate;

public class GolemAggroListener implements Listener {

    private final DomyaFate plugin;

    public GolemAggroListener(DomyaFate plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityTarget(EntityTargetEvent event) {
        if (event.getEntity().getType() != EntityType.IRON_GOLEM) {
            return;
        }

        if (event.getTarget() instanceof Player player) {
            int karma = plugin.getKarmaManager().getKarma(player.getUniqueId());
            if (plugin.getBuffConfig().hasEffect(karma, EffectType.GOLEM_AGGRO)) {
                return;
            }
        }

        if (event.getTarget() == null && event.getEntity().getCustomName() == null) {
            var golem = (org.bukkit.entity.IronGolem) event.getEntity();
            if (golem.getTarget() instanceof Player current) {
                int karma = plugin.getKarmaManager().getKarma(current.getUniqueId());
                if (plugin.getBuffConfig().hasEffect(karma, EffectType.GOLEM_AGGRO)) {
                    event.setCancelled(true);
                }
            }
        }
    }
}
