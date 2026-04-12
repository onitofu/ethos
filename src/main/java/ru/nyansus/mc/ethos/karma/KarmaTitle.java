package ru.nyansus.mc.ethos.karma;

public record KarmaTitle(int id, int minKarma, int maxKarma) {

    public boolean matches(int karma) {
        return karma >= minKarma && karma < maxKarma;
    }
}
