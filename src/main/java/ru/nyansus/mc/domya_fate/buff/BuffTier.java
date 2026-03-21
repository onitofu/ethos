package ru.nyansus.mc.domya_fate.buff;

import java.util.List;

public record BuffTier(int threshold, List<EffectEntry> effects,
                       double pvpDamagePenalty, double xpBonus,
                       boolean blockTrading, boolean golemAggro,
                       double golemAggroRange, boolean noFirstStrike,
                       long firstStrikeCooldownMs) {
}
