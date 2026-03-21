package ru.nyansus.mc.domya_fate;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import ru.nyansus.mc.domya_fate.title.Title;
import ru.nyansus.mc.domya_fate.buff.BuffApplyTask;
import ru.nyansus.mc.domya_fate.buff.BuffConfig;
import ru.nyansus.mc.domya_fate.buff.GolemAggroListener;
import ru.nyansus.mc.domya_fate.buff.PvpDamageListener;
import ru.nyansus.mc.domya_fate.buff.TradeBlockListener;
import ru.nyansus.mc.domya_fate.buff.XpBonusListener;
import ru.nyansus.mc.domya_fate.command.KarmaCommand;
import ru.nyansus.mc.domya_fate.command.TitleCommand;
import ru.nyansus.mc.domya_fate.karma.AntiFarmManager;
import ru.nyansus.mc.domya_fate.karma.KarmaManager;
import ru.nyansus.mc.domya_fate.karma.KarmaTitleManager;
import ru.nyansus.mc.domya_fate.karma.YamlKarmaStorage;
import ru.nyansus.mc.domya_fate.listener.AnimalListener;
import ru.nyansus.mc.domya_fate.listener.MobKillListener;
import ru.nyansus.mc.domya_fate.listener.PlayerKillListener;
import ru.nyansus.mc.domya_fate.listener.TradeListener;
import ru.nyansus.mc.domya_fate.listener.VillagerCureListener;
import ru.nyansus.mc.domya_fate.title.TitleManager;
import ru.nyansus.mc.domya_fate.title.TitleRegistry;
import ru.nyansus.mc.domya_fate.title.UnlockScanTask;
import ru.nyansus.mc.domya_fate.title.YamlTitleStorage;

public class DomyaFate extends JavaPlugin {

    private Messages messages;
    private KarmaManager karmaManager;
    private KarmaTitleManager karmaTitleManager;
    private AntiFarmManager antiFarmManager;
    private BuffConfig buffConfig;
    private TitleManager titleManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        messages = new Messages(this);

        YamlKarmaStorage karmaStorage = new YamlKarmaStorage(getDataFolder());
        karmaStorage.load();

        karmaManager = new KarmaManager(this, karmaStorage);
        karmaTitleManager = new KarmaTitleManager(getConfig());
        antiFarmManager = new AntiFarmManager(getConfig());
        buffConfig = new BuffConfig(getConfig());

        TitleRegistry titleRegistry = new TitleRegistry(this);
        YamlTitleStorage titleStorage = new YamlTitleStorage(getDataFolder());
        titleStorage.load();
        titleManager = new TitleManager(titleRegistry, titleStorage);

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
        if (karmaManager != null) {
            karmaManager.saveAll();
        }
        if (titleManager != null) {
            titleManager.saveAll();
        }
    }

    private void registerListeners() {
        var pm = getServer().getPluginManager();
        pm.registerEvents(new MobKillListener(this), this);
        pm.registerEvents(new PlayerKillListener(this), this);
        pm.registerEvents(new TradeListener(this), this);
        pm.registerEvents(new VillagerCureListener(this), this);
        pm.registerEvents(new AnimalListener(this), this);
        pm.registerEvents(new PvpDamageListener(this), this);
        pm.registerEvents(new TradeBlockListener(this), this);
        pm.registerEvents(new GolemAggroListener(this), this);
        pm.registerEvents(new XpBonusListener(this), this);
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
