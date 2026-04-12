package ru.nyansus.mc.ethos.listener;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import ru.nyansus.mc.ethos.Ethos;
import ru.nyansus.mc.ethos.karma.AntiFarmManager;
import ru.nyansus.mc.ethos.karma.KarmaManager;

public class PlayerKillListener implements Listener {

    private final Ethos plugin;

    public PlayerKillListener(Ethos plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        Player killer = victim.getKiller();
        if (killer == null || killer.equals(victim)) {
            return;
        }

        KarmaManager km = plugin.getKarmaManager();
        AntiFarmManager afm = plugin.getAntiFarmManager();

        if (afm.isPvpOnCooldown(killer.getUniqueId(), victim.getUniqueId())) {
            return;
        }
        if (afm.isMutualKill(killer.getUniqueId(), victim.getUniqueId())) {
            return;
        }

        afm.recordPvpKill(killer.getUniqueId(), victim.getUniqueId());
        plugin.getStatsStorage().incrementStat(killer.getUniqueId(),
                ru.nyansus.mc.ethos.util.StatKeys.PVP_KILLS);

        int victimKarma = km.getKarma(victim.getUniqueId());
        int karmaChange;
        if (victimKarma > 0) {
            karmaChange = plugin.getConfig().getInt("karma-actions.kill-good-player", -30);
        } else if (victimKarma < 0) {
            karmaChange = plugin.getConfig().getInt("karma-actions.kill-evil-player", 10);
        } else {
            karmaChange = plugin.getConfig().getInt("karma-actions.kill-player", -15);
        }

        km.addKarma(killer.getUniqueId(), karmaChange);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        plugin.getAntiFarmManager().clearPlayer(event.getPlayer().getUniqueId());
    }
}
