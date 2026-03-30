package ru.nyansus.mc.domya_fate.command;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import ru.nyansus.mc.domya_fate.DomyaFate;
import ru.nyansus.mc.domya_fate.buff.BuffEffect;
import ru.nyansus.mc.domya_fate.buff.BuffTier;
import ru.nyansus.mc.domya_fate.karma.KarmaTitle;
import ru.nyansus.mc.domya_fate.karma.StatsStorage;
import ru.nyansus.mc.domya_fate.util.ColorUtil;
import ru.nyansus.mc.domya_fate.util.StatKeys;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class KarmaCommand implements CommandExecutor, TabCompleter {

    private static final int BAR_LENGTH = 20;

    private final DomyaFate plugin;

    public KarmaCommand(DomyaFate plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length >= 1 && args[0].equalsIgnoreCase("set")) {
            return handleSet(sender, args);
        }
        if (args.length >= 1 && args[0].equalsIgnoreCase("reset")) {
            return handleReset(sender);
        }

        if (args.length == 0) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(plugin.getMessages().get(sender, "karma.player-only"));
                return true;
            }
            showKarma(player, player.getUniqueId(), player.getName());
            return true;
        }

        if (!sender.hasPermission("ethos.karma.view.others")) {
            sender.sendMessage(plugin.getMessages().get(sender, "command.no-permission"));
            return true;
        }

        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            sender.sendMessage(plugin.getMessages().get(sender, "karma.player-not-found",
                    "{player}", args[0]));
            return true;
        }

        if (sender instanceof Player player) {
            showKarma(player, target.getUniqueId(), target.getName());
        } else {
            showKarmaConsole(sender, target.getUniqueId(), target.getName());
        }
        return true;
    }

    private boolean handleSet(CommandSender sender, String[] args) {
        if (!sender.hasPermission("ethos.karma.admin")) {
            sender.sendMessage(plugin.getMessages().get(sender, "command.no-permission"));
            return true;
        }
        if (args.length < 3) {
            sender.sendMessage(plugin.getMessages().get(sender, "karma.set-usage"));
            return true;
        }

        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            sender.sendMessage(plugin.getMessages().get(sender, "karma.player-not-found",
                    "{player}", args[1]));
            return true;
        }

        int value;
        try {
            value = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            sender.sendMessage(plugin.getMessages().get(sender, "karma.invalid-number"));
            return true;
        }

        plugin.getKarmaManager().setKarma(target.getUniqueId(), value);
        sender.sendMessage(plugin.getMessages().get(sender, "karma.set-success",
                "{player}", target.getName(),
                "{karma}", String.valueOf(value)));
        return true;
    }

    private boolean handleReset(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.getMessages().get(sender, "karma.player-only"));
            return true;
        }

        long cooldownDays = plugin.getConfig().getLong("karma-reset-cooldown-days", 30);
        long cooldownMs = cooldownDays * 86_400_000L;
        StatsStorage stats = plugin.getStatsStorage();
        long lastReset = stats.getLongStat(player.getUniqueId(), StatKeys.LAST_KARMA_RESET);
        long now = System.currentTimeMillis();

        if (lastReset > 0 && now - lastReset < cooldownMs) {
            long remainingDays = (cooldownMs - (now - lastReset)) / 86_400_000L + 1;
            player.sendMessage(plugin.getMessages().get(player, "karma.reset-cooldown",
                    "{days}", String.valueOf(remainingDays)));
            return true;
        }

        int oldKarma = plugin.getKarmaManager().getKarma(player.getUniqueId());
        plugin.getKarmaManager().setKarma(player.getUniqueId(), 0);
        stats.setStat(player.getUniqueId(), StatKeys.LAST_KARMA_RESET, now);
        player.sendMessage(plugin.getMessages().get(player, "karma.reset-success",
                "{old}", String.valueOf(oldKarma)));
        return true;
    }

    private void showKarma(Player viewer, UUID targetUuid, String targetName) {
        int karma = plugin.getKarmaManager().getKarma(targetUuid);
        Optional<KarmaTitle> karmaTitle = plugin.getKarmaTitleManager().getTitle(karma);

        String bar = buildBar(karma);
        String titleStr = karmaTitle
                .flatMap(kt -> plugin.getTitleManager().getRegistry().getTitle(kt.id()))
                .map(t -> " §7[" + ColorUtil.colorCode(t.color()) + t.nameRu() + "§7]")
                .orElse("");

        viewer.sendMessage(plugin.getMessages().get(viewer, "karma.display",
                "{player}", targetName,
                "{karma}", String.valueOf(karma),
                "{bar}", bar,
                "{title}", titleStr));

        showEffects(viewer, karma);
    }

    private void showEffects(Player viewer, int karma) {
        BuffTier tier = plugin.getBuffConfig().findTier(karma);
        if (tier == null) {
            return;
        }

        for (BuffEffect effect : tier.effects()) {
            String line = formatEffect(viewer, effect);
            if (line != null) {
                viewer.sendMessage(line);
            }
        }
    }

    private String formatEffect(Player viewer, BuffEffect effect) {
        if (effect.effectType() == ru.nyansus.mc.domya_fate.buff.EffectType.POTION_EFFECT) {
            return formatPotionEffect(viewer, effect);
        }

        String key = "karma.effect." + effect.effectType().name().toLowerCase();
        String msg = plugin.getMessages().get(viewer, key);
        if (msg.startsWith("[")) {
            return null;
        }

        boolean isBuff = isBuff(effect);
        String prefix = isBuff ? "  §a▲ " : "  §c▼ ";
        String value = formatValue(effect);

        return prefix + (value.isEmpty() ? msg : value + " " + msg);
    }

    private String formatPotionEffect(Player viewer, BuffEffect effect) {
        String name = effect.potionType().translationKey();
        int level = effect.amplifier() + 1;
        String display = plugin.getMessages().get(viewer,
                "karma.potion." + effect.potionType().getKey().getKey(),
                "{level}", String.valueOf(level));
        if (display.startsWith("[")) {
            display = effect.potionType().getKey().getKey() + " " + level;
        }
        return "  §a▲ " + display;
    }

    private boolean isBuff(BuffEffect effect) {
        if (effect.value() < 0 && (
                effect.effectType() == ru.nyansus.mc.domya_fate.buff.EffectType.MOB_DAMAGE_BONUS
                || effect.effectType() == ru.nyansus.mc.domya_fate.buff.EffectType.PASSIVE_MOB_DAMAGE_BONUS)) {
            return false;
        }
        return switch (effect.effectType()) {
            case PVP_DAMAGE_PENALTY, XP_PENALTY, XP_DEATH_PENALTY,
                    TRADE_PRICE_INCREASE, BLOCK_TRADING, GOLEM_AGGRO,
                    PASSIVE_MOB_FLEE, PASSIVE_MOB_HOSTILE, BLOCK_TAMING, BLOCK_RIDING,
                    GLOWING, HUNGER_RATE, HOSTILE_MOB_INCREASED_RANGE -> false;
            default -> true;
        };
    }

    private String formatValue(BuffEffect effect) {
        double val = effect.value();
        return switch (effect.effectType()) {
            case MOB_DAMAGE_BONUS, PASSIVE_MOB_DAMAGE_BONUS ->
                    (val >= 0 ? "+" : "-") + pct(Math.abs(val));
            case PVP_DAMAGE_BONUS, XP_BONUS, LOOT_BONUS,
                    SPEED_BONUS, DOUBLE_CROP_CHANCE,
                    KEEP_INVENTORY_CHANCE -> "+" + pct(val);
            case RESISTANCE, FALL_DAMAGE_REDUCTION, FIRE_RESISTANCE -> "-" + pct(val);
            case PVP_DAMAGE_PENALTY, XP_PENALTY, XP_DEATH_PENALTY -> "-" + pct(val);
            case HEALTH_BONUS -> "+" + (int) (val / 2) + "❤";
            default -> "";
        };
    }

    private String pct(double value) {
        return (int) (value * 100) + "%";
    }

    private void showKarmaConsole(CommandSender sender, UUID targetUuid, String targetName) {
        int karma = plugin.getKarmaManager().getKarma(targetUuid);
        sender.sendMessage(targetName + ": " + karma);
    }

    private String buildBar(int karma) {
        int min = plugin.getConfig().getInt("karma.min", -1000);
        int max = plugin.getConfig().getInt("karma.max", 1000);
        double ratio = (double) (karma - min) / (max - min);
        int filled = (int) (ratio * BAR_LENGTH);

        StringBuilder bar = new StringBuilder("§8[");
        for (int i = 0; i < BAR_LENGTH; i++) {
            if (i < filled) {
                if (karma < 0) {
                    bar.append("§c|");
                } else {
                    bar.append("§a|");
                }
            } else {
                bar.append("§7|");
            }
        }
        bar.append("§8]");
        return bar.toString();
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command,
                                      String label, String[] args) {
        if (args.length == 1) {
            List<String> completions = new ArrayList<>();
            String prefix = args[0].toLowerCase();
            if (sender.hasPermission("ethos.karma.admin") && "set".startsWith(prefix)) {
                completions.add("set");
            }
            if ("reset".startsWith(prefix)) {
                completions.add("reset");
            }
            if (sender.hasPermission("ethos.karma.view.others")) {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (player.getName().toLowerCase().startsWith(prefix)) {
                        completions.add(player.getName());
                    }
                }
            }
            return completions;
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("set")
                && sender.hasPermission("ethos.karma.admin")) {
            List<String> names = new ArrayList<>();
            String prefix = args[1].toLowerCase();
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.getName().toLowerCase().startsWith(prefix)) {
                    names.add(player.getName());
                }
            }
            return names;
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("set")
                && sender.hasPermission("ethos.karma.admin")) {
            return List.of("-10000", "-5000", "0", "5000", "10000");
        }
        return List.of();
    }
}
