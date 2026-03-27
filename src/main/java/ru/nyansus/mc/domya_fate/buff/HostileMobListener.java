package ru.nyansus.mc.domya_fate.buff;

import org.bukkit.GameMode;
import org.bukkit.entity.Enemy;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityTargetEvent.TargetReason;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import ru.nyansus.mc.domya_fate.DomyaFate;

import java.util.Set;

public class HostileMobListener implements Listener {

    private static final Set<TargetReason> RETALIATION_REASONS = Set.of(
            TargetReason.TARGET_ATTACKED_ENTITY,
            TargetReason.TARGET_ATTACKED_NEARBY_ENTITY,
            TargetReason.OWNER_ATTACKED_TARGET,
            TargetReason.CUSTOM
    );

    private final DomyaFate plugin;

    public HostileMobListener(DomyaFate plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityTarget(EntityTargetLivingEntityEvent event) {
        if (!(event.getEntity() instanceof Enemy)) {
            return;
        }
        if (!(event.getTarget() instanceof Player player)) {
            return;
        }
        if (player.getGameMode() != GameMode.SURVIVAL) {
            return;
        }

        int karma = plugin.getKarmaManager().getKarma(player.getUniqueId());
        BuffConfig config = plugin.getBuffConfig();

        if (config.hasEffect(karma, EffectType.HOSTILE_MOB_NEUTRAL)) {
            if (!RETALIATION_REASONS.contains(event.getReason())) {
                event.setCancelled(true);
            }
            return;
        }

        double reducedRange = config.getNumericEffect(karma, EffectType.HOSTILE_MOB_REDUCED_RANGE);
        if (reducedRange > 0) {
            double distance = event.getEntity().getLocation()
                    .distance(player.getLocation());
            if (distance > reducedRange) {
                event.setCancelled(true);
            }
        }
    }
}
