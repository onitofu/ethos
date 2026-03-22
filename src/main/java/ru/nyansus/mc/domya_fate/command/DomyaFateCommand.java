package ru.nyansus.mc.domya_fate.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import ru.nyansus.mc.domya_fate.DomyaFate;

import java.util.List;

public class DomyaFateCommand implements CommandExecutor, TabCompleter {

    private final DomyaFate plugin;

    public DomyaFateCommand(DomyaFate plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("domya.karma.admin")) {
            sender.sendMessage(plugin.getMessages().get(sender, "command.no-permission"));
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage("§6domya-fate §7v" + plugin.getPluginMeta().getVersion());
            sender.sendMessage("§7/domyafate reload");
            return true;
        }

        if (args[0].equalsIgnoreCase("reload")) {
            plugin.reloadAll();
            sender.sendMessage(plugin.getMessages().get(sender, "command.reload-success"));
            return true;
        }

        return false;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command,
                                      String label, String[] args) {
        if (args.length == 1 && sender.hasPermission("domya.karma.admin")) {
            String prefix = args[0].toLowerCase();
            if ("reload".startsWith(prefix)) {
                return List.of("reload");
            }
        }
        return List.of();
    }
}
