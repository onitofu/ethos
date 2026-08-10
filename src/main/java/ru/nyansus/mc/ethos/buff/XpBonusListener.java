package ru.nyansus.mc.ethos.buff;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerExpChangeEvent;
import ru.nyansus.mc.ethos.Ethos;

public class XpBonusListener implements Listener {

    private final Ethos plugin;

    public XpBonusListener(Ethos plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onExpChange(PlayerExpChangeEvent event) {
        if (!plugin.areKarmaEffectsEnabled(event.getPlayer())) {
            return;
        }
        int karma = plugin.getKarmaManager().getKarma(event.getPlayer().getUniqueId());
        BuffConfig config = plugin.getBuffConfig();

        double bonus = config.getNumericEffect(karma, EffectType.XP_BONUS);
        double penalty = config.getNumericEffect(karma, EffectType.XP_PENALTY);
        double multiplier = 1.0 + bonus - penalty;

        if (multiplier != 1.0) {
            event.setAmount((int) (event.getAmount() * multiplier));
        }
    }
}
