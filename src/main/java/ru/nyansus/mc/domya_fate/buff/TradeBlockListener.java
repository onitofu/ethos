package ru.nyansus.mc.domya_fate.buff;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.MerchantInventory;
import ru.nyansus.mc.domya_fate.DomyaFate;

public class TradeBlockListener implements Listener {

    private final DomyaFate plugin;

    public TradeBlockListener(DomyaFate plugin) {
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

        int karma = plugin.getKarmaManager().getKarma(player.getUniqueId());
        if (plugin.getBuffConfig().isTradeBlocked(karma)) {
            event.setCancelled(true);
            player.sendMessage(plugin.getMessages().get(player, "karma.trade-blocked"));
        }
    }
}
