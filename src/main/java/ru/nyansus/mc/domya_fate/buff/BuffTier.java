package ru.nyansus.mc.domya_fate.buff;

import java.util.List;

public record BuffTier(int threshold, List<EffectEntry> effects,
                       double pvpDamagePenalty, double xpBonus,
                       double mobDamageBonus, double speedBonus,
                       int tradePriceIncrease, boolean blockTrading,
                       boolean golemAggro, double golemAggroRange) {
}
