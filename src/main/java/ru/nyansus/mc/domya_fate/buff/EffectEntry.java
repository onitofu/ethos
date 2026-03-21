package ru.nyansus.mc.domya_fate.buff;

import org.bukkit.potion.PotionEffectType;

public record EffectEntry(PotionEffectType type, int amplifier, boolean ambient) {
}
