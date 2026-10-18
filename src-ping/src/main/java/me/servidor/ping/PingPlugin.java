package me.servidor.ping;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class PingPlugin extends JavaPlugin implements CommandExecutor, TabCompleter {

    @Override
    public void onEnable() {
        var cmd = getCommand("ping");
        if (cmd != null) {
            cmd.setExecutor(this);
            cmd.setTabCompleter(this);
        }
        getLogger().info("PingPlugin ativado com sucesso!");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(Component.text("Use: /ping <jogador>", NamedTextColor.RED));
                return true;
            }
            int ping = player.getPing();
            player.sendMessage(
                Component.text("Seu ping atual: ", NamedTextColor.GRAY)
                    .append(formatPing(ping))
            );
            return true;
        }

        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            sender.sendMessage(Component.text("Jogador '" + args[0] + "' não encontrado ou offline.", NamedTextColor.RED));
            return true;
        }

        int ping = target.getPing();
        sender.sendMessage(
            Component.text("Ping de ", NamedTextColor.GRAY)
                .append(Component.text(target.getName(), NamedTextColor.WHITE))
                .append(Component.text(": ", NamedTextColor.GRAY))
                .append(formatPing(ping))
        );
        return true;
    }

    private Component formatPing(int ping) {
        NamedTextColor color;
        if (ping < 60) {
            color = NamedTextColor.GREEN;
        } else if (ping < 150) {
            color = NamedTextColor.YELLOW;
        } else if (ping < 250) {
            color = NamedTextColor.GOLD;
        } else {
            color = NamedTextColor.RED;
        }
        return Component.text(ping + " ms", color);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            String prefix = args[0].toLowerCase();
            List<String> names = new ArrayList<>();
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.getName().toLowerCase().startsWith(prefix)) {
                    names.add(p.getName());
                }
            }
            return names;
        }
        return Collections.emptyList();
    }
}
