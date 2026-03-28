package ru.nyansus.mc.domya_fate.integration;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import ru.nyansus.mc.domya_fate.DomyaFate;
import ru.nyansus.mc.domya_fate.title.Title;

public class DomyaFatePlaceholders extends PlaceholderExpansion {

    private static final float GRADIENT_HUE_SHIFT = 25f;

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
        var uuid = player.getUniqueId();
        return switch (params) {
            case "karma" -> String.valueOf(plugin.getKarmaManager().getKarma(uuid));
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
        return plugin.getTitleManager().getActiveTitle(player.getUniqueId())
                .map(title -> {
                    String name = resolveName(title, player);
                    String[] gradient = buildGradient(title.color());
                    return "<dark_gray>[<gradient:" + gradient[0] + ":" + gradient[1] + ">"
                            + name + "</gradient><dark_gray>]";
                })
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

    private static String[] buildGradient(String colorName) {
        String hex = resolveHex(colorName);
        float[] hsl = hexToHsl(hex);
        float hue2 = (hsl[0] + GRADIENT_HUE_SHIFT) % 360;
        return new String[]{hex, hslToHex(hue2, hsl[1], hsl[2])};
    }

    private static String resolveHex(String input) {
        if (input.startsWith("#")) {
            return input;
        }
        NamedTextColor color = NamedTextColor.NAMES.value(input.toLowerCase());
        return color != null ? String.format("#%06X", color.value()) : "#FFFFFF";
    }

    private static float[] hexToHsl(String hex) {
        int rgb = Integer.parseInt(hex.substring(1), 16);
        float r = ((rgb >> 16) & 0xFF) / 255f;
        float g = ((rgb >> 8) & 0xFF) / 255f;
        float b = (rgb & 0xFF) / 255f;
        float max = Math.max(r, Math.max(g, b));
        float min = Math.min(r, Math.min(g, b));
        float l = (max + min) / 2f;
        if (max == min) {
            return new float[]{0, 0, l};
        }
        float d = max - min;
        float s = d / (1 - Math.abs(2 * l - 1));
        float h;
        if (max == r) {
            h = ((g - b) / d) % 6;
        } else if (max == g) {
            h = (b - r) / d + 2;
        } else {
            h = (r - g) / d + 4;
        }
        h *= 60;
        if (h < 0) {
            h += 360;
        }
        return new float[]{h, s, l};
    }

    private static String hslToHex(float h, float s, float l) {
        float hn = h / 360f;
        float r;
        float g;
        float b;
        if (s == 0) {
            r = l;
            g = l;
            b = l;
        } else {
            float q = l < 0.5f ? l * (1 + s) : l + s - l * s;
            float p = 2 * l - q;
            r = hueToRgb(p, q, hn + 1f / 3);
            g = hueToRgb(p, q, hn);
            b = hueToRgb(p, q, hn - 1f / 3);
        }
        return String.format("#%02X%02X%02X",
                (int) (r * 255), (int) (g * 255), (int) (b * 255));
    }

    private static float hueToRgb(float p, float q, float t) {
        float tv = t;
        if (tv < 0) tv += 1;
        if (tv > 1) tv -= 1;
        if (tv < 1f / 6) return p + (q - p) * 6 * tv;
        if (tv < 1f / 2) return q;
        if (tv < 2f / 3) return p + (q - p) * (2f / 3 - tv) * 6;
        return p;
    }
}
