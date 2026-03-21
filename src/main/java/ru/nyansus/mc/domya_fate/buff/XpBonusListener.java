package ru.nyansus.mc.domya_fate.buff;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerExpChangeEvent;
import ru.nyansus.mc.domya_fate.DomyaFate;

public class XpBonusListener implements Listener {

    private final DomyaFate plugin;

    public XpBonusListener(DomyaFate plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onExpChange(PlayerExpChangeEvent event) {
        int karma = plugin.getKarmaManager().getKarma(event.getPlayer().getUniqueId());
        double bonus = plugin.getBuffConfig().getXpBonus(karma);
        if (bonus > 0) {
            int extra = (int) (event.getAmount() * bonus);
            event.setAmount(event.getAmount() + extra);
        }
    }
}
