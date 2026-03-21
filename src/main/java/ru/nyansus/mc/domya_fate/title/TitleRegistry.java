package ru.nyansus.mc.domya_fate.title;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.plugin.java.JavaPlugin;

public class TitleRegistry {

    private static final Logger LOGGER = Logger.getLogger(TitleRegistry.class.getName());

    private final Map<Integer, Title> titles = new HashMap<>();

    public TitleRegistry(JavaPlugin plugin) {
        load(plugin);
    }

    private void load(JavaPlugin plugin) {
        titles.clear();

        File file = new File(plugin.getDataFolder(), "titles.yml");
        if (!file.exists()) {
            plugin.saveResource("titles.yml", false);
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);

        InputStream defaultStream = plugin.getResource("titles.yml");
        if (defaultStream != null) {
            try (var reader = new InputStreamReader(defaultStream, StandardCharsets.UTF_8)) {
                YamlConfiguration defaults = YamlConfiguration.loadConfiguration(reader);
                config.setDefaults(defaults);
            } catch (IOException e) {
                LOGGER.log(Level.WARNING, "Failed to load default titles.yml", e);
            }
        }

        ConfigurationSection section = config.getConfigurationSection("titles");
        if (section == null) {
            return;
        }

        for (String key : section.getKeys(false)) {
            int id;
            try {
                id = Integer.parseInt(key);
            } catch (NumberFormatException e) {
                LOGGER.warning("Invalid title ID: " + key);
                continue;
            }

            ConfigurationSection entry = section.getConfigurationSection(key);
            if (entry == null) {
                continue;
            }

            ConfigurationSection nameSection = entry.getConfigurationSection("name");
            String nameRu = nameSection != null ? nameSection.getString("ru", "Title " + id) : "Title " + id;
            String nameEn = nameSection != null ? nameSection.getString("en", "Title " + id) : "Title " + id;
            String color = entry.getString("color", "white");

            ConfigurationSection descSection = entry.getConfigurationSection("description");
            String descRu = descSection != null ? descSection.getString("ru", "") : "";
            String descEn = descSection != null ? descSection.getString("en", "") : "";

            titles.put(id, new Title(id, nameRu, nameEn, color, descRu, descEn));
        }

        LOGGER.info("Loaded " + titles.size() + " titles");
    }

    public Optional<Title> getTitle(int id) {
        return Optional.ofNullable(titles.get(id));
    }

    public Map<Integer, Title> getAllTitles() {
        return Collections.unmodifiableMap(titles);
    }
}
