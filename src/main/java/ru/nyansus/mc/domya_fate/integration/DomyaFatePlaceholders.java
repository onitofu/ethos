package ru.nyansus.mc.domya_fate.integration;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import ru.nyansus.mc.domya_fate.DomyaFate;
import ru.nyansus.mc.domya_fate.title.Title;
import ru.nyansus.mc.domya_fate.title.TitleRenderer;

public class DomyaFatePlaceholders extends PlaceholderExpansion {

    private final DomyaFate plugin;

    public DomyaFatePlaceholders(DomyaFate plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "ethos";
    }

    @Override
    public @NotNull String getAuthor() {
        return "nyansus";
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getPluginMeta().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        if (player == null) {
            return "";
        }
        return switch (params) {
            case "karma" -> String.valueOf(plugin.getKarmaManager().getKarma(player.getUniqueId()));
            case "title" -> getActiveTitleName(player);
            case "title_colored" -> getActiveTitleColored(player);
            case "title_id" -> getActiveTitleId(player);
            case "karma_title" -> getKarmaTitleName(player);
            default -> null;
        };
    }

    private String getActiveTitleName(OfflinePlayer player) {
        return plugin.getTitleManager().getActiveTitle(player.getUniqueId())
                .map(title -> resolveName(title, player))
                .orElse("");
    }

    private String getActiveTitleColored(OfflinePlayer player) {
        float shift = plugin.getTitleGradientShift();
        return plugin.getTitleManager().getActiveTitle(player.getUniqueId())
                .map(title -> TitleRenderer.render(resolveName(title, player), title.color(), shift))
                .orElse("");
    }

    private String getActiveTitleId(OfflinePlayer player) {
        return plugin.getTitleManager().getActiveTitle(player.getUniqueId())
                .map(title -> String.valueOf(title.id()))
                .orElse("");
    }

    private String getKarmaTitleName(OfflinePlayer player) {
        int karma = plugin.getKarmaManager().getKarma(player.getUniqueId());
        return plugin.getKarmaTitleManager().getTitle(karma)
                .flatMap(kt -> plugin.getTitleManager().getRegistry().getTitle(kt.id()))
                .map(t -> resolveName(t, player))
                .orElse("");
    }

    private String resolveName(Title title, OfflinePlayer player) {
        if (player.isOnline()) {
            String lang = player.getPlayer().locale().getLanguage();
            if ("ru".equals(lang)) {
                return title.nameRu();
            }
        }
        return title.nameEn();
    }
}
