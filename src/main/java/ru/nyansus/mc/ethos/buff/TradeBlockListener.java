package ru.nyansus.mc.ethos.buff;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.MerchantInventory;
import org.bukkit.inventory.MerchantRecipe;
import org.bukkit.persistence.PersistentDataType;
import ru.nyansus.mc.ethos.Ethos;

import java.util.ArrayList;
import java.util.List;

public class TradeBlockListener implements Listener {

    private final Ethos plugin;
    private final NamespacedKey priceChangeKey;

    public TradeBlockListener(Ethos plugin) {
        this.plugin = plugin;
        this.priceChangeKey = new NamespacedKey(plugin, "trade_price_change");
    }

    @EventHandler(ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getInventory() instanceof MerchantInventory inventory)) {
            return;
        }
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (!plugin.areKarmaEffectsEnabled(player)) {
            if (inventory.getMerchant() instanceof Villager villager) {
                resetPrices(villager);
            }
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
        if (!plugin.areKarmaEffectsEnabled(player)) {
            resetPrices(villager);
            return;
        }
        int karma = plugin.getKarmaManager().getKarma(player.getUniqueId());
        BuffConfig config = plugin.getBuffConfig();

        if (config.hasEffect(karma, EffectType.BLOCK_TRADING)) {
            resetPrices(villager);
            event.setCancelled(true);
            player.sendMessage(plugin.getMessages().get(player, "karma.trade-blocked"));
            return;
        }

        int priceIncrease = (int) config.getNumericEffect(karma, EffectType.TRADE_PRICE_INCREASE);
        int priceDecrease = (int) config.getNumericEffect(karma, EffectType.TRADE_PRICE_DECREASE);
        int priceChange = priceIncrease - priceDecrease;

        applyPriceChange(villager, priceChange);
    }

    private void applyPriceChange(Villager villager, int priceChange) {
        int previous = villager.getPersistentDataContainer()
                .getOrDefault(priceChangeKey, PersistentDataType.INTEGER, 0);
        int delta = priceChange - previous;
        if (delta != 0) {
            adjustPrices(villager, delta);
        }
        if (priceChange == 0) {
            villager.getPersistentDataContainer().remove(priceChangeKey);
        } else {
            villager.getPersistentDataContainer()
                    .set(priceChangeKey, PersistentDataType.INTEGER, priceChange);
        }
    }

    private void resetPrices(Villager villager) {
        applyPriceChange(villager, 0);
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
