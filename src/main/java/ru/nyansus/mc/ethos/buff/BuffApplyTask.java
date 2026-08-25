package ru.nyansus.mc.ethos.buff;

import org.bukkit.GameMode;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import ru.nyansus.mc.ethos.Ethos;
import ru.nyansus.mc.ethos.karma.KarmaManager;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class BuffApplyTask extends BukkitRunnable {

    private static final NamespacedKey HEALTH_KEY =
            new NamespacedKey("ethos", "health_bonus");

    private final Ethos plugin;
    private final KarmaManager karmaManager;
    private final Map<UUID, Map<PotionEffectType, OwnedPotionEffect>> ownedPotionEffects =
            new HashMap<>();

    public BuffApplyTask(Ethos plugin, KarmaManager karmaManager) {
        this.plugin = plugin;
        this.karmaManager = karmaManager;
    }

    @Override
    public void run() {
        BuffConfig config = plugin.getBuffConfig();
        for (var player : plugin.getServer().getOnlinePlayers()) {
            int karma = karmaManager.getKarma(player.getUniqueId());

            plugin.getAntiFarmManager().updatePosition(player);

            if (!plugin.areKarmaEffectsEnabled(player)) {
                clearAppliedEffects(player);
                continue;
            }

            if (player.getGameMode() != GameMode.SURVIVAL) {
                continue;
            }

            applyEffects(player, karma, config);
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
                case SPEED_BONUS -> applyOwnedPotionEffect(player,
                        new PotionEffect(PotionEffectType.SPEED, duration, 0, true, false));
                case NIGHT_VISION -> applyOwnedPotionEffect(player,
                        new PotionEffect(PotionEffectType.NIGHT_VISION,
                                duration, 0, true, false));
                case REGENERATION_BONUS -> applyOwnedPotionEffect(player,
                        new PotionEffect(PotionEffectType.REGENERATION, duration,
                                (int) effect.value(), true, false));
                case MINING_SPEED -> {
                    int amp = (int) effect.value();
                    if (amp >= 0) {
                        applyOwnedPotionEffect(player, new PotionEffect(
                                PotionEffectType.HASTE, duration, amp, true, false));
                    } else {
                        applyOwnedPotionEffect(player, new PotionEffect(
                                PotionEffectType.MINING_FATIGUE, duration,
                                Math.abs(amp) - 1, true, false));
                    }
                }
                case GLOWING -> applyOwnedPotionEffect(player,
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
        applyOwnedPotionEffect(player, new PotionEffect(effect.potionType(), duration,
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

    public void clearAppliedEffects(Player player) {
        removeHealthModifier(player);
        Map<PotionEffectType, OwnedPotionEffect> effects =
                ownedPotionEffects.remove(player.getUniqueId());
        if (effects == null) {
            return;
        }
        for (Map.Entry<PotionEffectType, OwnedPotionEffect> entry : effects.entrySet()) {
            PotionEffect current = player.getPotionEffect(entry.getKey());
            if (entry.getValue().matches(current)) {
                player.removePotionEffect(entry.getKey());
            }
        }
    }

    public void forgetPlayer(UUID uuid) {
        ownedPotionEffects.remove(uuid);
    }

    private void applyOwnedPotionEffect(Player player, PotionEffect effect) {
        if (player.addPotionEffect(effect)) {
            ownedPotionEffects.computeIfAbsent(player.getUniqueId(), ignored -> new HashMap<>())
                    .put(effect.getType(), OwnedPotionEffect.from(effect));
        }
    }

    private static void removeHealthModifier(Player player) {
        AttributeInstance attr = player.getAttribute(Attribute.MAX_HEALTH);
        if (attr == null) {
            return;
        }
        attr.removeModifier(HEALTH_KEY);
    }

    private record OwnedPotionEffect(
            int amplifier,
            boolean ambient,
            boolean particles,
            boolean icon
    ) {
        private static OwnedPotionEffect from(PotionEffect effect) {
            return new OwnedPotionEffect(
                    effect.getAmplifier(),
                    effect.isAmbient(),
                    effect.hasParticles(),
                    effect.hasIcon()
            );
        }

        private boolean matches(PotionEffect effect) {
            return effect != null
                    && !effect.isInfinite()
                    && effect.getAmplifier() == amplifier
                    && effect.isAmbient() == ambient
                    && effect.hasParticles() == particles
                    && effect.hasIcon() == icon;
        }
    }
}
