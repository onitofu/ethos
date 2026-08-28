package ru.nyansus.mc.ethos.listener;

import org.bukkit.entity.EntityType;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class MobKillListenerTest {

    @Test
    public void bossesAreExemptFromAntiFarm() {
        assertTrue(MobKillListener.isAntiFarmExempt(EntityType.ENDER_DRAGON));
        assertTrue(MobKillListener.isAntiFarmExempt(EntityType.WITHER));
    }

    @Test
    public void regularMobsAreNotExemptFromAntiFarm() {
        assertFalse(MobKillListener.isAntiFarmExempt(EntityType.ZOMBIE));
    }
}
