package ru.nyansus.mc.ethos.buff;

import org.bukkit.GameMode;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.IronGolem;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityTargetEvent;
import ru.nyansus.mc.ethos.Ethos;

public class GolemAggroListener implements Listener {

    private final Ethos plugin;

    public GolemAggroListener(Ethos plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityTarget(EntityTargetEvent event) {
        if (event.getEntity().getType() != EntityType.IRON_GOLEM) {
            return;
        }
        IronGolem golem = (IronGolem) event.getEntity();

        if (event.getTarget() instanceof Player player) {
            if (!plugin.areKarmaEffectsEnabled(player)) {
                return;
            }
            int karma = plugin.getKarmaManager().getKarma(player.getUniqueId());
            if (plugin.getBuffConfig().hasEffect(karma, EffectType.GOLEM_AGGRO)) {
                return;
            }
        }

        if (event.getTarget() == null) {
            Player target = findNearbyEvilPlayer(golem);
            if (target != null) {
                event.setTarget(target);
            }
        }
    }

    private Player findNearbyEvilPlayer(IronGolem golem) {
        for (Entity entity : golem.getNearbyEntities(16, 16, 16)) {
            if (entity instanceof Player player) {
                if (!plugin.areKarmaEffectsEnabled(player)) {
                    continue;
                }
                int karma = plugin.getKarmaManager().getKarma(player.getUniqueId());
                double range = plugin.getBuffConfig()
                        .getNumericEffect(karma, EffectType.GOLEM_AGGRO);
                if (range > 0 && player.getGameMode() == GameMode.SURVIVAL
                        && golem.getLocation().distance(player.getLocation()) <= range) {
                    return player;
                }
            }
        }
        return null;
    }
}
