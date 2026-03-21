package ru.nyansus.mc.domya_fate.buff;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Entity;
import org.bukkit.entity.IronGolem;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.scheduler.BukkitRunnable;
import ru.nyansus.mc.domya_fate.DomyaFate;
import ru.nyansus.mc.domya_fate.karma.KarmaManager;
import ru.nyansus.mc.domya_fate.title.Title;

import java.util.Optional;

public class BuffApplyTask extends BukkitRunnable {

    private final DomyaFate plugin;
    private final KarmaManager karmaManager;

    public BuffApplyTask(DomyaFate plugin, KarmaManager karmaManager) {
        this.plugin = plugin;
        this.karmaManager = karmaManager;
    }

    @Override
    public void run() {
        BuffConfig config = plugin.getBuffConfig();
        for (var player : plugin.getServer().getOnlinePlayers()) {
            int karma = karmaManager.getKarma(player.getUniqueId());
            player.removeMetadata("domya_karma_cached", plugin);
            player.setMetadata("domya_karma_cached",
                    new org.bukkit.metadata.FixedMetadataValue(plugin, karma));

            plugin.getAntiFarmManager().updatePosition(player);
            applyEffects(player, karma, config);
            if (config.isGolemAggro(karma)) {
                aggroNearbyGolems(player, config.getGolemAggroRange(karma));
            }
            if (config.isTabEnabled()) {
                updateTabName(player, karma);
            }
        }
    }

    private void applyEffects(Player player, int karma, BuffConfig config) {
        BuffTier negativeTier = config.findNegativeTier(karma);
        BuffTier positiveTier = config.findPositiveTier(karma);

        int duration = config.getEffectDuration();

        if (negativeTier != null) {
            for (EffectEntry entry : negativeTier.effects()) {
                player.addPotionEffect(
                        new PotionEffect(entry.type(), duration, entry.amplifier(),
                                entry.ambient(), !entry.ambient()));
            }
            if (negativeTier.speedBonus() > 0) {
                player.addPotionEffect(
                        new PotionEffect(org.bukkit.potion.PotionEffectType.SPEED,
                                duration, 0, true, false));
            }
        }

        if (positiveTier != null) {
            for (EffectEntry entry : positiveTier.effects()) {
                player.addPotionEffect(
                        new PotionEffect(entry.type(), duration, entry.amplifier(),
                                entry.ambient(), !entry.ambient()));
            }
        }
    }

    private void aggroNearbyGolems(Player player, double range) {
        for (Entity entity : player.getNearbyEntities(range, range, range)) {
            if (entity instanceof IronGolem golem && golem.getTarget() == null) {
                golem.setTarget(player);
            }
        }
    }

    private void updateTabName(Player player, int karma) {
        NamedTextColor karmaColor = karma > 0 ? NamedTextColor.GREEN
                : karma < 0 ? NamedTextColor.RED : NamedTextColor.GRAY;

        Optional<Title> title = plugin.getKarmaTitleManager().getTitle(karma)
                .flatMap(kt -> plugin.getTitleManager().getRegistry().getTitle(kt.id()));

        Component name;
        if (title.isPresent()) {
            NamedTextColor titleColor = parseColor(title.get().color());
            name = Component.text("")
                    .append(Component.text(title.get().nameRu(), titleColor))
                    .append(Component.text(" "))
                    .append(Component.text(player.getName()))
                    .append(Component.text(" "))
                    .append(Component.text("[" + karma + "]", karmaColor));
        } else {
            name = Component.text(player.getName())
                    .append(Component.text(" "))
                    .append(Component.text("[" + karma + "]", karmaColor));
        }

        player.playerListName(name);
    }

    private static NamedTextColor parseColor(String color) {
        NamedTextColor parsed = NamedTextColor.NAMES.value(color);
        return parsed != null ? parsed : NamedTextColor.WHITE;
    }
}
