package ru.nyansus.mc.ethos.buff;

public enum EffectType {
    // Combat
    MOB_DAMAGE_BONUS,
    PASSIVE_MOB_DAMAGE_BONUS,
    PVP_DAMAGE_BONUS,
    PVP_DAMAGE_PENALTY,
    RESISTANCE,
    FALL_DAMAGE_REDUCTION,
    FIRE_RESISTANCE,

    // Stats
    HEALTH_BONUS,
    SPEED_BONUS,
    REGENERATION_BONUS,
    HUNGER_RATE,
    NIGHT_VISION,

    // XP
    XP_BONUS,
    XP_PENALTY,
    XP_DEATH_PENALTY,

    // Loot
    LOOT_BONUS,
    MINING_SPEED,
    DOUBLE_CROP_CHANCE,

    // Trading
    TRADE_PRICE_INCREASE,
    TRADE_PRICE_DECREASE,
    BLOCK_TRADING,

    // Mob interaction
    HOSTILE_MOB_NEUTRAL,
    HOSTILE_MOB_REDUCED_RANGE,
    HOSTILE_MOB_INCREASED_RANGE,
    PASSIVE_MOB_FLEE,
    PASSIVE_MOB_HOSTILE,
    GOLEM_AGGRO,

    // Animal interaction
    BLOCK_TAMING,
    BLOCK_RIDING,

    // Death
    KEEP_INVENTORY_CHANCE,

    // Visibility
    GLOWING,

    // Vanilla potion effect
    POTION_EFFECT
}
