package ru.nyansus.mc.domya_fate.listener;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import ru.nyansus.mc.domya_fate.DomyaFate;
import ru.nyansus.mc.domya_fate.title.Title;
import ru.nyansus.mc.domya_fate.title.TitleManager;

import java.util.Optional;
import java.util.UUID;

public class PlayerJoinListener implements Listener {

    private final DomyaFate plugin;

    public PlayerJoinListener(DomyaFate plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        TitleManager tm = plugin.getTitleManager();

        if (!tm.getUnlockedTitles(uuid).isEmpty()) {
            return;
        }

        int defaultId = plugin.getConfig().getInt("default-title", -1);
        if (defaultId < 0) {
            return;
        }

        Optional<Title> title = tm.getRegistry().getTitle(defaultId);
        if (title.isEmpty()) {
            return;
        }

        tm.unlockTitle(uuid, defaultId);
        tm.setActiveTitle(uuid, defaultId);
    }
}
