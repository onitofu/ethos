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
        this.negativeTiers = new ArrayList<>();
        this.positiveTiers = new ArrayList<>();
        loadTiers(config);
    }

    private void loadTiers(FileConfiguration config) {
        ConfigurationSection section = config.getConfigurationSection("buffs.tiers");
        if (section == null) {
            loadLegacyTiers(config);
            return;
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

            List<BuffEffect> effects = parseEffects(tierSection);
            BuffTier tier = new BuffTier(threshold, effects);

            if (threshold < 0) {
                negativeTiers.add(tier);
            } else {
                positiveTiers.add(tier);
            }
        }

        negativeTiers.sort(Comparator.comparingInt(BuffTier::threshold));
        positiveTiers.sort(Comparator.comparingInt(BuffTier::threshold).reversed());
    }

    private List<BuffEffect> parseEffects(ConfigurationSection tierSection) {
        List<BuffEffect> effects = new ArrayList<>();
        List<Map<?, ?>> effectList = tierSection.getMapList("effects");
        for (Map<?, ?> map : effectList) {
            BuffEffect effect = parseOneEffect(map);
            if (effect != null) {
                effects.add(effect);
            }
        }
        return effects;
    }

    private BuffEffect parseOneEffect(Map<?, ?> map) {
        String typeName = String.valueOf(map.get("type"));

        if ("POTION_EFFECT".equals(typeName)) {
            return parsePotionEffect(map);
        }

        EffectType type;
        try {
            type = EffectType.valueOf(typeName);
        } catch (IllegalArgumentException e) {
            LOGGER.warning("Unknown effect type: " + typeName);
            return null;
        }

        Object rawValue = map.get("value");
        double value = 0;
        if (rawValue instanceof Number num) {
            value = num.doubleValue();
        } else if (rawValue instanceof Boolean bool) {
            value = bool ? 1.0 : 0;
        }

        return BuffEffect.numeric(type, value);
    }

    private BuffEffect parsePotionEffect(Map<?, ?> map) {
        String potionName = String.valueOf(map.get("potion"));
        Registry<PotionEffectType> registry =
                RegistryAccess.registryAccess().getRegistry(RegistryKey.MOB_EFFECT);
        PotionEffectType potionType = registry.get(
                NamespacedKey.minecraft(potionName.toLowerCase()));
        if (potionType == null) {
            LOGGER.warning("Unknown potion type: " + potionName);
            return null;
        }
        int amplifier = 0;
        if (map.containsKey("amplifier")) {
            amplifier = ((Number) map.get("amplifier")).intValue();
        }
        boolean ambient = false;
        if (map.containsKey("ambient")) {
            ambient = (Boolean) map.get("ambient");
        }
        return BuffEffect.potion(potionType, amplifier, ambient);
    }

    @SuppressWarnings("deprecation")
    private void loadLegacyTiers(FileConfiguration config) {
        loadLegacySide(config, "buffs.negative", negativeTiers, true);
        loadLegacySide(config, "buffs.positive", positiveTiers, false);
    }

    private void loadLegacySide(FileConfiguration config, String path,
                                List<BuffTier> tiers, boolean negative) {
        ConfigurationSection section = config.getConfigurationSection(path);
        if (section == null) {
            return;
        }
        for (String key : section.getKeys(false)) {
            int threshold;
            try {
                threshold = Integer.parseInt(key);
            } catch (NumberFormatException e) {
                continue;
            }
            ConfigurationSection ts = section.getConfigurationSection(key);
            if (ts == null) {
                continue;
            }
            List<BuffEffect> effects = new ArrayList<>();

            addIfPresent(effects, ts, "mob-damage-bonus", EffectType.MOB_DAMAGE_BONUS);
            addIfPresent(effects, ts, "pvp-damage-penalty", EffectType.PVP_DAMAGE_PENALTY);
            addIfPresent(effects, ts, "xp-bonus", EffectType.XP_BONUS);
            addIfPresent(effects, ts, "speed-bonus", EffectType.SPEED_BONUS);
            addIntIfPresent(effects, ts, "trade-price-increase", EffectType.TRADE_PRICE_INCREASE);
            if (ts.getBoolean("block-trading", false)) {
                effects.add(BuffEffect.bool(EffectType.BLOCK_TRADING));
            }
            if (ts.getBoolean("golem-aggro", false)) {
                effects.add(BuffEffect.numeric(EffectType.GOLEM_AGGRO,
                        ts.getDouble("golem-aggro-range", 16.0)));
            }

            List<Map<?, ?>> potionList = ts.getMapList("effects");
            for (Map<?, ?> map : potionList) {
                BuffEffect pe = parsePotionEffect(map);
                if (pe != null) {
                    effects.add(pe);
                }
            }

            tiers.add(new BuffTier(threshold, effects));
        }
        if (negative) {
            tiers.sort(Comparator.comparingInt(BuffTier::threshold));
        } else {
            tiers.sort(Comparator.comparingInt(BuffTier::threshold).reversed());
        }
    }

    private void addIfPresent(List<BuffEffect> effects, ConfigurationSection ts,
                              String key, EffectType type) {
        if (ts.contains(key)) {
            effects.add(BuffEffect.numeric(type, ts.getDouble(key)));
        }
    }

    private void addIntIfPresent(List<BuffEffect> effects, ConfigurationSection ts,
                                 String key, EffectType type) {
        if (ts.contains(key)) {
            effects.add(BuffEffect.numeric(type, ts.getInt(key)));
        }
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

    public BuffTier findTier(int karma) {
        if (karma < 0) {
            return findNegativeTier(karma);
        }
        return findPositiveTier(karma);
    }

    public double getNumericEffect(int karma, EffectType type) {
        BuffTier tier = findTier(karma);
        return tier != null ? tier.getNumeric(type) : 0;
    }

    public boolean hasEffect(int karma, EffectType type) {
        BuffTier tier = findTier(karma);
        return tier != null && tier.has(type);
    }

    public int getEffectDuration() {
        return effectDuration;
    }

    public boolean isTabEnabled() {
        return tabEnabled;
    }
}
