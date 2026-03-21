package ru.nyansus.mc.domya_fate.buff;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.metadata.MetadataValue;
import ru.nyansus.mc.domya_fate.DomyaFate;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PvpDamageListener implements Listener {

    private final DomyaFate plugin;
    private final Map<UUID, Long> lastHitTimestamp = new HashMap<>();

    public PvpDamageListener(DomyaFate plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player attacker)) {
            return;
        }
        if (!(event.getEntity() instanceof Player victim)) {
            return;
        }

        int attackerKarma = getCachedKarma(attacker);
        BuffConfig config = plugin.getBuffConfig();

        if (config.isNoFirstStrike(attackerKarma)) {
            long cooldownMs = config.getFirstStrikeCooldownMs(attackerKarma);
            Long lastHit = lastHitTimestamp.get(attacker.getUniqueId());
            if (lastHit == null || System.currentTimeMillis() - lastHit > cooldownMs) {
                Long victimLastAttack = lastHitTimestamp.get(victim.getUniqueId());
                boolean victimAttackedRecently = victimLastAttack != null
                        && System.currentTimeMillis() - victimLastAttack < cooldownMs;
                if (!victimAttackedRecently) {
                    event.setCancelled(true);
                    return;
                }
            }
        }

        double penalty = config.getPvpDamagePenalty(attackerKarma);
        if (penalty > 0) {
            event.setDamage(event.getDamage() * (1.0 - penalty));
        }

        lastHitTimestamp.put(victim.getUniqueId(), System.currentTimeMillis());
    }

    private int getCachedKarma(Player player) {
        for (MetadataValue value : player.getMetadata("domya_karma_cached")) {
            return value.asInt();
        }
        return plugin.getKarmaManager().getKarma(player.getUniqueId());
    }
}
