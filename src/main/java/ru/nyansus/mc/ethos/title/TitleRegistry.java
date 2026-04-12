package ru.nyansus.mc.ethos.title;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
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
            TitleColor color = parseColor(entry);

            ConfigurationSection descSection = entry.getConfigurationSection("description");
            String descRu = descSection != null ? descSection.getString("ru", "") : "";
            String descEn = descSection != null ? descSection.getString("en", "") : "";

            UnlockCondition condition = parseUnlockCondition(entry);

            titles.put(id, new Title(id, nameRu, nameEn, color, descRu, descEn, condition));
        }

        LOGGER.info("Loaded " + titles.size() + " titles");
    }

    private TitleColor parseColor(ConfigurationSection entry) {
        String autoGradient = entry.getString("auto-gradient");
        if (autoGradient != null && !autoGradient.isBlank()) {
            return new TitleColor(TitleColor.Mode.AUTO_GRADIENT, List.of(autoGradient));
        }
        List<String> gradient = entry.getStringList("gradient");
        if (!gradient.isEmpty()) {
            return new TitleColor(TitleColor.Mode.GRADIENT, List.copyOf(gradient));
        }
        if (entry.isList("color")) {
            List<String> list = entry.getStringList("color");
            if (!list.isEmpty()) {
                return new TitleColor(TitleColor.Mode.ALTERNATE, List.copyOf(list));
            }
        }
        String raw = entry.getString("color", "white");
        if (raw.startsWith("gradient:")) {
            String[] parts = raw.substring("gradient:".length()).split(":");
            List<String> list = new ArrayList<>();
            for (String p : parts) {
                if (!p.isBlank()) {
                    list.add(p.trim());
                }
            }
            if (!list.isEmpty()) {
                return new TitleColor(TitleColor.Mode.GRADIENT, list);
            }
        }
        return TitleColor.single(raw);
    }

    private UnlockCondition parseUnlockCondition(ConfigurationSection entry) {
        ConfigurationSection unlock = entry.getConfigurationSection("unlock");
        if (unlock == null) {
            return null;
        }

        String typeName = unlock.getString("type");
        if (typeName == null) {
            return null;
        }

        UnlockCondition.Type type;
        try {
            type = UnlockCondition.Type.valueOf(typeName);
        } catch (IllegalArgumentException e) {
            LOGGER.warning("Unknown unlock type: " + typeName);
            return null;
        }

        int value = unlock.getInt("value", 0);

        List<EntityType> entities = new ArrayList<>();
        String entityStr = unlock.getString("entity");
        if (entityStr != null) {
            for (String name : entityStr.split(",")) {
                try {
                    entities.add(EntityType.valueOf(name.trim()));
                } catch (IllegalArgumentException e) {
                    LOGGER.warning("Unknown entity type: " + name);
                }
            }
        }

        List<Material> materials = new ArrayList<>();
        String materialStr = unlock.getString("material");
        if (materialStr != null) {
            for (String name : materialStr.split(",")) {
                Material mat = Material.matchMaterial(name.trim());
                if (mat != null) {
                    materials.add(mat);
                } else {
                    LOGGER.warning("Unknown material: " + name);
                }
            }
        }

        String statKey = unlock.getString("stat-key", "");

        return new UnlockCondition(type, value, entities, materials, statKey);
    }

    public Optional<Title> getTitle(int id) {
        return Optional.ofNullable(titles.get(id));
    }

    public Map<Integer, Title> getAllTitles() {
        return Collections.unmodifiableMap(titles);
    }
}
