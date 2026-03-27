package ru.nyansus.mc.domya_fate.listener;

import org.bukkit.Material;
import org.bukkit.entity.Animals;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.ItemStack;
import ru.nyansus.mc.domya_fate.DomyaFate;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class AnimalListener implements Listener {

    private final DomyaFate plugin;
    private final Map<UUID, Long> lastFeedKarma = new HashMap<>();

    public AnimalListener(DomyaFate plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        if (!(event.getRightClicked() instanceof Animals animal)) {
            return;
        }

        Player player = event.getPlayer();
        ItemStack hand = player.getInventory().getItem(event.getHand());
        if (hand == null || hand.getType() == Material.AIR) {
            return;
        }

        if (animal.isBreedItem(hand) && animal.canBreed()) {
            long cooldownMs = plugin.getConfig().getLong("anti-farm.feed-cooldown-seconds", 30)
                    * 1000L;
            long now = System.currentTimeMillis();
            Long lastTime = lastFeedKarma.get(player.getUniqueId());

            if (lastTime != null && now - lastTime < cooldownMs) {
                return;
            }

            lastFeedKarma.put(player.getUniqueId(), now);
            int karmaChange = plugin.getConfig().getInt("karma-actions.feed-animal", 1);
            plugin.getKarmaManager().addKarma(player.getUniqueId(), karmaChange);
        }
    }
}
