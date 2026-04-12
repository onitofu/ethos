package ru.nyansus.mc.ethos.title;

import java.util.List;

public record TitleColor(Mode mode, List<String> colors) {

    public enum Mode { SINGLE, AUTO_GRADIENT, ALTERNATE, GRADIENT }

    public static TitleColor single(String color) {
        return new TitleColor(Mode.SINGLE, List.of(color));
    }

    public String primary() {
        return colors.isEmpty() ? "white" : colors.get(0);
    }
}
