package ru.nyansus.mc.domya_fate.buff;

import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class BuffConfig {

    private static final Logger LOGGER = Logger.getLogger(BuffConfig.class.getName());

    private final int effectDuration;
    private final boolean tabEnabled;
    private final List<BuffTier> negativeTiers;
    private final List<BuffTier> positiveTiers;

    public BuffConfig(FileConfiguration config) {
        this.effectDuration = config.getInt("buffs.effect-duration", 1400);
        this.tabEnabled = config.getBoolean("tab.enabled", true);

        this.negativeTiers = loadTiers(config, "buffs.negative", true);
        this.positiveTiers = loadTiers(config, "buffs.positive", false);
    }

    private List<BuffTier> loadTiers(FileConfiguration config, String path, boolean negative) {
        List<BuffTier> tiers = new ArrayList<>();
        ConfigurationSection section = config.getConfigurationSection(path);
        if (section == null) {
            return tiers;
        }

        for (String key : section.getKeys(false)) {
            int threshold;
            try {
                threshold = Integer.parseInt(key);
            } catch (NumberFormatException e) {
                LOGGER.warning("Invalid buff threshold: " + key);
                continue;
            }

            ConfigurationSection tierSection = section.getConfigurationSection(key);
            if (tierSection == null) {
                continue;
            }

            List<EffectEntry> effects = loadEffects(tierSection);
            double pvpDamagePenalty = tierSection.getDouble("pvp-damage-penalty", 0);
            double xpBonus = tierSection.getDouble("xp-bonus", 0);
            boolean blockTrading = tierSection.getBoolean("block-trading", false);
            boolean golemAggro = tierSection.getBoolean("golem-aggro", false);
            double golemAggroRange = tierSection.getDouble("golem-aggro-range", 16.0);
            boolean noFirstStrike = tierSection.getBoolean("no-first-strike", false);
            long firstStrikeCooldownMs = tierSection.getLong("first-strike-cooldown-seconds", 5) * 1000L;

            tiers.add(new BuffTier(threshold, effects, pvpDamagePenalty, xpBonus,
                    blockTrading, golemAggro, golemAggroRange, noFirstStrike, firstStrikeCooldownMs));
        }

        if (negative) {
            tiers.sort(Comparator.comparingInt(BuffTier::threshold));
        } else {
            tiers.sort(Comparator.comparingInt(BuffTier::threshold).reversed());
        }
        return tiers;
    }

    @SuppressWarnings("unchecked")
    private List<EffectEntry> loadEffects(ConfigurationSection tierSection) {
        List<EffectEntry> effects = new ArrayList<>();
        List<Map<?, ?>> effectList = tierSection.getMapList("effects");
        for (Map<?, ?> map : effectList) {
            String typeName = String.valueOf(map.get("type"));
            Registry<PotionEffectType> registry =
                    RegistryAccess.registryAccess().getRegistry(RegistryKey.MOB_EFFECT);
            PotionEffectType type = registry.get(NamespacedKey.minecraft(typeName.toLowerCase()));
            if (type == null) {
                LOGGER.warning("Unknown potion effect type: " + typeName);
                continue;
            }
            int amplifier = 0;
            if (map.containsKey("amplifier")) {
                amplifier = ((Number) map.get("amplifier")).intValue();
            }
            boolean ambient = false;
            if (map.containsKey("ambient")) {
                ambient = (Boolean) map.get("ambient");
            }
            effects.add(new EffectEntry(type, amplifier, ambient));
        }
        return effects;
    }

    public BuffTier findNegativeTier(int karma) {
        for (BuffTier tier : negativeTiers) {
            if (karma <= tier.threshold()) {
                return tier;
            }
        }
        return null;
    }

    public BuffTier findPositiveTier(int karma) {
        for (BuffTier tier : positiveTiers) {
            if (karma >= tier.threshold()) {
                return tier;
            }
        }
        return null;
    }

    public int getEffectDuration() {
        return effectDuration;
    }

    public boolean isTabEnabled() {
        return tabEnabled;
    }

    public double getPvpDamagePenalty(int karma) {
        BuffTier tier = findPositiveTier(karma);
        return tier != null ? tier.pvpDamagePenalty() : 0;
    }

    public boolean isNoFirstStrike(int karma) {
        BuffTier tier = findPositiveTier(karma);
        return tier != null && tier.noFirstStrike();
    }

    public long getFirstStrikeCooldownMs(int karma) {
        BuffTier tier = findPositiveTier(karma);
        return tier != null ? tier.firstStrikeCooldownMs() : 5000L;
    }

    public double getXpBonus(int karma) {
        BuffTier tier = findPositiveTier(karma);
        return tier != null ? tier.xpBonus() : 0;
    }

    public boolean isTradeBlocked(int karma) {
        BuffTier tier = findNegativeTier(karma);
        return tier != null && tier.blockTrading();
    }

    public boolean isGolemAggro(int karma) {
        BuffTier tier = findNegativeTier(karma);
        return tier != null && tier.golemAggro();
    }

    public double getGolemAggroRange(int karma) {
        BuffTier tier = findNegativeTier(karma);
        return tier != null ? tier.golemAggroRange() : 16.0;
    }
}
