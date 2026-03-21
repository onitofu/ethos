package ru.nyansus.mc.domya_fate.listener;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.MerchantInventory;
import ru.nyansus.mc.domya_fate.DomyaFate;

public class TradeListener implements Listener {

    private final DomyaFate plugin;

    public TradeListener(DomyaFate plugin) {
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

        int karmaChange = plugin.getConfig().getInt("karma-actions.villager-trade", 2);
        plugin.getKarmaManager().addKarma(player.getUniqueId(), karmaChange);
    }
}
