package ru.nyansus.mc.ethos.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import ru.nyansus.mc.ethos.Ethos;

import java.util.List;

public class EthosCommand implements CommandExecutor, TabCompleter {

    private final Ethos plugin;

    public EthosCommand(Ethos plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("ethos.admin")) {
            sender.sendMessage(plugin.getMessages().get(sender, "command.no-permission"));
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(plugin.getMessages().get(sender, "command.info",
                    "{version}", plugin.getPluginMeta().getVersion()));
            sender.sendMessage(plugin.getMessages().get(sender, "command.reload-usage"));
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
        if (args.length == 1 && sender.hasPermission("ethos.admin")) {
            String prefix = args[0].toLowerCase();
            if ("reload".startsWith(prefix)) {
                return List.of("reload");
            }
        }
        return List.of();
    }
}
