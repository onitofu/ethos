package ru.nyansus.mc.ethos.buff;

import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.MerchantInventory;
import org.bukkit.inventory.MerchantRecipe;
import ru.nyansus.mc.ethos.Ethos;

import java.util.ArrayList;
import java.util.List;

public class TradeBlockListener implements Listener {

    private final Ethos plugin;

    public TradeBlockListener(Ethos plugin) {
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
        if (plugin.getBuffConfig().hasEffect(karma, EffectType.BLOCK_TRADING)) {
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

        if (config.hasEffect(karma, EffectType.BLOCK_TRADING)) {
            event.setCancelled(true);
            player.sendMessage(plugin.getMessages().get(player, "karma.trade-blocked"));
            return;
        }

        int priceIncrease = (int) config.getNumericEffect(karma, EffectType.TRADE_PRICE_INCREASE);
        int priceDecrease = (int) config.getNumericEffect(karma, EffectType.TRADE_PRICE_DECREASE);
        int priceChange = priceIncrease - priceDecrease;

        if (priceChange != 0) {
            adjustPrices(villager, priceChange);
        }
    }

    private void adjustPrices(Villager villager, int change) {
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
                    original.getSpecialPrice() + change);
            modified.setIngredients(original.getIngredients());
            recipes.add(modified);
        }
        villager.setRecipes(recipes);
    }
}
