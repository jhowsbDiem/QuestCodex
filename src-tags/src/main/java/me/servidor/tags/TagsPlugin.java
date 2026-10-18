package me.servidor.tags;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class TagsPlugin extends JavaPlugin implements Listener, CommandExecutor, TabCompleter {

    @Override
    public void onEnable() {
        saveDefaultConfig();
        Bukkit.getPluginManager().registerEvents(this, this);

        var cmd = getCommand("tag");
        if (cmd != null) {
            cmd.setExecutor(this);
            cmd.setTabCompleter(this);
        }

        for (Player player : Bukkit.getOnlinePlayers()) {
            applyPlayerTag(player);
        }

        getLogger().info("TagsPlugin ativado com sucesso!");
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        applyPlayerTag(event.getPlayer());
    }

    @EventHandler
    public void onChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        TagInfo info = getPlayerTagInfo(player.getName());

        Component prefixComp = Component.text(info.prefix, info.color);
        if (info.bold) {
            prefixComp = prefixComp.decorate(TextDecoration.BOLD);
        }

        Component nameComp = Component.text(player.getName(), info.color)
                .decoration(TextDecoration.BOLD, false);

        Component separator = Component.text(": ", NamedTextColor.GRAY)
                .decoration(TextDecoration.BOLD, false);

        Component finalPrefix = prefixComp.append(nameComp).append(separator);

        event.renderer((source, sourceDisplayName, message, viewer) ->
                finalPrefix.append(message.colorIfAbsent(NamedTextColor.WHITE).decoration(TextDecoration.BOLD, false))
        );
    }

    public void applyPlayerTag(Player player) {
        TagInfo info = getPlayerTagInfo(player.getName());
        Scoreboard sb = Bukkit.getScoreboardManager().getMainScoreboard();

        String teamName = "t_" + info.order + "_" + info.id;
        if (teamName.length() > 16) {
            teamName = teamName.substring(0, 16);
        }

        for (Team t : sb.getTeams()) {
            if (t.getName().startsWith("t_") && t.hasEntry(player.getName())) {
                t.removeEntry(player.getName());
            }
        }

        Team team = sb.getTeam(teamName);
        if (team == null) {
            team = sb.registerNewTeam(teamName);
        }

        Component prefixComp = Component.text(info.prefix, info.color);
        if (info.bold) {
            prefixComp = prefixComp.decorate(TextDecoration.BOLD);
        }

        team.prefix(prefixComp);
        team.color(info.color);
        team.addEntry(player.getName());

        Component tabName = prefixComp.append(
                Component.text(player.getName(), info.color).decoration(TextDecoration.BOLD, false)
        );
        player.playerListName(tabName);
        player.displayName(tabName);
    }

    private Set<String> getAllTags() {
        ConfigurationSection sec = getConfig().getConfigurationSection("tags");
        if (sec == null) return Collections.emptySet();
        return sec.getKeys(false);
    }

    private List<String> getAvailableTags(Player player) {
        Set<String> allTags = getAllTags();
        if (player.isOp() || player.hasPermission("tags.all") || player.hasPermission("tags.*")) {
            return new ArrayList<>(allTags);
        }

        List<String> unlocked = getConfig().getStringList("tags-liberadas." + player.getName().toLowerCase());
        if (unlocked.contains("*")) {
            return new ArrayList<>(allTags);
        }

        Set<String> result = new LinkedHashSet<>();
        String defaultTag = getConfig().getString("tag-padrao", "membro");
        if (allTags.contains(defaultTag)) {
            result.add(defaultTag);
        }

        for (String tagId : allTags) {
            if (unlocked.contains(tagId.toLowerCase()) || player.hasPermission("tags.tag." + tagId.toLowerCase())) {
                result.add(tagId);
            }
        }

        String current = getConfig().getString("jogadores." + player.getName().toLowerCase());
        if (current != null && allTags.contains(current)) {
            result.add(current);
        }

        return new ArrayList<>(result);
    }

    private TagInfo getTagInfoById(String tagId) {
        String defaultTag = getConfig().getString("tag-padrao", "membro");
        ConfigurationSection tagSec = getConfig().getConfigurationSection("tags." + tagId);
        if (tagSec == null) {
            tagSec = getConfig().getConfigurationSection("tags." + defaultTag);
            tagId = defaultTag;
        }

        if (tagSec == null) {
            return new TagInfo("membro", "MEMBRO ", NamedTextColor.GRAY, true, "99");
        }

        String rawPrefix = tagSec.getString("prefixo", tagId.toUpperCase() + " ");
        String cleanPrefix = rawPrefix.replace("[", "").replace("]", "").trim();
        if (!cleanPrefix.isEmpty()) {
            cleanPrefix = cleanPrefix + " ";
        }

        String colorName = tagSec.getString("cor", "GRAY").toUpperCase();
        boolean bold = tagSec.getBoolean("negrito", true);
        String order = tagSec.getString("ordem", "50");

        NamedTextColor color = NamedTextColor.NAMES.value(colorName.toLowerCase());
        if (color == null) {
            color = NamedTextColor.GRAY;
        }

        return new TagInfo(tagId, cleanPrefix, color, bold, order);
    }

    private TagInfo getPlayerTagInfo(String playerName) {
        String defaultTag = getConfig().getString("tag-padrao", "membro");
        String tagId = getConfig().getString("jogadores." + playerName.toLowerCase(), defaultTag);
        return getTagInfoById(tagId);
    }

    private void showInteractiveTagsMenu(Player player) {
        List<String> available = getAvailableTags(player);
        TagInfo current = getPlayerTagInfo(player.getName());

        player.sendMessage(Component.empty());
        player.sendMessage(
                Component.text("Suas tags disponíveis ", NamedTextColor.YELLOW)
                        .append(Component.text("(Clique para selecionar):", NamedTextColor.GRAY))
        );

        Component listLine = Component.empty();
        for (int i = 0; i < available.size(); i++) {
            String tagId = available.get(i);
            TagInfo info = getTagInfoById(tagId);
            boolean isCurrent = info.id.equalsIgnoreCase(current.id);

            String displayTagName = info.prefix.trim();
            Component tagButton = Component.text(displayTagName, info.color)
                    .decorate(TextDecoration.BOLD);

            if (isCurrent) {
                tagButton = tagButton.decorate(TextDecoration.UNDERLINED);
            }

            Component hoverText = Component.text("Clique para equipar a tag ", NamedTextColor.GRAY)
                    .append(Component.text(displayTagName, info.color).decorate(TextDecoration.BOLD))
                    .append(Component.newline())
                    .append(Component.text("Prévia: ", NamedTextColor.DARK_GRAY))
                    .append(Component.text(info.prefix, info.color).decorate(TextDecoration.BOLD))
                    .append(Component.text(player.getName(), info.color).decoration(TextDecoration.BOLD, false));

            if (isCurrent) {
                hoverText = hoverText.append(Component.newline())
                        .append(Component.text("✔ Você já está usando esta tag!", NamedTextColor.GREEN));
            }

            tagButton = tagButton
                    .hoverEvent(HoverEvent.showText(hoverText))
                    .clickEvent(ClickEvent.runCommand("/tag " + info.id));

            listLine = listLine.append(tagButton);

            if (i < available.size() - 1) {
                listLine = listLine.append(
                        Component.text(" | ", NamedTextColor.DARK_GRAY)
                                .decoration(TextDecoration.BOLD, false)
                                .decoration(TextDecoration.UNDERLINED, false)
                );
            }
        }

        player.sendMessage(listLine);
        player.sendMessage(Component.text("Dica: Digite /tag help para ver todos os comandos.", NamedTextColor.DARK_GRAY));
        player.sendMessage(Component.empty());
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            if (!(sender instanceof Player player)) {
                sendHelp(sender);
                return true;
            }
            showInteractiveTagsMenu(player);
            return true;
        }

        String sub = args[0].toLowerCase();

        switch (sub) {
            case "help", "ajuda", "?" -> {
                sendHelp(sender);
                return true;
            }
            case "lista", "list" -> {
                ConfigurationSection sec = getConfig().getConfigurationSection("tags");
                if (sec == null || sec.getKeys(false).isEmpty()) {
                    sender.sendMessage(Component.text("Nenhuma tag cadastrada.", NamedTextColor.RED));
                    return true;
                }
                sender.sendMessage(Component.text("=== Todas as Tags do Servidor ===", NamedTextColor.GOLD));
                for (String key : sec.getKeys(false)) {
                    TagInfo info = getTagInfoById(key);
                    sender.sendMessage(
                            Component.text("- " + key + ": ", NamedTextColor.GRAY)
                                    .append(Component.text(info.prefix.trim(), info.color).decorate(TextDecoration.BOLD))
                    );
                }
                return true;
            }
            case "reload" -> {
                if (!sender.isOp() && !sender.hasPermission("tags.admin")) {
                    sender.sendMessage(Component.text("Sem permissão!", NamedTextColor.RED));
                    return true;
                }
                reloadConfig();
                for (Player p : Bukkit.getOnlinePlayers()) {
                    applyPlayerTag(p);
                }
                sender.sendMessage(Component.text("Configuração de tags recarregada com sucesso!", NamedTextColor.GREEN));
                return true;
            }
            case "criar" -> {
                if (!sender.isOp() && !sender.hasPermission("tags.admin")) {
                    sender.sendMessage(Component.text("Sem permissão!", NamedTextColor.RED));
                    return true;
                }
                if (args.length < 4) {
                    sender.sendMessage(Component.text("Uso: /tag criar <id> <COR> <NomeDaTag>", NamedTextColor.RED));
                    sender.sendMessage(Component.text("Exemplo: /tag criar builder GREEN BUILDER", NamedTextColor.GRAY));
                    return true;
                }
                String id = args[1].toLowerCase();
                String cor = args[2].toUpperCase();
                NamedTextColor c = NamedTextColor.NAMES.value(cor.toLowerCase());
                if (c == null) {
                    sender.sendMessage(Component.text("Cor inválida! Use: RED, DARK_RED, GOLD, YELLOW, GREEN, DARK_GREEN, AQUA, DARK_AQUA, BLUE, LIGHT_PURPLE, DARK_PURPLE, WHITE, GRAY", NamedTextColor.RED));
                    return true;
                }
                StringBuilder sb = new StringBuilder();
                for (int i = 3; i < args.length; i++) {
                    sb.append(args[i]).append(" ");
                }
                String prefixo = sb.toString().replace("[", "").replace("]", "").trim() + " ";

                getConfig().set("tags." + id + ".prefixo", prefixo);
                getConfig().set("tags." + id + ".cor", cor);
                getConfig().set("tags." + id + ".negrito", true);
                getConfig().set("tags." + id + ".ordem", "10");
                saveConfig();

                sender.sendMessage(
                        Component.text("Tag criada com sucesso: ", NamedTextColor.GREEN)
                                .append(Component.text(prefixo.trim(), c).decorate(TextDecoration.BOLD))
                );
                return true;
            }
            case "add", "dar", "liberar" -> {
                if (!sender.isOp() && !sender.hasPermission("tags.admin")) {
                    sender.sendMessage(Component.text("Sem permissão!", NamedTextColor.RED));
                    return true;
                }
                if (args.length < 3) {
                    sender.sendMessage(Component.text("Uso: /tag add <jogador> <tag|*>", NamedTextColor.RED));
                    return true;
                }
                String targetName = args[1];
                String tagId = args[2].toLowerCase();
                if (!tagId.equals("*") && !getConfig().contains("tags." + tagId)) {
                    sender.sendMessage(Component.text("Essa tag não existe! Use /tag lista para ver as opções.", NamedTextColor.RED));
                    return true;
                }
                String path = "tags-liberadas." + targetName.toLowerCase();
                List<String> list = new ArrayList<>(getConfig().getStringList(path));
                if (!list.contains(tagId)) {
                    list.add(tagId);
                    getConfig().set(path, list);
                    saveConfig();
                }
                sender.sendMessage(Component.text("Tag '" + tagId.toUpperCase() + "' adicionada para " + targetName + "!", NamedTextColor.GREEN));
                return true;
            }
            case "remover", "remove", "del" -> {
                if (!sender.isOp() && !sender.hasPermission("tags.admin")) {
                    sender.sendMessage(Component.text("Sem permissão!", NamedTextColor.RED));
                    return true;
                }
                if (args.length < 2) {
                    sender.sendMessage(Component.text("Uso: /tag remover <jogador> [tag]", NamedTextColor.RED));
                    return true;
                }
                String targetName = args[1];
                if (args.length >= 3) {
                    String tagToRemove = args[2].toLowerCase();
                    String path = "tags-liberadas." + targetName.toLowerCase();
                    List<String> list = new ArrayList<>(getConfig().getStringList(path));
                    list.remove(tagToRemove);
                    getConfig().set(path, list);
                    saveConfig();
                    sender.sendMessage(Component.text("Tag '" + tagToRemove.toUpperCase() + "' removida de " + targetName + ".", NamedTextColor.YELLOW));
                } else {
                    getConfig().set("jogadores." + targetName.toLowerCase(), null);
                    saveConfig();
                    Player online = Bukkit.getPlayerExact(targetName);
                    if (online != null) {
                        applyPlayerTag(online);
                    }
                    sender.sendMessage(Component.text("Tag ativa de " + targetName + " resetada para Membro.", NamedTextColor.YELLOW));
                }
                return true;
            }
            case "set" -> {
                if (!sender.isOp() && !sender.hasPermission("tags.admin")) {
                    sender.sendMessage(Component.text("Sem permissão!", NamedTextColor.RED));
                    return true;
                }
                if (args.length < 3) {
                    sender.sendMessage(Component.text("Uso: /tag set <jogador> <tag>", NamedTextColor.RED));
                    return true;
                }
                String targetName = args[1];
                String tagId = args[2].toLowerCase();
                if (!getConfig().contains("tags." + tagId)) {
                    sender.sendMessage(Component.text("A tag '" + tagId + "' não existe!", NamedTextColor.RED));
                    return true;
                }
                getConfig().set("jogadores." + targetName.toLowerCase(), tagId);
                String path = "tags-liberadas." + targetName.toLowerCase();
                List<String> list = new ArrayList<>(getConfig().getStringList(path));
                if (!list.contains(tagId) && !list.contains("*")) {
                    list.add(tagId);
                    getConfig().set(path, list);
                }
                saveConfig();

                Player online = Bukkit.getPlayerExact(targetName);
                if (online != null) {
                    applyPlayerTag(online);
                }
                sender.sendMessage(Component.text("Tag de " + targetName + " definida para " + tagId.toUpperCase() + "!", NamedTextColor.GREEN));
                return true;
            }
        }

        if (args.length == 1 && sender instanceof Player player) {
            String requestedTag = sub;
            if (!getConfig().contains("tags." + requestedTag)) {
                player.sendMessage(Component.text("A tag '" + requestedTag + "' não existe! Digite /tag ou /tag help.", NamedTextColor.RED));
                return true;
            }

            List<String> available = getAvailableTags(player);
            if (!available.contains(requestedTag)) {
                player.sendMessage(Component.text("Você não possui a tag " + requestedTag.toUpperCase() + " liberada!", NamedTextColor.RED));
                return true;
            }

            getConfig().set("jogadores." + player.getName().toLowerCase(), requestedTag);
            saveConfig();
            applyPlayerTag(player);

            TagInfo info = getTagInfoById(requestedTag);
            player.sendMessage(
                    Component.text("Você selecionou a tag ", NamedTextColor.GREEN)
                            .append(Component.text(info.prefix.trim(), info.color).decorate(TextDecoration.BOLD))
                            .append(Component.text("!", NamedTextColor.GREEN))
            );
            return true;
        }

        if (args.length == 2 && (sender.isOp() || sender.hasPermission("tags.admin"))) {
            String targetName = args[0];
            String tagId = args[1].toLowerCase();
            if (getConfig().contains("tags." + tagId)) {
                getConfig().set("jogadores." + targetName.toLowerCase(), tagId);
                String path = "tags-liberadas." + targetName.toLowerCase();
                List<String> list = new ArrayList<>(getConfig().getStringList(path));
                if (!list.contains(tagId) && !list.contains("*")) {
                    list.add(tagId);
                    getConfig().set(path, list);
                }
                saveConfig();

                Player online = Bukkit.getPlayerExact(targetName);
                if (online != null) {
                    applyPlayerTag(online);
                }
                sender.sendMessage(Component.text("Tag de " + targetName + " definida para " + tagId.toUpperCase() + "!", NamedTextColor.GREEN));
                return true;
            }
        }

        sendHelp(sender);
        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(Component.empty());
        sender.sendMessage(Component.text("====== Menu de Ajuda: /tag ======", NamedTextColor.GOLD).decorate(TextDecoration.BOLD));
        sender.sendMessage(Component.text("/tag", NamedTextColor.YELLOW).append(Component.text(" - Abre suas tags no chat para escolher clicando", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/tag <nome>", NamedTextColor.YELLOW).append(Component.text(" - Equipa uma tag direto (ex: /tag dono)", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/tag help", NamedTextColor.YELLOW).append(Component.text(" - Mostra esta lista de comandos", NamedTextColor.GRAY)));
        if (sender.isOp() || sender.hasPermission("tags.admin")) {
            sender.sendMessage(Component.text("--- Comandos de Admin (OP) ---", NamedTextColor.RED));
            sender.sendMessage(Component.text("/tag add <nick> <tag|*>", NamedTextColor.AQUA).append(Component.text(" - Libera uma tag para o jogador usar", NamedTextColor.GRAY)));
            sender.sendMessage(Component.text("/tag set <nick> <tag>", NamedTextColor.AQUA).append(Component.text(" - Coloca e libera a tag no jogador na hora", NamedTextColor.GRAY)));
            sender.sendMessage(Component.text("/tag remover <nick> [tag]", NamedTextColor.AQUA).append(Component.text(" - Reseta ou remove uma tag do jogador", NamedTextColor.GRAY)));
            sender.sendMessage(Component.text("/tag criar <id> <COR> <Nome>", NamedTextColor.AQUA).append(Component.text(" - Cria uma nova tag no servidor", NamedTextColor.GRAY)));
            sender.sendMessage(Component.text("/tag lista", NamedTextColor.AQUA).append(Component.text(" - Lista todas as tags cadastradas", NamedTextColor.GRAY)));
            sender.sendMessage(Component.text("/tag reload", NamedTextColor.AQUA).append(Component.text(" - Recarrega o config.yml", NamedTextColor.GRAY)));
        }
        sender.sendMessage(Component.empty());
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            String prefix = args[0].toLowerCase();
            List<String> suggestions = new ArrayList<>();

            if (sender instanceof Player player) {
                suggestions.addAll(getAvailableTags(player));
            } else {
                suggestions.addAll(getAllTags());
            }

            suggestions.add("help");

            if (sender.isOp() || sender.hasPermission("tags.admin")) {
                suggestions.addAll(Arrays.asList("add", "set", "remover", "criar", "lista", "reload"));
            }

            suggestions.removeIf(s -> !s.toLowerCase().startsWith(prefix));
            return suggestions;
        }

        if (args.length == 2 && (sender.isOp() || sender.hasPermission("tags.admin"))) {
            String first = args[0].toLowerCase();
            String prefix = args[1].toLowerCase();

            if (first.equals("add") || first.equals("dar") || first.equals("set") || first.equals("remover")) {
                List<String> players = new ArrayList<>();
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (p.getName().toLowerCase().startsWith(prefix)) {
                        players.add(p.getName());
                    }
                }
                return players;
            }
        }

        if (args.length == 3 && (sender.isOp() || sender.hasPermission("tags.admin"))) {
            String first = args[0].toLowerCase();
            String prefix = args[2].toLowerCase();

            if (first.equals("add") || first.equals("dar") || first.equals("set") || first.equals("remover")) {
                List<String> tags = new ArrayList<>(getAllTags());
                if (first.equals("add") || first.equals("dar")) tags.add("*");
                tags.removeIf(t -> !t.toLowerCase().startsWith(prefix));
                return tags;
            }

            if (first.equals("criar")) {
                String upPrefix = args[2].toUpperCase();
                List<String> colors = new ArrayList<>(Arrays.asList(
                        "RED", "DARK_RED", "GOLD", "YELLOW", "GREEN", "DARK_GREEN",
                        "AQUA", "DARK_AQUA", "BLUE", "LIGHT_PURPLE", "DARK_PURPLE", "WHITE", "GRAY"
                ));
                colors.removeIf(c -> !c.startsWith(upPrefix));
                return colors;
            }
        }

        return Collections.emptyList();
    }

    private record TagInfo(String id, String prefix, NamedTextColor color, boolean bold, String order) {}
}
