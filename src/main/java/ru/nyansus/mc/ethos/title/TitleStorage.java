package ru.nyansus.mc.ethos.title;

import java.util.Set;
import java.util.UUID;

public interface TitleStorage {

    int getActiveTitle(UUID uuid);

    void setActiveTitle(UUID uuid, int titleId);

    Set<Integer> getUnlockedTitles(UUID uuid);

    void unlockTitle(UUID uuid, int titleId);

    void revokeTitle(UUID uuid, int titleId);

    void load();

    void save();
}
