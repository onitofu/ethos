package ru.nyansus.mc.domya_fate.title;

import org.bukkit.Material;
import org.bukkit.Statistic;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import ru.nyansus.mc.domya_fate.karma.StatsStorage;

import java.util.List;

public class UnlockCondition {

    private final Type type;
    private final int value;
    private final List<EntityType> entities;
    private final List<Material> materials;

    public UnlockCondition(Type type, int value, List<EntityType> entities, List<Material> materials) {
        this.type = type;
        this.value = value;
        this.entities = entities;
        this.materials = materials;
    }

    public boolean isMet(Player player, StatsStorage stats) {
        return getProgress(player, stats) >= value;
    }

    public int getProgress(Player player, StatsStorage stats) {
        return switch (type) {
            case KILL_ENTITY -> sumEntityStats(player, Statistic.KILL_ENTITY);
            case MINE_BLOCK -> sumMaterialStats(player, Statistic.MINE_BLOCK);
            case PLAYER_KILLS -> stats.getStat(player.getUniqueId(), "pvp-kills");
            case DEATHS -> player.getStatistic(Statistic.DEATHS);
            case FISH_CAUGHT -> player.getStatistic(Statistic.FISH_CAUGHT);
            case TRADED_WITH_VILLAGER -> player.getStatistic(Statistic.TRADED_WITH_VILLAGER);
            case ANIMALS_BRED -> player.getStatistic(Statistic.ANIMALS_BRED);
            case ENCHANT_ITEM -> player.getStatistic(Statistic.ITEM_ENCHANTED);
            case DISTANCE_KM -> getTotalDistanceCm(player) / 100_000;
        };
    }

    private int sumEntityStats(Player player, Statistic stat) {
        int sum = 0;
        for (EntityType entity : entities) {
            sum += player.getStatistic(stat, entity);
        }
        return sum;
    }

    private int sumMaterialStats(Player player, Statistic stat) {
        int sum = 0;
        for (Material material : materials) {
            sum += player.getStatistic(stat, material);
        }
        return sum;
    }

    private int getTotalDistanceCm(Player player) {
        int total = 0;
        for (Statistic stat : List.of(
                Statistic.WALK_ONE_CM,
                Statistic.SPRINT_ONE_CM,
                Statistic.CROUCH_ONE_CM,
                Statistic.SWIM_ONE_CM,
                Statistic.BOAT_ONE_CM,
                Statistic.HORSE_ONE_CM,
                Statistic.PIG_ONE_CM,
                Statistic.STRIDER_ONE_CM,
                Statistic.MINECART_ONE_CM)) {
            total += player.getStatistic(stat);
        }
        return total;
    }

    public enum Type {
        KILL_ENTITY,
        MINE_BLOCK,
        PLAYER_KILLS,
        DEATHS,
        FISH_CAUGHT,
        TRADED_WITH_VILLAGER,
        ANIMALS_BRED,
        ENCHANT_ITEM,
        DISTANCE_KM
    }
}
