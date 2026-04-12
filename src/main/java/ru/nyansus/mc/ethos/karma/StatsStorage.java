package ru.nyansus.mc.ethos.karma;

import java.util.UUID;

public interface StatsStorage {

    int getStat(UUID uuid, String key);

    void setStat(UUID uuid, String key, long value);

    long getLongStat(UUID uuid, String key);

    void incrementStat(UUID uuid, String key);

    void load();

    void save();
}
