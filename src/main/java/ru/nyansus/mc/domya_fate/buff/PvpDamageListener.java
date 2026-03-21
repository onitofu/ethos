package ru.nyansus.mc.domya_fate.buff;

import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import ru.nyansus.mc.domya_fate.DomyaFate;

public class PvpDamageListener implements Listener {

    private final DomyaFate plugin;

    public PvpDamageListener(DomyaFate plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player attacker)) {
            return;
        }

        int attackerKarma = plugin.getKarmaManager().getKarma(attacker.getUniqueId());
        BuffConfig config = plugin.getBuffConfig();

        if (event.getEntity() instanceof Player) {
            double penalty = config.getPvpDamagePenalty(attackerKarma);
            if (penalty > 0) {
                event.setDamage(event.getDamage() * (1.0 - penalty));
            }
        } else if (event.getEntity() instanceof Mob) {
            double bonus = config.getMobDamageBonus(attackerKarma);
            if (bonus > 0) {
                event.setDamage(event.getDamage() * (1.0 + bonus));
            }
        }
    }
}
