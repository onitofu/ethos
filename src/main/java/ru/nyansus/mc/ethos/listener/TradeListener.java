package ru.nyansus.mc.ethos.listener;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.MerchantInventory;
import ru.nyansus.mc.ethos.Ethos;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class TradeListener implements Listener {

    private final Ethos plugin;
    private final Map<UUID, Long> lastTradeKarma = new HashMap<>();

    public TradeListener(Ethos plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getInventory() instanceof MerchantInventory)) {
            return;
        }
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (event.getSlot() != 2) {
            return;
        }
        if (event.getCurrentItem() == null || event.getCurrentItem().getType().isAir()) {
            return;
        }

        long cooldownMs = plugin.getConfig().getLong("anti-farm.trade-cooldown-seconds", 60) * 1000L;
        long now = System.currentTimeMillis();
        Long lastTime = lastTradeKarma.get(player.getUniqueId());

        if (lastTime != null && now - lastTime < cooldownMs) {
            return;
        }

        lastTradeKarma.put(player.getUniqueId(), now);
        int karmaChange = plugin.getConfig().getInt("karma-actions.villager-trade", 1);
        plugin.getKarmaManager().addKarma(player.getUniqueId(), karmaChange);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        lastTradeKarma.remove(event.getPlayer().getUniqueId());
    }
}
