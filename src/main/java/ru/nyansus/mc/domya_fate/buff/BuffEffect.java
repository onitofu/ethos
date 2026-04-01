package ru.nyansus.mc.domya_fate.buff;

import org.bukkit.potion.PotionEffectType;

public record BuffEffect(EffectType effectType, double value,
                         PotionEffectType potionType, int amplifier,
                         boolean ambient) {

    public static BuffEffect numeric(EffectType type, double value) {
        return new BuffEffect(type, value, null, 0, false);
    }

    public static BuffEffect bool(EffectType type) {
        return new BuffEffect(type, 1.0, null, 0, false);
    }

    public static BuffEffect potion(PotionEffectType potionType, int amplifier, boolean ambient) {
        return new BuffEffect(EffectType.POTION_EFFECT, 0, potionType, amplifier, ambient);
    }

    public boolean isEnabled() {
        return value > 0;
    }
}
