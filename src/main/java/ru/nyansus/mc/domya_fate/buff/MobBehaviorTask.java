package ru.nyansus.mc.domya_fate.buff;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Animals;
import org.bukkit.entity.Bee;
import org.bukkit.entity.Chicken;
import org.bukkit.entity.Cow;
import org.bukkit.entity.Donkey;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Horse;
import org.bukkit.entity.Llama;
import org.bukkit.entity.MushroomCow;
import org.bukkit.entity.Pig;
import org.bukkit.entity.Rabbit;
import org.bukkit.entity.Sheep;
import org.bukkit.entity.Enemy;
import org.bukkit.entity.Entity;
import org.bukkit.entity.IronGolem;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import ru.nyansus.mc.domya_fate.DomyaFate;

public class MobBehaviorTask extends BukkitRunnable {

    private final DomyaFate plugin;

    public MobBehaviorTask(DomyaFate plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        BuffConfig config = plugin.getBuffConfig();
        for (var player : plugin.getServer().getOnlinePlayers()) {
            if (player.getGameMode() != GameMode.SURVIVAL) {
                continue;
            }
            int karma = plugin.getKarmaManager().getKarma(player.getUniqueId());

            if (config.hasEffect(karma, EffectType.PASSIVE_MOB_HOSTILE)) {
                hostilePassiveMobs(player);
            } else if (config.hasEffect(karma, EffectType.PASSIVE_MOB_FLEE)) {
                fleePassiveMobs(player);
            }

            if (config.hasEffect(karma, EffectType.GOLEM_AGGRO)) {
                double range = config.getNumericEffect(karma, EffectType.GOLEM_AGGRO);
                aggroGolems(player, range);
            }

            double mobRange = config.getNumericEffect(
                    karma, EffectType.HOSTILE_MOB_INCREASED_RANGE);
            if (mobRange > 0) {
                attractHostileMobs(player, mobRange);
            }
        }
    }

    private void fleePassiveMobs(Player player) {
        Location playerLoc = player.getLocation();
        for (Entity entity : player.getNearbyEntities(12, 12, 12)) {
            if (!(entity instanceof Animals animal)) {
                continue;
            }
            double dist = animal.getLocation().distance(playerLoc);
            if (dist > 10) {
                continue;
            }
            var dir = animal.getLocation().subtract(playerLoc).toVector();
            if (dir.lengthSquared() == 0) {
                continue;
            }
            dir.normalize().multiply(8);
            Location fleeTarget = animal.getLocation().add(dir);
            animal.getPathfinder().moveTo(fleeTarget, 1.5);
        }
    }

    private void hostilePassiveMobs(Player player) {
        Location playerLoc = player.getLocation();
        for (Entity entity : player.getNearbyEntities(12, 12, 12)) {
            if (!(entity instanceof Animals animal)) {
                continue;
            }
            double dist = animal.getLocation().distance(playerLoc);
            if (dist > 10) {
                continue;
            }
            animal.getPathfinder().moveTo(playerLoc, 1.3);
            if (dist < 2.0) {
                player.damage(getAnimalDamage(animal), animal);
            }
        }
    }

    private double getAnimalDamage(Animals animal) {
        var config = plugin.getConfig();
        if (animal instanceof Horse || animal instanceof Donkey
                || animal instanceof Llama || animal instanceof MushroomCow) {
            return config.getDouble("passive-mob-damage.large", 3.0);
        }
        if (animal instanceof Cow || animal instanceof Pig || animal instanceof Sheep) {
            return config.getDouble("passive-mob-damage.medium", 2.0);
        }
        if (animal instanceof Chicken || animal instanceof Rabbit || animal instanceof Bee) {
            return config.getDouble("passive-mob-damage.small", 0.5);
        }
        return config.getDouble("passive-mob-damage.default", 1.0);
    }

    private void aggroGolems(Player player, double range) {
        for (Entity entity : player.getNearbyEntities(range, range, range)) {
            if (entity instanceof IronGolem golem && golem.getTarget() == null) {
                golem.setTarget(player);
            }
        }
    }

    private void attractHostileMobs(Player player, double range) {
        for (Entity entity : player.getNearbyEntities(range, range, range)) {
            if (entity instanceof Enemy mob && ((Mob) mob).getTarget() == null) {
                ((Mob) mob).setTarget(player);
            }
        }
    }
}
