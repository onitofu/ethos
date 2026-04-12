package ru.nyansus.mc.ethos.buff;

import org.bukkit.entity.Animals;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import ru.nyansus.mc.ethos.Ethos;

public class PvpDamageListener implements Listener {

    private final Ethos plugin;

    public PvpDamageListener(Ethos plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.NORMAL)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player attacker)) {
            return;
        }

        int karma = plugin.getKarmaManager().getKarma(attacker.getUniqueId());
        BuffConfig config = plugin.getBuffConfig();

        if (event.getEntity() instanceof Player) {
            double penalty = config.getNumericEffect(karma, EffectType.PVP_DAMAGE_PENALTY);
            double bonus = config.getNumericEffect(karma, EffectType.PVP_DAMAGE_BONUS);
            double multiplier = 1.0 + bonus - penalty;
            if (multiplier != 1.0) {
                event.setDamage(event.getDamage() * multiplier);
            }
        } else if (event.getEntity() instanceof Mob) {
            double mobBonus = config.getNumericEffect(karma, EffectType.MOB_DAMAGE_BONUS);
            if (event.getEntity() instanceof Animals) {
                double passiveBonus = config.getNumericEffect(
                        karma, EffectType.PASSIVE_MOB_DAMAGE_BONUS);
                mobBonus += passiveBonus;
            }
            if (mobBonus != 0) {
                event.setDamage(event.getDamage() * (1.0 + mobBonus));
            }
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.NORMAL)
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        int karma = plugin.getKarmaManager().getKarma(player.getUniqueId());
        BuffConfig config = plugin.getBuffConfig();

        double resistance = config.getNumericEffect(karma, EffectType.RESISTANCE);
        if (resistance > 0) {
            event.setDamage(event.getDamage() * (1.0 - resistance));
        }

        if (event.getCause() == EntityDamageEvent.DamageCause.FALL) {
            double fallReduction = config.getNumericEffect(karma, EffectType.FALL_DAMAGE_REDUCTION);
            if (fallReduction > 0) {
                event.setDamage(event.getDamage() * (1.0 - fallReduction));
            }
        }

        if (event.getCause() == EntityDamageEvent.DamageCause.FIRE
                || event.getCause() == EntityDamageEvent.DamageCause.FIRE_TICK
                || event.getCause() == EntityDamageEvent.DamageCause.LAVA) {
            double fireRes = config.getNumericEffect(karma, EffectType.FIRE_RESISTANCE);
            if (fireRes > 0) {
                event.setDamage(event.getDamage() * (1.0 - fireRes));
            }
        }
    }
}
