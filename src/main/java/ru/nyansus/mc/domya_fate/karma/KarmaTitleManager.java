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
        int id = 1;
        for (String key : section.getKeys(false)) {
            ConfigurationSection entry = section.getConfigurationSection(key);
            if (entry == null) {
                continue;
            }
            List<Integer> range = entry.getIntegerList("range");
            if (range.size() != 2) {
                continue;
            }
            ConfigurationSection nameSection = entry.getConfigurationSection("name");
            String nameRu = nameSection != null ? nameSection.getString("ru", key) : key;
            String nameEn = nameSection != null ? nameSection.getString("en", key) : key;
            String color = entry.getString("color", "white");

            titles.add(new KarmaTitle(id++, range.get(0), range.get(1), nameRu, nameEn, color));
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
