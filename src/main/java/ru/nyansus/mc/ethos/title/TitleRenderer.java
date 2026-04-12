package ru.nyansus.mc.ethos.title;

import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;

import java.util.ArrayList;
import java.util.List;

public final class TitleRenderer {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    private TitleRenderer() {
    }

    public static String render(String text, TitleColor color, float autoGradientShift) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        return switch (color.mode()) {
            case SINGLE -> renderFlat(text, color.primary());
            case AUTO_GRADIENT -> renderAutoGradient(text, color.primary(), autoGradientShift);
            case ALTERNATE -> renderAlternate(text, color.colors());
            case GRADIENT -> renderGradient(text, color.colors());
        };
    }

    private static String renderFlat(String text, String colorName) {
        return "<color:" + toHex(colorName) + ">" + escape(text) + "</color>";
    }

    private static String renderAutoGradient(String text, String colorName, float shift) {
        String hex = toHex(colorName);
        if (shift <= 0f) {
            return renderFlat(text, colorName);
        }
        float[] hsl = hexToHsl(hex);
        float hue2 = (hsl[0] + shift) % 360f;
        String hex2 = hslToHex(hue2, hsl[1], hsl[2]);
        return "<gradient:" + hex + ":" + hex2 + ">" + escape(text) + "</gradient>";
    }

    private static String renderAlternate(String text, List<String> colors) {
        List<String> hexes = new ArrayList<>(colors.size());
        for (String c : colors) {
            hexes.add(toHex(c));
        }
        StringBuilder sb = new StringBuilder();
        int i = 0;
        for (int cp : (Iterable<Integer>) text.codePoints()::iterator) {
            String ch = new String(Character.toChars(cp));
            if (Character.isWhitespace(cp)) {
                sb.append(ch);
                continue;
            }
            sb.append("<color:").append(hexes.get(i % hexes.size())).append(">")
                    .append(escape(ch)).append("</color>");
            i++;
        }
        return sb.toString();
    }

    private static String renderGradient(String text, List<String> colors) {
        StringBuilder sb = new StringBuilder("<gradient");
        for (String c : colors) {
            sb.append(":").append(toHex(c));
        }
        sb.append(">").append(escape(text)).append("</gradient>");
        return sb.toString();
    }

    private static String escape(String s) {
        return MM.escapeTags(s);
    }

    public static String toHex(String input) {
        if (input == null || input.isEmpty()) {
            return "#FFFFFF";
        }
        if (input.startsWith("#")) {
            return input.toUpperCase();
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
