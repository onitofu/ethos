package ru.nyansus.mc.domya_fate.buff;

import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.MerchantInventory;
import org.bukkit.inventory.MerchantRecipe;
import ru.nyansus.mc.domya_fate.DomyaFate;

import java.util.ArrayList;
import java.util.List;

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

    @EventHandler(ignoreCancelled = true)
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        if (!(event.getRightClicked() instanceof Villager villager)) {
            return;
        }

        Player player = event.getPlayer();
        int karma = plugin.getKarmaManager().getKarma(player.getUniqueId());
        BuffConfig config = plugin.getBuffConfig();

        if (config.isTradeBlocked(karma)) {
            event.setCancelled(true);
            player.sendMessage(plugin.getMessages().get(player, "karma.trade-blocked"));
            return;
        }

        int priceIncrease = config.getTradePriceIncrease(karma);
        if (priceIncrease > 0) {
            adjustPrices(villager, priceIncrease);
        }
    }

    private void adjustPrices(Villager villager, int levels) {
        List<MerchantRecipe> recipes = new ArrayList<>();
        for (MerchantRecipe original : villager.getRecipes()) {
            MerchantRecipe modified = new MerchantRecipe(
                    original.getResult(),
                    original.getUses(),
                    original.getMaxUses(),
                    original.hasExperienceReward(),
                    original.getVillagerExperience(),
                    original.getPriceMultiplier(),
                    original.getDemand(),
                    original.getSpecialPrice() + levels);
            modified.setIngredients(original.getIngredients());
            recipes.add(modified);
        }
        villager.setRecipes(recipes);
    }
}
