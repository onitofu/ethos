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

    private static final int TITLES_PER_PAGE = 10;
    private static final MiniMessage MM = MiniMessage.miniMessage();
    private static final int PROGRESS_BAR_WIDTH = 20;

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
            return handleList(player, 1, false);
        }

        if (args[0].equalsIgnoreCase("reset")) {
            return handleReset(player);
        }

        if (args[0].equalsIgnoreCase("info") && args.length >= 2) {
            return handleInfo(player, args[1]);
        }

        if (args[0].equalsIgnoreCase("all")) {
            return args.length >= 2
                    ? handleList(player, args[1], true)
                    : handleList(player, 1, true);
        }

        if ((args[0].equalsIgnoreCase("page") || args[0].equalsIgnoreCase("list"))
                && args.length >= 2) {
            return handleList(player, args[1], false);
        }

        return handleSelect(player, args[0]);
    }

    private static String buildProgressLine(int unlocked, int total) {
        double ratio = total == 0 ? 0 : (double) unlocked / total;
        int filled = (int) Math.round(ratio * PROGRESS_BAR_WIDTH);
        String bar = "<green>" + "▰".repeat(filled) + "</green>"
                + "<dark_gray>" + "▱".repeat(PROGRESS_BAR_WIDTH - filled) + "</dark_gray>";
        String percent = String.format(Locale.ROOT, "%.1f", ratio * 100);
        return "<gray>" + unlocked + "<dark_gray>/</dark_gray><gray>" + total
                + " <dark_gray>(</dark_gray><yellow>" + percent + "%</yellow><dark_gray>)</dark_gray> "
                + bar;
    }

    private boolean handleList(Player player, String pageStr, boolean includeLocked) {
        int page;
        try {
            page = Integer.parseInt(pageStr);
        } catch (NumberFormatException e) {
            player.sendMessage(plugin.getMessages().get(player, "title.invalid-page"));
            return true;
        }
        return handleList(player, page, includeLocked);
    }

    private boolean handleList(Player player, int page, boolean includeLocked) {
        TitleManager tm = plugin.getTitleManager();
        Set<Integer> unlocked = tm.getUnlockedTitles(player.getUniqueId());
        Optional<Title> active = tm.getActiveTitle(player.getUniqueId());
        List<Integer> titleIds = includeLocked
                ? tm.getRegistry().getAllTitles().keySet().stream().sorted().toList()
                : unlocked.stream().sorted().toList();

        if (titleIds.isEmpty()) {
            player.sendMessage(plugin.getMessages().get(player, "title.no-titles"));
            return true;
        }

        int totalPages = Math.max(1, (int) Math.ceil((double) titleIds.size() / TITLES_PER_PAGE));
        int currentPage = Math.max(1, Math.min(page, totalPages));
        int fromIndex = (currentPage - 1) * TITLES_PER_PAGE;
        int toIndex = Math.min(fromIndex + TITLES_PER_PAGE, titleIds.size());

        player.sendMessage(plugin.getMessages().get(player, "title.list-header"));
        player.sendMessage(MM.deserialize(buildProgressLine(
                unlocked.size(), tm.getRegistry().getAllTitles().size())));
        player.sendMessage(plugin.getMessages().get(player, "title.page",
                "{page}", String.valueOf(currentPage),
                "{pages}", String.valueOf(totalPages)));

        int activeId = active.map(Title::id).orElse(-1);
        for (int id : titleIds.subList(fromIndex, toIndex)) {
            Optional<Title> title = tm.getRegistry().getTitle(id);
            if (title.isEmpty()) {
                continue;
            }
            Title t = title.get();
            boolean isUnlocked = unlocked.contains(id);
            String marker = isUnlocked && id == activeId ? " <green>✔</green>" : "";
            String description = t.localizedDescription(player, plugin.getMessages());
            String desc = description.isEmpty() ? ""
                    : " <dark_gray>-</dark_gray> <gray>" + description + "</gray>";
            String progress = includeLocked && !isUnlocked ? buildTitleProgress(player, t) : "";
            String rendered = isUnlocked
                    ? TitleRenderer.render(t.localizedName(player, plugin.getMessages()),
                            t.color(), plugin.getTitleGradientShift())
                    : "<dark_gray>?????</dark_gray>";
            Component line = MM.deserialize(
                    rendered + " <gray>(ID: " + t.id() + ")</gray>"
                            + marker + desc + progress);
            player.sendMessage(line);
        }
        if (totalPages > 1) {
            String hintKey = includeLocked ? "title.page-hint-all" : "title.page-hint";
            player.sendMessage(plugin.getMessages().get(player, hintKey,
                    "{prev}", String.valueOf(Math.max(1, currentPage - 1)),
                    "{next}", String.valueOf(Math.min(totalPages, currentPage + 1))));
        }
        return true;
    }

    private String buildTitleProgress(Player player, Title title) {
        if (title.unlockCondition() == null) {
            return "";
        }
        int current = Math.max(0, title.unlockCondition().getProgress(
                player, plugin.getStatsStorage()));
        int target = title.unlockCondition().value();
        if (target <= 0) {
            return "";
        }
        int capped = Math.min(current, target);
        return " <dark_gray>(</dark_gray><yellow>" + capped
                + "<dark_gray>/</dark_gray><yellow>" + target
                + "</yellow><dark_gray>)</dark_gray>";
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
            String name = title.map(t -> t.localizedName(player, plugin.getMessages()))
                    .orElse(String.valueOf(id));
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
        String nameRendered = TitleRenderer.render(t.localizedName(player, plugin.getMessages()),
                t.color(), plugin.getTitleGradientShift());
        player.sendMessage(MM.deserialize(nameRendered));
        player.sendMessage("§7ID: " + t.id());
        String description = t.localizedDescription(player, plugin.getMessages());
        if (!description.isEmpty()) {
            player.sendMessage("§7" + description);
        }
        player.sendMessage("§7" + plugin.getMessages().get(player, "title.status") + ": " + status);
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
        String titleName = sender instanceof Player player
                ? title.get().localizedName(player, plugin.getMessages())
                : title.get().localizedName(target, plugin.getMessages());
        sender.sendMessage(plugin.getMessages().get(sender, "title.give-success",
                "{player}", target.getName(),
                "{title}", titleName));
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
            for (String sub : List.of("reset", "info", "page", "list", "all")) {
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
            if (args[0].equalsIgnoreCase("page") || args[0].equalsIgnoreCase("list")) {
                return completePageNumbers(sender, args[1], false);
            }
            if (args[0].equalsIgnoreCase("all")) {
                return completePageNumbers(sender, args[1], true);
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

    private List<String> completePageNumbers(CommandSender sender, String prefix,
                                             boolean includeLocked) {
        if (!(sender instanceof Player player)) {
            return List.of();
        }
        int total = includeLocked
                ? plugin.getTitleManager().getRegistry().getAllTitles().size()
                : plugin.getTitleManager().getUnlockedTitles(player.getUniqueId()).size();
        int pages = Math.max(1, (int) Math.ceil((double) total / TITLES_PER_PAGE));
        List<String> numbers = new ArrayList<>();
        for (int page = 1; page <= pages; page++) {
            String pageStr = String.valueOf(page);
            if (pageStr.startsWith(prefix)) {
                numbers.add(pageStr);
            }
        }
        return numbers;
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
