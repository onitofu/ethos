package ru.nyansus.mc.domya_fate.buff;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.GameMode;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import ru.nyansus.mc.domya_fate.DomyaFate;
import ru.nyansus.mc.domya_fate.karma.KarmaManager;
import ru.nyansus.mc.domya_fate.title.Title;

import java.util.Optional;

public class BuffApplyTask extends BukkitRunnable {

    private static final NamespacedKey HEALTH_KEY =
            new NamespacedKey("ethos", "health_bonus");

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

            plugin.getAntiFarmManager().updatePosition(player);

            if (player.getGameMode() != GameMode.SURVIVAL) {
                if (config.isTabEnabled()) {
                    updateTabName(player, karma);
                }
                continue;
            }

            applyEffects(player, karma, config);


            if (config.isTabEnabled()) {
                updateTabName(player, karma);
            }
        }
    }

    private void applyEffects(Player player, int karma, BuffConfig config) {
        BuffTier tier = config.findTier(karma);
        if (tier == null) {
            removeHealthModifier(player);
            return;
        }

        int duration = config.getEffectDuration();

        for (BuffEffect effect : tier.effects()) {
            switch (effect.effectType()) {
                case POTION_EFFECT -> applyPotionIfStronger(player, effect, duration);
                case SPEED_BONUS -> player.addPotionEffect(
                        new PotionEffect(PotionEffectType.SPEED, duration, 0, true, false));
                case NIGHT_VISION -> player.addPotionEffect(
                        new PotionEffect(PotionEffectType.NIGHT_VISION,
                                duration, 0, true, false));
                case REGENERATION_BONUS -> player.addPotionEffect(
                        new PotionEffect(PotionEffectType.REGENERATION, duration,
                                (int) effect.value(), true, false));
                case MINING_SPEED -> {
                    int amp = (int) effect.value();
                    if (amp >= 0) {
                        player.addPotionEffect(new PotionEffect(
                                PotionEffectType.HASTE, duration, amp, true, false));
                    } else {
                        player.addPotionEffect(new PotionEffect(
                                PotionEffectType.MINING_FATIGUE, duration,
                                Math.abs(amp) - 1, true, false));
                    }
                }
                case GLOWING -> player.addPotionEffect(
                        new PotionEffect(PotionEffectType.GLOWING,
                                duration, 0, true, false));
                case HEALTH_BONUS -> applyHealthModifier(player, effect.value());
                default -> { }
            }
        }
    }

    private void applyPotionIfStronger(Player player, BuffEffect effect, int duration) {
        PotionEffect existing = player.getPotionEffect(effect.potionType());
        if (existing != null && existing.getAmplifier() > effect.amplifier()) {
            return;
        }
        player.addPotionEffect(new PotionEffect(effect.potionType(), duration,
                effect.amplifier(), effect.ambient(), !effect.ambient()));
    }

    private void applyHealthModifier(Player player, double bonus) {
        AttributeInstance attr = player.getAttribute(Attribute.MAX_HEALTH);
        if (attr == null) {
            return;
        }
        removeHealthModifier(player);
        attr.addTransientModifier(new AttributeModifier(
                HEALTH_KEY, bonus, AttributeModifier.Operation.ADD_NUMBER));
    }

    private void removeHealthModifier(Player player) {
        AttributeInstance attr = player.getAttribute(Attribute.MAX_HEALTH);
        if (attr == null) {
            return;
        }
        attr.removeModifier(HEALTH_KEY);
    }

    private void updateTabName(Player player, int karma) {
        NamedTextColor karmaColor = karma > 0 ? NamedTextColor.GREEN
                : karma < 0 ? NamedTextColor.RED : NamedTextColor.GRAY;

        Optional<Title> title = plugin.getKarmaTitleManager().getTitle(karma)
                .flatMap(kt -> plugin.getTitleManager().getRegistry().getTitle(kt.id()));

        Component name;
        if (title.isPresent()) {
            NamedTextColor titleColor = parseColor(title.get().color().primary());
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
