package ru.nyansus.mc.ethos.buff;

import org.bukkit.entity.EntityType;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class MobBehaviorTaskTest {

    @Test
    public void endermenAndZombifiedPiglinsAreExemptFromForcedAggro() {
        assertTrue(MobBehaviorTask.isForcedAggroExempt(EntityType.ENDERMAN));
        assertTrue(MobBehaviorTask.isForcedAggroExempt(EntityType.ZOMBIFIED_PIGLIN));
    }

    @Test
    public void regularHostileMobsAreNotExemptFromForcedAggro() {
        assertFalse(MobBehaviorTask.isForcedAggroExempt(EntityType.ZOMBIE));
    }
}
