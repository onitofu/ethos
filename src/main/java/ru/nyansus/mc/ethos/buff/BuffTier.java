package ru.nyansus.mc.ethos.buff;

import java.util.List;

public record BuffTier(int threshold, List<BuffEffect> effects) {

    public double getNumeric(EffectType type) {
        for (BuffEffect effect : effects) {
            if (effect.effectType() == type) {
                return effect.value();
            }
        }
        return 0;
    }

    public boolean has(EffectType type) {
        for (BuffEffect effect : effects) {
            if (effect.effectType() == type && effect.isEnabled()) {
                return true;
            }
        }
        return false;
    }
}
