package ru.nyansus.mc.ethos;

import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;

public final class Messages {

    private static final String FALLBACK_LOCALE = "en";
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    private static final LegacyComponentSerializer LEGACY =
            LegacyComponentSerializer.legacySection();
    private static final Map<String, String> LOCALE_MAP = Map.of(
            "ru", "ru",
            "en", "en",
            "ru_ru", "ru",
            "en_us", "en",
            "en_gb", "en"
    );

    private final JavaPlugin plugin;
    private final Map<String, YamlConfiguration> locales = new HashMap<>();
    private String defaultLocale = FALLBACK_LOCALE;

    public Messages(JavaPlugin plugin) {
        this.plugin = plugin;
        load();
    }

    private void load() {
        locales.clear();
        loadLocale("ru", "lang/ru.yml");
        loadLocale("en", "lang/en.yml");
        defaultLocale = plugin.getConfig().getString("default-locale", FALLBACK_LOCALE)
                .toLowerCase(Locale.ROOT);
        if (!locales.containsKey(defaultLocale)) {
            defaultLocale = FALLBACK_LOCALE;
        }
    }

    private void loadLocale(String locale, String resource) {
        File localeFile = new File(plugin.getDataFolder(), resource);
        if (!localeFile.exists()) {
            plugin.saveResource(resource, false);
        }
        YamlConfiguration config = YamlConfiguration.loadConfiguration(localeFile);
        try (InputStream stream = plugin.getResource(resource)) {
            if (stream == null) {
                return;
            }
            config.setDefaults(YamlConfiguration.loadConfiguration(
                    new InputStreamReader(stream, StandardCharsets.UTF_8)));
            locales.put(locale, config);
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to load locale file: " + resource, e);
        }
    }

    public String get(CommandSender sender, String key) {
        String locale = defaultLocale;
        if (sender instanceof Player player) {
            locale = normalizeLocale(player.locale().toString());
        }
        return get(locale, key);
    }

    public String get(CommandSender sender, String key, String... replacements) {
        String msg = get(sender, key);
        for (int i = 0; i + 1 < replacements.length; i += 2) {
            msg = msg.replace(replacements[i], replacements[i + 1]);
        }
        return msg;
    }

    public String get(Player player, String key) {
        return get(normalizeLocale(player.locale().toString()), key);
    }

    public boolean isRussian(Player player) {
        return "ru".equals(normalizeLocale(player.locale().toString()));
    }

    public String get(String locale, String key) {
        String msg = getFromLocale(locale, key);
        if (msg == null && !FALLBACK_LOCALE.equals(locale)) {
            msg = getFromLocale(FALLBACK_LOCALE, key);
        }
        if (msg == null) {
            return "[" + key + "]";
        }
        return msg.indexOf('\u00a7') >= 0
                ? msg
                : LEGACY.serialize(MINI_MESSAGE.deserialize(msg));
    }

    public String render(String miniMessage) {
        return LEGACY.serialize(MINI_MESSAGE.deserialize(miniMessage));
    }

    private String getFromLocale(String locale, String key) {
        YamlConfiguration config = locales.get(locale);
        if (config == null) {
            return null;
        }
        return config.getString(key);
    }

    private static String normalizeLocale(String clientLocale) {
        if (clientLocale == null || clientLocale.isEmpty()) {
            return FALLBACK_LOCALE;
        }
        String lower = clientLocale.toLowerCase(Locale.ROOT);
        return LOCALE_MAP.getOrDefault(lower, lower.split("_")[0]);
    }

    public void reload() {
        load();
    }
}
