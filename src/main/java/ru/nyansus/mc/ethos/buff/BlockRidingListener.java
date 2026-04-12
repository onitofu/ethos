package ru.nyansus.mc.ethos.buff;

import org.bukkit.entity.Animals;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.vehicle.VehicleEnterEvent;
import ru.nyansus.mc.ethos.Ethos;

public class BlockRidingListener implements Listener {

    private final Ethos plugin;

    public BlockRidingListener(Ethos plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true)
    public void onVehicleEnter(VehicleEnterEvent event) {
        if (!(event.getEntered() instanceof Player player)) {
            return;
        }
        if (!(event.getVehicle() instanceof Animals)) {
            return;
        }
        int karma = plugin.getKarmaManager().getKarma(player.getUniqueId());
        if (plugin.getBuffConfig().hasEffect(karma, EffectType.BLOCK_RIDING)) {
            event.setCancelled(true);
        }
    }
}
