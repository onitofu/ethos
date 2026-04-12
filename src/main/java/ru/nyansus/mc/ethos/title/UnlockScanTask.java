package ru.nyansus.mc.ethos.title;

import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import ru.nyansus.mc.ethos.Ethos;

import ru.nyansus.mc.ethos.karma.StatsStorage;

import java.util.Set;
import java.util.UUID;

public class UnlockScanTask extends BukkitRunnable {

    private final Ethos plugin;

    public UnlockScanTask(Ethos plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        TitleManager tm = plugin.getTitleManager();
        StatsStorage stats = plugin.getStatsStorage();
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            UUID uuid = player.getUniqueId();
            Set<Integer> unlocked = tm.getUnlockedTitles(uuid);

            for (var entry : tm.getRegistry().getAllTitles().entrySet()) {
                Title title = entry.getValue();
                if (title.unlockCondition() == null) {
                    continue;
                }
                if (unlocked.contains(title.id())) {
                    continue;
                }
                if (title.unlockCondition().isMet(player, stats)) {
                    boolean isNew = tm.unlockTitle(uuid, title.id());
                    if (isNew) {
                        plugin.broadcastTitleUnlock(player, title);
                    }
                }
            }
        }
    }
}
