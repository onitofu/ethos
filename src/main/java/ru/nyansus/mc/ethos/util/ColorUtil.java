package ru.nyansus.mc.ethos.util;

import ru.nyansus.mc.ethos.title.TitleColor;

public final class ColorUtil {

    private ColorUtil() {
    }

    public static String colorCode(TitleColor color) {
        return color == null ? "§f" : colorCode(color.primary());
    }

    public static String colorCode(String color) {
        return switch (color) {
            case "dark_red" -> "§4";
            case "red" -> "§c";
            case "gold" -> "§6";
            case "green" -> "§a";
            case "dark_green" -> "§2";
            case "aqua" -> "§b";
            case "dark_aqua" -> "§3";
            case "light_purple" -> "§d";
            case "dark_purple" -> "§5";
            case "white" -> "§f";
            case "gray" -> "§7";
            case "dark_gray" -> "§8";
            case "yellow" -> "§e";
            case "blue" -> "§9";
            default -> "§f";
        };
    }
}
