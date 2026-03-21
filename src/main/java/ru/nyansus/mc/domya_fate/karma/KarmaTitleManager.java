package ru.nyansus.mc.domya_fate.karma;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class KarmaTitleManager {

    private final List<KarmaTitle> titles = new ArrayList<>();

    public KarmaTitleManager(FileConfiguration config) {
        loadTitles(config);
    }

    private void loadTitles(FileConfiguration config) {
        titles.clear();
        ConfigurationSection section = config.getConfigurationSection("karma-titles");
        if (section == null) {
            return;
        }
        for (String key : section.getKeys(false)) {
            int id;
            try {
                id = Integer.parseInt(key);
            } catch (NumberFormatException e) {
                continue;
            }
            List<Integer> range = section.getIntegerList(key);
            if (range.size() != 2) {
                continue;
            }
            titles.add(new KarmaTitle(id, range.get(0), range.get(1)));
        }
    }

    public Optional<KarmaTitle> getTitle(int karma) {
        for (KarmaTitle title : titles) {
            if (title.matches(karma)) {
                return Optional.of(title);
            }
        }
        return Optional.empty();
    }

    public List<KarmaTitle> getAllTitles() {
        return List.copyOf(titles);
    }
}
