package ru.nyansus.mc.ethos.buff;

import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;
import ru.nyansus.mc.ethos.Ethos;

import java.util.concurrent.ThreadLocalRandom;

public class KarmaEffectsListener implements Listener {

    private final Ethos plugin;

    public KarmaEffectsListener(Ethos plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        if (!plugin.areKarmaEffectsEnabled(player)) {
            return;
        }
        int karma = plugin.getKarmaManager().getKarma(player.getUniqueId());
        BuffConfig config = plugin.getBuffConfig();

        double keepChance = config.getNumericEffect(karma, EffectType.KEEP_INVENTORY_CHANCE);
        if (keepChance > 0 && ThreadLocalRandom.current().nextDouble() < keepChance) {
            event.setKeepInventory(true);
            event.getDrops().clear();
            event.setKeepLevel(true);
            event.setDroppedExp(0);
            return;
        }

        double xpDeathPenalty = config.getNumericEffect(karma, EffectType.XP_DEATH_PENALTY);
        if (xpDeathPenalty > 0) {
            int extra = (int) (event.getDroppedExp() * xpDeathPenalty);
            event.setDroppedExp(Math.max(0, event.getDroppedExp() - extra));
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent event) {
        Player killer = event.getEntity().getKiller();
        if (killer == null) {
            return;
        }
        if (!plugin.areKarmaEffectsEnabled(killer)) {
            return;
        }

        int karma = plugin.getKarmaManager().getKarma(killer.getUniqueId());
        double lootBonus = plugin.getBuffConfig().getNumericEffect(karma, EffectType.LOOT_BONUS);
        if (lootBonus <= 0) {
            return;
        }

        for (ItemStack drop : new java.util.ArrayList<>(event.getDrops())) {
            if (ThreadLocalRandom.current().nextDouble() < lootBonus) {
                event.getDrops().add(drop.clone());
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onFoodLevelChange(FoodLevelChangeEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        if (event.getFoodLevel() >= player.getFoodLevel()) {
            return;
        }
        if (!plugin.areKarmaEffectsEnabled(player)) {
            return;
        }

        int karma = plugin.getKarmaManager().getKarma(player.getUniqueId());
        double rate = plugin.getBuffConfig().getNumericEffect(karma, EffectType.HUNGER_RATE);
        if (rate > 1.0) {
            int loss = player.getFoodLevel() - event.getFoodLevel();
            int extraLoss = (int) (loss * (rate - 1.0));
            event.setFoodLevel(Math.max(0, event.getFoodLevel() - extraLoss));
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (!plugin.areKarmaEffectsEnabled(player)) {
            return;
        }
        Block block = event.getBlock();

        if (!(block.getBlockData() instanceof Ageable ageable)) {
            return;
        }
        if (ageable.getAge() < ageable.getMaximumAge()) {
            return;
        }

        int karma = plugin.getKarmaManager().getKarma(player.getUniqueId());
        double chance = plugin.getBuffConfig()
                .getNumericEffect(karma, EffectType.DOUBLE_CROP_CHANCE);
        if (chance > 0 && ThreadLocalRandom.current().nextDouble() < chance) {
            for (ItemStack drop : block.getDrops(player.getInventory().getItemInMainHand())) {
                block.getWorld().dropItemNaturally(block.getLocation(), drop);
            }
        }
    }
}
