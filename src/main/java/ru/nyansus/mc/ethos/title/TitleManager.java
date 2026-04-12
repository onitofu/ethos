package ru.nyansus.mc.ethos.title;

import ru.nyansus.mc.ethos.karma.KarmaTitle;
import ru.nyansus.mc.ethos.karma.KarmaTitleManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public class TitleManager {

    private final TitleRegistry registry;
    private final TitleStorage storage;

    public TitleManager(TitleRegistry registry, TitleStorage storage) {
        this.registry = registry;
        this.storage = storage;
    }

    public Optional<Title> getActiveTitle(UUID uuid) {
        int activeId = storage.getActiveTitle(uuid);
        if (activeId < 0) {
            return Optional.empty();
        }
        return registry.getTitle(activeId);
    }

    public boolean setActiveTitle(UUID uuid, int titleId) {
        if (titleId < 0) {
            storage.setActiveTitle(uuid, -1);
            return true;
        }
        if (!storage.getUnlockedTitles(uuid).contains(titleId)) {
            return false;
        }
        if (registry.getTitle(titleId).isEmpty()) {
            return false;
        }
        storage.setActiveTitle(uuid, titleId);
        return true;
    }

    public void resetActiveTitle(UUID uuid) {
        storage.setActiveTitle(uuid, -1);
    }

    public Set<Integer> getUnlockedTitles(UUID uuid) {
        return storage.getUnlockedTitles(uuid);
    }

    public boolean unlockTitle(UUID uuid, int titleId) {
        if (storage.getUnlockedTitles(uuid).contains(titleId)) {
            return false;
        }
        storage.unlockTitle(uuid, titleId);
        return true;
    }

    public void revokeTitle(UUID uuid, int titleId) {
        storage.revokeTitle(uuid, titleId);
        if (storage.getActiveTitle(uuid) == titleId) {
            storage.setActiveTitle(uuid, -1);
        }
    }

    /**
     * Syncs karmic titles based on current karma.
     *
     * Positive titles unlock when karma reaches their minKarma and stay
     * unlocked while karma remains positive. They are revoked when karma
     * drops to zero or below.
     *
     * Negative titles unlock when karma reaches their maxKarma and stay
     * unlocked while karma remains negative. They are revoked when karma
     * rises to zero or above.
     *
     * Returns list of newly unlocked titles (for broadcast).
     */
    public List<Title> syncKarmaTitles(UUID uuid, int karma, KarmaTitleManager ktm) {
        List<Title> newlyUnlocked = new ArrayList<>();
        Set<Integer> unlocked = storage.getUnlockedTitles(uuid);

        for (KarmaTitle kt : ktm.getAllTitles()) {
            int id = kt.id();
            boolean has = unlocked.contains(id);
            boolean isPositiveTitle = kt.minKarma() >= 0;

            if (isPositiveTitle) {
                boolean shouldUnlock = karma >= kt.minKarma();
                boolean shouldRevoke = karma <= 0;

                if (shouldUnlock && !has) {
                    storage.unlockTitle(uuid, id);
                    registry.getTitle(id).ifPresent(newlyUnlocked::add);
                } else if (shouldRevoke && has) {
                    revokeTitle(uuid, id);
                }
            } else {
                boolean shouldUnlock = karma <= kt.maxKarma();
                boolean shouldRevoke = karma >= 0;

                if (shouldUnlock && !has) {
                    storage.unlockTitle(uuid, id);
                    registry.getTitle(id).ifPresent(newlyUnlocked::add);
                } else if (shouldRevoke && has) {
                    revokeTitle(uuid, id);
                }
            }
        }

        return newlyUnlocked;
    }

    public TitleStorage getStorage() {
        return storage;
    }

    public TitleRegistry getRegistry() {
        return registry;
    }

    public void saveAll() {
        storage.save();
    }
}
