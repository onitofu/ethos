package ru.nyansus.mc.domya_fate.command;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import ru.nyansus.mc.domya_fate.DomyaFate;
import ru.nyansus.mc.domya_fate.buff.BuffConfig;
import ru.nyansus.mc.domya_fate.buff.BuffTier;
import ru.nyansus.mc.domya_fate.karma.KarmaTitle;

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

        if (args.length == 0) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(plugin.getMessages().get(sender, "karma.player-only"));
                return true;
            }
            showKarma(player, player.getUniqueId(), player.getName());
            return true;
        }

        if (!sender.hasPermission("domya.karma.view.others")) {
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
        if (!sender.hasPermission("domya.karma.admin")) {
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

    private void showKarma(Player viewer, UUID targetUuid, String targetName) {
        int karma = plugin.getKarmaManager().getKarma(targetUuid);
        Optional<KarmaTitle> karmaTitle = plugin.getKarmaTitleManager().getTitle(karma);

        String bar = buildBar(karma);
        String titleStr = karmaTitle
                .flatMap(kt -> plugin.getTitleManager().getRegistry().getTitle(kt.id()))
                .map(t -> " §7[" + colorCode(t.color()) + t.nameRu() + "§7]")
                .orElse("");

        viewer.sendMessage(plugin.getMessages().get(viewer, "karma.display",
                "{player}", targetName,
                "{karma}", String.valueOf(karma),
                "{bar}", bar,
                "{title}", titleStr));

        showEffects(viewer, karma);
    }

    private void showEffects(Player viewer, int karma) {
        BuffConfig config = plugin.getBuffConfig();
        List<String> buffs = new ArrayList<>();
        List<String> debuffs = new ArrayList<>();

        if (karma < 0) {
            BuffTier tier = config.findNegativeTier(karma);
            if (tier != null) {
                if (tier.mobDamageBonus() > 0) {
                    buffs.add("+" + pct(tier.mobDamageBonus())
                            + " " + plugin.getMessages().get(viewer, "karma.buff.mob-damage"));
                }
                if (tier.speedBonus() > 0) {
                    buffs.add("+" + pct(tier.speedBonus())
                            + " " + plugin.getMessages().get(viewer, "karma.buff.speed"));
                }
                if (tier.tradePriceIncrease() > 0) {
                    debuffs.add(plugin.getMessages().get(viewer, "karma.debuff.trade-prices",
                            "{level}", String.valueOf(tier.tradePriceIncrease())));
                }
                if (tier.blockTrading()) {
                    debuffs.add(plugin.getMessages().get(viewer, "karma.debuff.trade-blocked"));
                }
                if (tier.golemAggro()) {
                    debuffs.add(plugin.getMessages().get(viewer, "karma.debuff.golem-aggro"));
                }
            }
        } else if (karma > 0) {
            BuffTier tier = config.findPositiveTier(karma);
            if (tier != null) {
                if (tier.xpBonus() > 0) {
                    buffs.add("+" + pct(tier.xpBonus())
                            + " " + plugin.getMessages().get(viewer, "karma.buff.xp"));
                }
                if (tier.effects().stream().anyMatch(e ->
                        e.type().equals(org.bukkit.potion.PotionEffectType.HERO_OF_THE_VILLAGE))) {
                    buffs.add(plugin.getMessages().get(viewer, "karma.buff.trade-discount"));
                }
                if (tier.effects().stream().anyMatch(e ->
                        e.type().equals(org.bukkit.potion.PotionEffectType.LUCK))) {
                    buffs.add(plugin.getMessages().get(viewer, "karma.buff.luck"));
                }
                if (tier.pvpDamagePenalty() > 0) {
                    debuffs.add("-" + pct(tier.pvpDamagePenalty())
                            + " " + plugin.getMessages().get(viewer, "karma.debuff.pvp-damage"));
                }
            }
        }

        for (String buff : buffs) {
            viewer.sendMessage("  §a▲ " + buff);
        }
        for (String debuff : debuffs) {
            viewer.sendMessage("  §c▼ " + debuff);
        }
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

    private String colorCode(String color) {
        return switch (color) {
            case "dark_red" -> "§4";
            case "red" -> "§c";
            case "gold" -> "§6";
            case "green" -> "§a";
            case "dark_green" -> "§2";
            case "aqua" -> "§b";
            case "light_purple" -> "§d";
            case "dark_purple" -> "§5";
            case "white" -> "§f";
            case "gray" -> "§7";
            case "dark_gray" -> "§8";
            case "yellow" -> "§e";
            case "blue" -> "§9";
            default -> "§f";
        };
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command,
                                      String label, String[] args) {
        if (args.length == 1) {
            List<String> completions = new ArrayList<>();
            String prefix = args[0].toLowerCase();
            if (sender.hasPermission("domya.karma.admin") && "set".startsWith(prefix)) {
                completions.add("set");
            }
            if (sender.hasPermission("domya.karma.view.others")) {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (player.getName().toLowerCase().startsWith(prefix)) {
                        completions.add(player.getName());
                    }
                }
            }
            return completions;
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("set")
                && sender.hasPermission("domya.karma.admin")) {
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
                && sender.hasPermission("domya.karma.admin")) {
            return List.of("-1000", "-500", "0", "500", "1000");
        }
        return List.of();
    }
}
