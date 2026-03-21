package ru.nyansus.mc.domya_fate.karma;

import java.util.UUID;

public interface KarmaStorage {

    int getKarma(UUID uuid);

    long getLastUpdate(UUID uuid);

    void setKarma(UUID uuid, int karma, long timestamp);

    void load();

    void save();
}
