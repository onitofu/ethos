package ru.nyansus.mc.ethos.command;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import ru.nyansus.mc.ethos.Ethos;
import ru.nyansus.mc.ethos.title.Title;
import ru.nyansus.mc.ethos.title.TitleManager;
import ru.nyansus.mc.ethos.title.TitleRenderer;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

public class TitleCommand implements CommandExecutor, TabCompleter {

    private final Ethos plugin;

    public TitleCommand(Ethos plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length >= 1 && args[0].equalsIgnoreCase("give")) {
            return handleGive(sender, args);
        }

        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.getMessages().get(sender, "karma.player-only"));
            return true;
        }

        if (args.length == 0) {
            return handleList(player);
        }

        if (args[0].equalsIgnoreCase("reset")) {
            return handleReset(player);
        }

        if (args[0].equalsIgnoreCase("info") && args.length >= 2) {
            return handleInfo(player, args[1]);
        }

        return handleSelect(player, args[0]);
    }

    private static final MiniMessage MM = MiniMessage.miniMessage();
    private static final int PROGRESS_BAR_WIDTH = 20;

    private static String buildProgressLine(int unlocked, int total) {
        double ratio = total == 0 ? 0 : (double) unlocked / total;
        int filled = (int) Math.round(ratio * PROGRESS_BAR_WIDTH);
        String bar = "<green>" + "█".repeat(filled) + "</green>"
                + "<dark_gray>" + "░".repeat(PROGRESS_BAR_WIDTH - filled) + "</dark_gray>";
        String percent = String.format(Locale.ROOT, "%.1f", ratio * 100);
        return "  <gray>" + unlocked + "<dark_gray>/</dark_gray><gray>" + total
                + " <dark_gray>(</dark_gray><yellow>" + percent + "%</yellow><dark_gray>)</dark_gray> "
                + bar;
    }

    private boolean handleList(Player player) {
        TitleManager tm = plugin.getTitleManager();
        Set<Integer> unlocked = tm.getUnlockedTitles(player.getUniqueId());
        Optional<Title> active = tm.getActiveTitle(player.getUniqueId());

        if (unlocked.isEmpty()) {
            player.sendMessage(plugin.getMessages().get(player, "title.no-titles"));
            return true;
        }

        player.sendMessage(plugin.getMessages().get(player, "title.list-header"));
        player.sendMessage(MM.deserialize(buildProgressLine(
                unlocked.size(), tm.getRegistry().getAllTitles().size())));

        int activeId = active.map(Title::id).orElse(-1);
        for (int id : unlocked.stream().sorted().toList()) {
            Optional<Title> title = tm.getRegistry().getTitle(id);
            if (title.isEmpty()) {
                continue;
            }
            Title t = title.get();
            String marker = id == activeId ? " <green>✔</green>" : "";
            String desc = t.descriptionRu().isEmpty() ? ""
                    : " <dark_gray>-</dark_gray> <gray>" + t.descriptionRu() + "</gray>";
            String rendered = TitleRenderer.render(t.nameRu(), t.color(), plugin.getTitleGradientShift());
            Component line = MM.deserialize(
                    "  " + rendered + " <gray>(ID: " + t.id() + ")</gray>" + marker + desc);
            player.sendMessage(line);
        }
        return true;
    }

    private boolean handleSelect(Player player, String idStr) {
        int id;
        try {
            id = Integer.parseInt(idStr);
        } catch (NumberFormatException e) {
            player.sendMessage(plugin.getMessages().get(player, "title.invalid-id"));
            return true;
        }

        TitleManager tm = plugin.getTitleManager();
        if (tm.setActiveTitle(player.getUniqueId(), id)) {
            Optional<Title> title = tm.getRegistry().getTitle(id);
            String name = title.map(Title::nameRu).orElse(String.valueOf(id));
            player.sendMessage(plugin.getMessages().get(player, "title.selected",
                    "{title}", name));
        } else {
            player.sendMessage(plugin.getMessages().get(player, "title.not-unlocked"));
        }
        return true;
    }

    private boolean handleReset(Player player) {
        plugin.getTitleManager().resetActiveTitle(player.getUniqueId());
        player.sendMessage(plugin.getMessages().get(player, "title.reset"));
        return true;
    }

    private boolean handleInfo(Player player, String idStr) {
        int id;
        try {
            id = Integer.parseInt(idStr);
        } catch (NumberFormatException e) {
            player.sendMessage(plugin.getMessages().get(player, "title.invalid-id"));
            return true;
        }

        Optional<Title> title = plugin.getTitleManager().getRegistry().getTitle(id);
        if (title.isEmpty()) {
            player.sendMessage(plugin.getMessages().get(player, "title.not-found"));
            return true;
        }

        Title t = title.get();
        boolean unlocked = plugin.getTitleManager().getUnlockedTitles(player.getUniqueId()).contains(id);
        String status = unlocked ? "§a✔" : "§c✘";

        player.sendMessage(plugin.getMessages().get(player, "title.info-header"));
        String nameRendered = TitleRenderer.render(t.nameRu(), t.color(), plugin.getTitleGradientShift());
        String enRendered = TitleRenderer.render(t.nameEn(), t.color(), plugin.getTitleGradientShift());
        player.sendMessage(MM.deserialize(
                "  " + nameRendered + " <gray>/</gray> " + enRendered));
        player.sendMessage("  §7ID: " + t.id());
        if (!t.descriptionRu().isEmpty()) {
            player.sendMessage("  §7" + t.descriptionRu());
        }
        player.sendMessage("  §7" + plugin.getMessages().get(player, "title.status") + ": " + status);
        return true;
    }

    private boolean handleGive(CommandSender sender, String[] args) {
        if (!sender.hasPermission("ethos.title.admin")) {
            sender.sendMessage(plugin.getMessages().get(sender, "command.no-permission"));
            return true;
        }
        if (args.length < 3) {
            sender.sendMessage(plugin.getMessages().get(sender, "title.give-usage"));
            return true;
        }

        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            sender.sendMessage(plugin.getMessages().get(sender, "karma.player-not-found",
                    "{player}", args[1]));
            return true;
        }

        int titleId;
        try {
            titleId = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            sender.sendMessage(plugin.getMessages().get(sender, "title.invalid-id"));
            return true;
        }

        Optional<Title> title = plugin.getTitleManager().getRegistry().getTitle(titleId);
        if (title.isEmpty()) {
            sender.sendMessage(plugin.getMessages().get(sender, "title.not-found"));
            return true;
        }

        boolean isNew = plugin.getTitleManager().unlockTitle(target.getUniqueId(), titleId);
        sender.sendMessage(plugin.getMessages().get(sender, "title.give-success",
                "{player}", target.getName(),
                "{title}", title.get().nameRu()));
        if (isNew) {
            plugin.broadcastTitleUnlock(target, title.get());
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command,
                                      String label, String[] args) {
        if (args.length == 1) {
            List<String> completions = new ArrayList<>();
            String prefix = args[0].toLowerCase();
            for (String sub : List.of("reset", "info")) {
                if (sub.startsWith(prefix)) {
                    completions.add(sub);
                }
            }
            if (sender.hasPermission("ethos.title.admin") && "give".startsWith(prefix)) {
                completions.add("give");
            }
            if (sender instanceof Player player) {
                Set<Integer> unlocked = plugin.getTitleManager()
                        .getUnlockedTitles(player.getUniqueId());
                for (int id : unlocked) {
                    String idStr = String.valueOf(id);
                    if (idStr.startsWith(prefix)) {
                        completions.add(idStr);
                    }
                }
            }
            return completions;
        }
        if (args.length == 2) {
            if (args[0].equalsIgnoreCase("info")) {
                return completeTitleIds(args[1]);
            }
            if (args[0].equalsIgnoreCase("give") && sender.hasPermission("ethos.title.admin")) {
                return completePlayerNames(args[1]);
            }
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("give")
                && sender.hasPermission("ethos.title.admin")) {
            return completeTitleIds(args[2]);
        }
        return List.of();
    }

    private List<String> completeTitleIds(String prefix) {
        List<String> ids = new ArrayList<>();
        for (int id : plugin.getTitleManager().getRegistry().getAllTitles().keySet()) {
            String idStr = String.valueOf(id);
            if (idStr.startsWith(prefix)) {
                ids.add(idStr);
            }
        }
        return ids;
    }

    private List<String> completePlayerNames(String prefix) {
        List<String> names = new ArrayList<>();
        String lower = prefix.toLowerCase();
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getName().toLowerCase().startsWith(lower)) {
                names.add(player.getName());
            }
        }
        return names;
    }

}
