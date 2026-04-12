package ru.nyansus.mc.ethos.listener;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import ru.nyansus.mc.ethos.Ethos;
import ru.nyansus.mc.ethos.title.Title;
import ru.nyansus.mc.ethos.title.TitleManager;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;
import java.util.UUID;

public class PlayerJoinListener implements Listener {

    private final Ethos plugin;

    public PlayerJoinListener(Ethos plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        TitleManager tm = plugin.getTitleManager();

        if (tm.getUnlockedTitles(uuid).isEmpty()) {
            grantDefaultTitle(uuid, tm);
        }

        checkSeasonalTitles(player, tm);
    }

    private void grantDefaultTitle(UUID uuid, TitleManager tm) {
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

    private void checkSeasonalTitles(Player player, TitleManager tm) {
        ConfigurationSection section = plugin.getConfig()
                .getConfigurationSection("seasonal-titles");
        if (section == null) {
            return;
        }

        for (String key : section.getKeys(false)) {
            ConfigurationSection entry = section.getConfigurationSection(key);
            if (entry == null) {
                continue;
            }

            int titleId = entry.getInt("title-id", -1);
            if (titleId < 0) {
                continue;
            }
            if (tm.getUnlockedTitles(player.getUniqueId()).contains(titleId)) {
                continue;
            }

            String fromStr = entry.getString("from", "");
            String toStr = entry.getString("to", "");
            String tz = entry.getString("timezone", "UTC");
            if (fromStr.isEmpty() || toStr.isEmpty()) {
                continue;
            }

            LocalDateTime from = parseDateTime(fromStr);
            LocalDateTime to = parseDateTime(toStr);
            if (from == null || to == null) {
                continue;
            }

            LocalDateTime now = LocalDateTime.now(ZoneId.of(tz));
            if (!now.isBefore(from) && !now.isAfter(to)) {
                boolean isNew = tm.unlockTitle(player.getUniqueId(), titleId);
                if (isNew) {
                    Optional<Title> title = tm.getRegistry().getTitle(titleId);
                    title.ifPresent(t -> Bukkit.getScheduler().runTaskLater(plugin,
                            () -> plugin.broadcastTitleUnlock(player, t), 20L));
                }
            }
        }
    }

    private LocalDateTime parseDateTime(String str) {
        String[] parts = str.split("-");
        if (parts.length < 3) {
            return null;
        }
        try {
            int year = Integer.parseInt(parts[0]);
            int month = Integer.parseInt(parts[1]);
            int day = Integer.parseInt(parts[2]);
            int hour = parts.length > 3 ? Integer.parseInt(parts[3]) : 0;
            int minute = parts.length > 4 ? Integer.parseInt(parts[4]) : 0;
            return LocalDateTime.of(year, month, day, hour, minute);
        } catch (Exception e) {
            return null;
        }
    }
}
