package ru.nyansus.mc.domya_fate.karma;

public record KarmaTitle(int id, int minKarma, int maxKarma,
                          String nameRu, String nameEn, String color) {

    public boolean matches(int karma) {
        return karma >= minKarma && karma < maxKarma;
    }
}
