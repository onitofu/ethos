package ru.nyansus.mc.domya_fate;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import ru.nyansus.mc.domya_fate.title.Title;
import ru.nyansus.mc.domya_fate.buff.BuffApplyTask;
import ru.nyansus.mc.domya_fate.buff.BuffConfig;
import ru.nyansus.mc.domya_fate.buff.BlockRidingListener;
import ru.nyansus.mc.domya_fate.buff.BlockTamingListener;
import ru.nyansus.mc.domya_fate.buff.GolemAggroListener;
import ru.nyansus.mc.domya_fate.buff.HostileMobListener;
import ru.nyansus.mc.domya_fate.buff.PvpDamageListener;
import ru.nyansus.mc.domya_fate.buff.TradeBlockListener;
import ru.nyansus.mc.domya_fate.buff.XpBonusListener;
import ru.nyansus.mc.domya_fate.command.DomyaFateCommand;
import ru.nyansus.mc.domya_fate.command.KarmaCommand;
import ru.nyansus.mc.domya_fate.command.TitleCommand;
import ru.nyansus.mc.domya_fate.karma.AntiFarmManager;
import ru.nyansus.mc.domya_fate.karma.KarmaManager;
import ru.nyansus.mc.domya_fate.karma.KarmaTitleManager;
import ru.nyansus.mc.domya_fate.karma.StatsStorage;
import ru.nyansus.mc.domya_fate.storage.DatabaseManager;
import ru.nyansus.mc.domya_fate.storage.SqliteKarmaStorage;
import ru.nyansus.mc.domya_fate.storage.SqliteStatsStorage;
import ru.nyansus.mc.domya_fate.storage.SqliteTitleStorage;
import ru.nyansus.mc.domya_fate.listener.AnimalListener;
import ru.nyansus.mc.domya_fate.listener.MobKillListener;
import ru.nyansus.mc.domya_fate.listener.PlayerJoinListener;
import ru.nyansus.mc.domya_fate.listener.PlayerKillListener;
import ru.nyansus.mc.domya_fate.listener.StatsListener;
import ru.nyansus.mc.domya_fate.listener.TradeListener;
import ru.nyansus.mc.domya_fate.listener.VillagerCureListener;
import ru.nyansus.mc.domya_fate.title.TitleManager;
import ru.nyansus.mc.domya_fate.title.TitleRegistry;
import ru.nyansus.mc.domya_fate.title.UnlockScanTask;

import java.sql.SQLException;

public class DomyaFate extends JavaPlugin {

    private Messages messages;
    private KarmaManager karmaManager;
    private KarmaTitleManager karmaTitleManager;
    private AntiFarmManager antiFarmManager;
    private BuffConfig buffConfig;
    private TitleManager titleManager;
    private StatsStorage statsStorage;
    private DatabaseManager databaseManager;

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
        new BuffApplyTask(this, karmaManager).runTaskTimer(this, 100L, interval);

        long unlockInterval = getConfig().getLong("unlock-check-interval", 6000L);
        new UnlockScanTask(this).runTaskTimer(this, 200L, unlockInterval);
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
    }

    private void registerCommands() {
        var karmaCmd = getCommand("karma");
        if (karmaCmd != null) {
            KarmaCommand karmaCommand = new KarmaCommand(this);
            karmaCmd.setExecutor(karmaCommand);
            karmaCmd.setTabCompleter(karmaCommand);
        }
        var dtCmd = getCommand("dt");
        if (dtCmd != null) {
            TitleCommand titleCommand = new TitleCommand(this);
            dtCmd.setExecutor(titleCommand);
            dtCmd.setTabCompleter(titleCommand);
        }
        var fateCmd = getCommand("domyafate");
        if (fateCmd != null) {
            DomyaFateCommand fateCommand = new DomyaFateCommand(this);
            fateCmd.setExecutor(fateCommand);
            fateCmd.setTabCompleter(fateCommand);
        }
    }

    private void registerPlaceholders() {
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new ru.nyansus.mc.domya_fate.integration.DomyaFatePlaceholders(this).register();
            getLogger().info("PlaceholderAPI integration enabled.");
        }
    }

    public void reloadAll() {
        reloadConfig();
        messages.reload();
        karmaTitleManager = new KarmaTitleManager(getConfig());
        antiFarmManager = new AntiFarmManager(getConfig());
        buffConfig = new BuffConfig(getConfig());
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

    public StatsStorage getStatsStorage() {
        return statsStorage;
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
                    "{title}", title.nameRu());
            online.sendMessage(msg);
        }
        String consoleMsg = messages.get("ru", "title.unlock-broadcast")
                .replace("{player}", player.getName())
                .replace("{title}", title.nameRu());
        getServer().getConsoleSender().sendMessage(consoleMsg);

        player.sendMessage(messages.get(player, "title.unlock-hint"));
    }
}
