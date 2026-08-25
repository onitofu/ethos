package ru.nyansus.mc.ethos;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import ru.nyansus.mc.ethos.title.Title;
import ru.nyansus.mc.ethos.buff.BuffApplyTask;
import ru.nyansus.mc.ethos.buff.BuffConfig;
import ru.nyansus.mc.ethos.buff.BlockRidingListener;
import ru.nyansus.mc.ethos.buff.BlockTamingListener;
import ru.nyansus.mc.ethos.buff.GolemAggroListener;
import ru.nyansus.mc.ethos.buff.HostileMobListener;
import ru.nyansus.mc.ethos.buff.MobBehaviorTask;
import ru.nyansus.mc.ethos.buff.KarmaEffectsListener;
import ru.nyansus.mc.ethos.buff.PvpDamageListener;
import ru.nyansus.mc.ethos.buff.TradeBlockListener;
import ru.nyansus.mc.ethos.buff.XpBonusListener;
import ru.nyansus.mc.ethos.command.EthosCommand;
import ru.nyansus.mc.ethos.command.KarmaCommand;
import ru.nyansus.mc.ethos.command.TitleCommand;
import ru.nyansus.mc.ethos.karma.AntiFarmManager;
import ru.nyansus.mc.ethos.karma.KarmaManager;
import ru.nyansus.mc.ethos.karma.KarmaTitleManager;
import ru.nyansus.mc.ethos.karma.StatsStorage;
import ru.nyansus.mc.ethos.storage.DatabaseManager;
import ru.nyansus.mc.ethos.storage.SqliteKarmaStorage;
import ru.nyansus.mc.ethos.storage.SqliteStatsStorage;
import ru.nyansus.mc.ethos.storage.SqliteTitleStorage;
import ru.nyansus.mc.ethos.listener.AnimalListener;
import ru.nyansus.mc.ethos.listener.MobKillListener;
import ru.nyansus.mc.ethos.listener.PlayerJoinListener;
import ru.nyansus.mc.ethos.listener.PlayerKillListener;
import ru.nyansus.mc.ethos.listener.StatsListener;
import ru.nyansus.mc.ethos.listener.TradeListener;
import ru.nyansus.mc.ethos.listener.VillagerCureListener;
import ru.nyansus.mc.ethos.title.TitleManager;
import ru.nyansus.mc.ethos.title.TitleRegistry;
import ru.nyansus.mc.ethos.title.UnlockScanTask;
import ru.nyansus.mc.ethos.util.StatKeys;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class Ethos extends JavaPlugin {

    private Messages messages;
    private KarmaManager karmaManager;
    private KarmaTitleManager karmaTitleManager;
    private AntiFarmManager antiFarmManager;
    private BuffConfig buffConfig;
    private TitleManager titleManager;
    private StatsStorage statsStorage;
    private DatabaseManager databaseManager;
    private BuffApplyTask buffApplyTask;
    private final Map<UUID, Boolean> karmaEffectsEnabled = new HashMap<>();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        messages = new Messages(this);

        databaseManager = new DatabaseManager(getDataFolder());
        try {
            databaseManager.initialize();
        } catch (SQLException e) {
            getLogger().severe("Failed to initialize database: " + e.getMessage());
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        karmaManager = new KarmaManager(this, new SqliteKarmaStorage(databaseManager));
        karmaTitleManager = new KarmaTitleManager(getConfig());
        antiFarmManager = new AntiFarmManager(getConfig());
        buffConfig = new BuffConfig(getConfig());
        statsStorage = new SqliteStatsStorage(databaseManager);

        TitleRegistry titleRegistry = new TitleRegistry(this);
        titleManager = new TitleManager(titleRegistry, new SqliteTitleStorage(databaseManager));

        karmaManager.setOnKarmaChange((uuid, karma) -> {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                syncKarmaTitles(player);
            }
        });

        registerListeners();
        registerCommands();
        registerPlaceholders();

        long interval = getConfig().getLong("buff-check-interval", 1200L);
        buffApplyTask = new BuffApplyTask(this, karmaManager);
        buffApplyTask.runTaskTimer(this, 100L, interval);

        long unlockInterval = getConfig().getLong("unlock-check-interval", 6000L);
        new UnlockScanTask(this).runTaskTimer(this, 200L, unlockInterval);

        new MobBehaviorTask(this).runTaskTimer(this, 20L, 20L);
    }

    @Override
    public void onDisable() {
        if (databaseManager != null) {
            databaseManager.close();
        }
    }

    private void registerListeners() {
        var pm = getServer().getPluginManager();
        pm.registerEvents(new PlayerJoinListener(this), this);
        pm.registerEvents(new MobKillListener(this), this);
        pm.registerEvents(new PlayerKillListener(this), this);
        pm.registerEvents(new TradeListener(this), this);
        pm.registerEvents(new VillagerCureListener(this), this);
        pm.registerEvents(new AnimalListener(this), this);
        pm.registerEvents(new StatsListener(this), this);
        pm.registerEvents(new PvpDamageListener(this), this);
        pm.registerEvents(new TradeBlockListener(this), this);
        pm.registerEvents(new GolemAggroListener(this), this);
        pm.registerEvents(new XpBonusListener(this), this);
        pm.registerEvents(new HostileMobListener(this), this);
        pm.registerEvents(new BlockTamingListener(this), this);
        pm.registerEvents(new BlockRidingListener(this), this);
        pm.registerEvents(new KarmaEffectsListener(this), this);
    }

    private void registerCommands() {
        var karmaCmd = getCommand("karma");
        if (karmaCmd != null) {
            KarmaCommand karmaCommand = new KarmaCommand(this);
            karmaCmd.setExecutor(karmaCommand);
            karmaCmd.setTabCompleter(karmaCommand);
        }
        var titleCmd = getCommand("etitle");
        if (titleCmd != null) {
            TitleCommand titleCommand = new TitleCommand(this);
            titleCmd.setExecutor(titleCommand);
            titleCmd.setTabCompleter(titleCommand);
        }
        var fateCmd = getCommand("ethos");
        if (fateCmd != null) {
            EthosCommand fateCommand = new EthosCommand(this);
            fateCmd.setExecutor(fateCommand);
            fateCmd.setTabCompleter(fateCommand);
        }
    }

    private void registerPlaceholders() {
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new ru.nyansus.mc.ethos.integration.EthosPlaceholders(this).register();
            getLogger().info("PlaceholderAPI integration enabled.");
        }
    }

    public void reloadAll() {
        reloadConfig();
        messages.reload();
        karmaTitleManager = new KarmaTitleManager(getConfig());
        antiFarmManager = new AntiFarmManager(getConfig());
        buffConfig = new BuffConfig(getConfig());
        TitleRegistry titleRegistry = new TitleRegistry(this);
        titleManager = new TitleManager(titleRegistry, titleManager.getStorage());
    }

    public Messages getMessages() {
        return messages;
    }

    public KarmaManager getKarmaManager() {
        return karmaManager;
    }

    public KarmaTitleManager getKarmaTitleManager() {
        return karmaTitleManager;
    }

    public AntiFarmManager getAntiFarmManager() {
        return antiFarmManager;
    }

    public BuffConfig getBuffConfig() {
        return buffConfig;
    }

    public TitleManager getTitleManager() {
        return titleManager;
    }

    public float getTitleGradientShift() {
        return (float) getConfig().getDouble("title.gradient-shift", 25d);
    }

    public StatsStorage getStatsStorage() {
        return statsStorage;
    }

    public boolean areKarmaEffectsEnabled(UUID uuid) {
        return karmaEffectsEnabled.computeIfAbsent(uuid,
                key -> statsStorage.getStat(key, StatKeys.KARMA_EFFECTS_DISABLED) == 0);
    }

    public boolean areKarmaEffectsEnabled(Player player) {
        return areKarmaEffectsEnabled(player.getUniqueId());
    }

    public void loadKarmaEffectsState(UUID uuid) {
        karmaEffectsEnabled.put(uuid,
                statsStorage.getStat(uuid, StatKeys.KARMA_EFFECTS_DISABLED) == 0);
    }

    public void setKarmaEffectsEnabled(UUID uuid, boolean enabled) {
        statsStorage.setStat(uuid, StatKeys.KARMA_EFFECTS_DISABLED, enabled ? 0 : 1);
        karmaEffectsEnabled.put(uuid, enabled);
    }

    public void clearKarmaEffectsState(UUID uuid) {
        karmaEffectsEnabled.remove(uuid);
        if (buffApplyTask != null) {
            buffApplyTask.forgetPlayer(uuid);
        }
    }

    public void clearAppliedKarmaEffects(Player player) {
        if (buffApplyTask != null) {
            buffApplyTask.clearAppliedEffects(player);
        }
    }

    public void syncKarmaTitles(Player player) {
        int karma = karmaManager.getKarma(player.getUniqueId());
        java.util.List<Title> newTitles = titleManager.syncKarmaTitles(
                player.getUniqueId(), karma, karmaTitleManager);
        for (Title title : newTitles) {
            broadcastTitleUnlock(player, title);
        }
    }

    public void broadcastTitleUnlock(Player player, Title title) {
        for (Player online : getServer().getOnlinePlayers()) {
            String msg = messages.get(online, "title.unlock-broadcast",
                    "{player}", player.getName(),
                    "{title}", title.localizedName(online, messages));
            online.sendMessage(msg);
        }
        String consoleMsg = messages.get("ru", "title.unlock-broadcast")
                .replace("{player}", player.getName())
                .replace("{title}", title.nameRu());
        getServer().getConsoleSender().sendMessage(consoleMsg);

        player.sendMessage(messages.get(player, "title.unlock-hint"));
    }
}
