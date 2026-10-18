package me.servidor.tags;

import io.papermc.paper.event.player.AsyncChatEvent;
import io.papermc.paper.scoreboard.numbers.NumberFormat;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.entity.EntityBreedEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.EntityTameEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.FurnaceExtractEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class TagsPlugin extends JavaPlugin implements Listener, CommandExecutor, TabCompleter {

    private final Set<UUID> hiddenScoreboard = new HashSet<>();
    private final Map<String, QuestDef> quests = new LinkedHashMap<>();
    private final Map<String, Integer> blockBreakProgress = new HashMap<>();
    private final Map<String, Integer> mobKillProgress = new HashMap<>();
    private NamespacedKey codexBookKey;

    private static final String GUI_TITLE_MARKER = "QuestCodex — Conquistas";

    @Override
    public void onEnable() {
        saveDefaultConfig();
        codexBookKey = new NamespacedKey(this, "quest_codex_book");

        if (!getConfig().contains("scoreboard.titulo")) {
            getConfig().set("scoreboard.ativada", true);
            getConfig().set("scoreboard.titulo", "QUEST CODEX");
            getConfig().set("scoreboard.cor-titulo", "GOLD");
            getConfig().set("scoreboard.negrito-titulo", true);
        }
        if (!getConfig().contains("partida.ativa")) {
            getConfig().set("partida.ativa", true);
            getConfig().set("partida.bonus-primeiro-porcentagem", 50);
        }
        saveConfig();

        registerDefaultQuests();

        Bukkit.getPluginManager().registerEvents(this, this);

        for (String cmdName : Arrays.asList("tag", "sb", "codex", "partida", "pontos")) {
            var cmd = getCommand(cmdName);
            if (cmd != null) {
                cmd.setExecutor(this);
                cmd.setTabCompleter(this);
            }
        }

        for (Player player : Bukkit.getOnlinePlayers()) {
            applyPlayerTag(player);
            ensurePlayerHasCodexBook(player);
        }

        Bukkit.getScheduler().runTaskTimer(this, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                updatePlayerScoreboard(player);
            }
        }, 20L, 20L);

        getLogger().info("QuestCodex v2.0 (Tags + Scoreboard + Livro de Conquistas) ativado!");
    }

    private void registerDefaultQuests() {
        quests.clear();

        // LINHA 1 (Slots 10..16): FÁCEIS (Início do Survival) - 10 a 20 pts (+50% First Blood)
        addQuest("madeira", 10, Material.OAK_LOG, "Lenha na Fogueira",
                "Quebre 10 troncos de madeira de qualquer árvore.",
                "Fácil", NamedTextColor.GREEN, 10, 15);
        addQuest("bancada", 11, Material.CRAFTING_TABLE, "Mãos à Obra",
                "Crie uma Bancada de Trabalho (Crafting Table).",
                "Fácil", NamedTextColor.GREEN, 10, 15);
        addQuest("picareta_pedra", 12, Material.STONE_PICKAXE, "Idade da Pedra",
                "Crie uma Picareta de Pedra.",
                "Fácil", NamedTextColor.GREEN, 15, 25);
        addQuest("fornalha", 13, Material.FURNACE, "Fogo Alto",
                "Crie uma Fornalha.",
                "Fácil", NamedTextColor.GREEN, 15, 25);
        addQuest("carvao", 14, Material.COAL, "Combustível Fóssil",
                "Minere um minério de Carvão.",
                "Fácil", NamedTextColor.GREEN, 15, 25);
        addQuest("pao", 15, Material.BREAD, "Padeiro da Vila",
                "Crie ou coma um Pão.",
                "Fácil", NamedTextColor.GREEN, 20, 30);
        addQuest("pescador", 16, Material.FISHING_ROD, "Dia de Pesca",
                "Pesque qualquer peixe ou item usando uma Vara de Pesca.",
                "Fácil", NamedTextColor.GREEN, 20, 30);

        // LINHA 2 (Slots 19..25): MÉDIAS (Ferro e Aventura) - 30 a 50 pts
        addQuest("ferro", 19, Material.IRON_INGOT, "Idade do Ferro",
                "Obtenha uma Barra de Ferro (fornalha ou coleta).",
                "Média", NamedTextColor.YELLOW, 30, 45);
        addQuest("escudo", 20, Material.SHIELD, "Defesa Implacável",
                "Crie um Escudo de proteção.",
                "Média", NamedTextColor.YELLOW, 30, 45);
        addQuest("balde_lava", 21, Material.LAVA_BUCKET, "Quente Demais!",
                "Encha um Balde com Lava.",
                "Média", NamedTextColor.YELLOW, 35, 55);
        addQuest("cacador", 22, Material.IRON_SWORD, "Caçador da Noite",
                "Elimine 10 monstros hostis (Zumbis, Esqueletos, Aranhas, etc).",
                "Média", NamedTextColor.YELLOW, 40, 60);
        addQuest("ouro", 23, Material.GOLD_INGOT, "Febre do Ouro",
                "Obtenha uma Barra de Ouro.",
                "Média", NamedTextColor.YELLOW, 40, 60);
        addQuest("domador", 24, Material.BONE, "Melhor Amigo",
                "Dome um Lobo ou Gato, ou reproduza dois animais.",
                "Média", NamedTextColor.YELLOW, 45, 70);
        addQuest("redstone", 25, Material.REDSTONE, "Engenheiro Elétrico",
                "Minere um bloco de Minério de Redstone.",
                "Média", NamedTextColor.YELLOW, 40, 60);

        // LINHA 3 (Slots 28..34): DIFÍCEIS (Diamante e Nether) - 70 a 120 pts
        addQuest("diamante", 28, Material.DIAMOND, "Brilho Eterno!",
                "Minere ou obtenha o seu primeiro Diamante.",
                "Difícil", NamedTextColor.GOLD, 70, 105);
        addQuest("picareta_diamante", 29, Material.DIAMOND_PICKAXE, "Minerador Mestre",
                "Crie uma Picareta de Diamante.",
                "Difícil", NamedTextColor.GOLD, 80, 120);
        addQuest("obsidiana", 30, Material.OBSIDIAN, "Rocha Indestrutível",
                "Quebre um bloco de Obsidiana.",
                "Difícil", NamedTextColor.GOLD, 80, 120);
        addQuest("encantador", 31, Material.ENCHANTING_TABLE, "Mago Aprendiz",
                "Crie uma Mesa de Encantamentos ou encante um item.",
                "Difícil", NamedTextColor.GOLD, 90, 135);
        addQuest("nether", 32, Material.NETHERRACK, "Profundezas Ardentes",
                "Construa um portal e entre na dimensão do Nether.",
                "Difícil", NamedTextColor.GOLD, 100, 150);
        addQuest("blaze", 33, Material.BLAZE_ROD, "Chama Viva",
                "Derrote um Blaze no Nether e obtenha uma Vara de Blaze.",
                "Difícil", NamedTextColor.GOLD, 110, 165);
        addQuest("maca_dourada", 34, Material.GOLDEN_APPLE, "Fruto Dourado",
                "Crie ou coma uma Maçã Dourada.",
                "Difícil", NamedTextColor.GOLD, 90, 135);

        // LINHA 4 (Slots 37..43): ÉPICAS / LENDÁRIAS (Endgame) - 150 a 500 pts
        addQuest("olho_ender", 37, Material.ENDER_EYE, "Visão do Fim",
                "Crie um Olho de Ender (Olho do Fim).",
                "Épica", NamedTextColor.LIGHT_PURPLE, 130, 195);
        addQuest("ancestral", 38, Material.ANCIENT_DEBRIS, "Relíquia Ancestral",
                "Minere um Detrito Ancestral no Nether.",
                "Épica", NamedTextColor.LIGHT_PURPLE, 180, 270);
        addQuest("netherite", 39, Material.NETHERITE_INGOT, "Forja Suprema",
                "Obtenha uma Barra de Netherite.",
                "Épica", NamedTextColor.LIGHT_PURPLE, 220, 330);
        addQuest("the_end", 40, Material.END_STONE, "Além do Horizonte",
                "Entre na dimensão do The End.",
                "Lendária", NamedTextColor.RED, 250, 375);
        addQuest("matador_dragao", 41, Material.DRAGON_HEAD, "Lenda de QuestCodex",
                "Derrote o Ender Dragon!",
                "Lendária", NamedTextColor.RED, 400, 600);
        addQuest("wither", 42, Material.NETHER_STAR, "Caçador de Chefes",
                "Derrote o Wither e obtenha uma Estrela do Nether.",
                "Lendária", NamedTextColor.RED, 350, 525);
        addQuest("elytra", 43, Material.ELYTRA, "Dominador dos Céus",
                "Encontre e colete uma Elytra na End City.",
                "Lendária", NamedTextColor.RED, 300, 450);
    }

    private void addQuest(String id, int slot, Material icon, String title, String description,
                          String difficulty, NamedTextColor diffColor, int basePoints, int firstPoints) {
        quests.put(id, new QuestDef(id, slot, icon, title, description, difficulty, diffColor, basePoints, firstPoints));
    }

    // =========================================================================
    // LIVRO DO QUEST CODEX E MENU GRÁFICO (INVENTÁRIO)
    // =========================================================================

    public ItemStack createCodexBook() {
        ItemStack item = new ItemStack(Material.KNOWLEDGE_BOOK);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(
                    Component.text("📖 QuestCodex ", NamedTextColor.GOLD).decorate(TextDecoration.BOLD)
                            .append(Component.text("(Clique Direito)", NamedTextColor.YELLOW).decoration(TextDecoration.BOLD, false))
                            .decoration(TextDecoration.ITALIC, false)
            );
            List<Component> lore = new ArrayList<>();
            lore.add(Component.empty());
            lore.add(Component.text("Clique com o botão direito para abrir", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
            lore.add(Component.text("a árvore de conquistas da partida!", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
            lore.add(Component.empty());
            lore.add(Component.text("★ Faça conquistas primeiro para ganhar", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false));
            lore.add(Component.text("  bônus de 1º Lugar (First Blood)!", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false));
            meta.lore(lore);
            meta.addEnchant(Enchantment.UNBREAKING, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            meta.getPersistentDataContainer().set(codexBookKey, PersistentDataType.BYTE, (byte) 1);
            item.setItemMeta(meta);
        }
        return item;
    }

    public boolean isCodexBook(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;
        return meta.getPersistentDataContainer().has(codexBookKey, PersistentDataType.BYTE);
    }

    public void ensurePlayerHasCodexBook(Player player) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (isCodexBook(item)) {
                return;
            }
        }
        player.getInventory().addItem(createCodexBook());
    }

    public void openCodexGui(Player player) {
        Inventory inv = Bukkit.createInventory(
                null,
                54,
                Component.text(GUI_TITLE_MARKER, NamedTextColor.GOLD).decorate(TextDecoration.BOLD)
        );

        // Moldura decorativa com painéis de vidro cinza escuro
        ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta fillerMeta = filler.getItemMeta();
        if (fillerMeta != null) {
            fillerMeta.displayName(Component.text(" "));
            filler.setItemMeta(fillerMeta);
        }
        for (int i = 0; i < 54; i++) {
            if (i < 9 || i >= 45 || i % 9 == 0 || i % 9 == 8) {
                inv.setItem(i, filler);
            }
        }

        String pKey = player.getName().toLowerCase();
        List<String> completedList = getConfig().getStringList("conquistas-jogador." + pKey);
        int playerPts = getPlayerPoints(player.getName());

        // Ícone de Resumo e Ranking no Topo (Slot 4)
        ItemStack infoItem = new ItemStack(Material.NETHER_STAR);
        ItemMeta infoMeta = infoItem.getItemMeta();
        if (infoMeta != null) {
            infoMeta.displayName(
                    Component.text("★ Seu Progresso no QuestCodex ★", NamedTextColor.GOLD)
                            .decorate(TextDecoration.BOLD)
                            .decoration(TextDecoration.ITALIC, false)
            );
            List<Component> infoLore = new ArrayList<>();
            infoLore.add(Component.empty());
            infoLore.add(Component.text("Seus Pontos: ", NamedTextColor.WHITE)
                    .append(Component.text(playerPts + " pts", NamedTextColor.AQUA).decorate(TextDecoration.BOLD))
                    .decoration(TextDecoration.ITALIC, false));
            infoLore.add(Component.text("Conquistas Feitas: ", NamedTextColor.WHITE)
                    .append(Component.text(completedList.size() + "/" + quests.size(), NamedTextColor.GREEN))
                    .decoration(TextDecoration.ITALIC, false));
            infoLore.add(Component.empty());
            infoLore.add(Component.text("🏆 Top 3 Jogadores da Partida:", NamedTextColor.YELLOW)
                    .decorate(TextDecoration.BOLD)
                    .decoration(TextDecoration.ITALIC, false));

            List<Map.Entry<String, Integer>> top = getTopPlayers(3);
            if (top.isEmpty()) {
                infoLore.add(Component.text("  Ninguém pontuou ainda!", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
            } else {
                for (int i = 0; i < top.size(); i++) {
                    var entry = top.get(i);
                    infoLore.add(
                            Component.text("  #" + (i + 1) + " " + entry.getKey() + ": ", NamedTextColor.WHITE)
                                    .append(Component.text(entry.getValue() + " pts", NamedTextColor.GREEN))
                                    .decoration(TextDecoration.ITALIC, false)
                    );
                }
            }
            infoMeta.lore(infoLore);
            infoItem.setItemMeta(infoMeta);
        }
        inv.setItem(4, infoItem);

        // Preenche cada conquista
        for (QuestDef q : quests.values()) {
            boolean doneByPlayer = completedList.contains(q.id);
            String firstPlayer = getConfig().getString("primeiro-conquista." + q.id, null);

            ItemStack icon = new ItemStack(q.icon);
            ItemMeta meta = icon.getItemMeta();
            if (meta != null) {
                NamedTextColor titleColor = doneByPlayer ? NamedTextColor.GREEN : q.diffColor;
                String prefixStatus = doneByPlayer ? "✔ " : "✦ ";
                meta.displayName(
                        Component.text(prefixStatus + q.title, titleColor)
                                .decorate(TextDecoration.BOLD)
                                .decoration(TextDecoration.ITALIC, false)
                );

                List<Component> lore = new ArrayList<>();
                lore.add(Component.text("Dificuldade: ", NamedTextColor.DARK_GRAY)
                        .append(Component.text(q.difficulty, q.diffColor))
                        .decoration(TextDecoration.ITALIC, false));
                lore.add(Component.empty());
                lore.add(Component.text("Objetivo:", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false));
                lore.add(Component.text(" " + q.description, NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false));
                lore.add(Component.empty());
                lore.add(Component.text("Recompensa Normal: ", NamedTextColor.GRAY)
                        .append(Component.text("+" + q.basePoints + " pts", NamedTextColor.GREEN))
                        .decoration(TextDecoration.ITALIC, false));
                lore.add(Component.text("1º a Completar (First Blood): ", NamedTextColor.GRAY)
                        .append(Component.text("+" + q.firstPoints + " pts", NamedTextColor.GOLD).decorate(TextDecoration.BOLD))
                        .decoration(TextDecoration.ITALIC, false));
                lore.add(Component.empty());

                if (firstPlayer != null) {
                    lore.add(Component.text("⚡ Feita 1º por: ", NamedTextColor.GOLD)
                            .append(Component.text(firstPlayer, NamedTextColor.YELLOW).decorate(TextDecoration.BOLD))
                            .decoration(TextDecoration.ITALIC, false));
                } else {
                    lore.add(Component.text("⚡ Ninguém completou ainda! Seja o 1º!", NamedTextColor.AQUA)
                            .decoration(TextDecoration.ITALIC, false));
                }

                if (doneByPlayer) {
                    lore.add(Component.text("✔ CONQUISTA CONCLUÍDA POR VOCÊ!", NamedTextColor.GREEN)
                            .decorate(TextDecoration.BOLD)
                            .decoration(TextDecoration.ITALIC, false));
                    meta.addEnchant(Enchantment.UNBREAKING, 1, true);
                    meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                } else {
                    lore.add(Component.text("⏳ Pendente — cumpra o objetivo no mundo!", NamedTextColor.RED)
                            .decoration(TextDecoration.ITALIC, false));
                }

                meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
                meta.lore(lore);
                icon.setItemMeta(meta);
            }
            inv.setItem(q.slot, icon);
        }

        player.openInventory(inv);
        player.playSound(player.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 1.0f, 1.1f);
    }

    // =========================================================================
    // LÓGICA DE PONTOS E CONCLUSÃO DE CONQUISTAS
    // =========================================================================

    public int getPlayerPoints(String playerName) {
        return getConfig().getInt("pontuacao." + playerName.toLowerCase(), 0);
    }

    public List<Map.Entry<String, Integer>> getTopPlayers(int limit) {
        ConfigurationSection sec = getConfig().getConfigurationSection("pontuacao");
        if (sec == null) return Collections.emptyList();

        List<Map.Entry<String, Integer>> list = new ArrayList<>();
        for (String key : sec.getKeys(false)) {
            list.add(Map.entry(key, sec.getInt(key, 0)));
        }
        list.sort(Map.Entry.comparingByValue(Comparator.reverseOrder()));
        if (list.size() > limit) {
            return list.subList(0, limit);
        }
        return list;
    }

    public void completeQuest(Player player, String questId) {
        if (!getConfig().getBoolean("partida.ativa", true)) {
            return;
        }

        QuestDef q = quests.get(questId);
        if (q == null) return;

        String pKey = player.getName().toLowerCase();
        String playerQuestsPath = "conquistas-jogador." + pKey;
        List<String> done = new ArrayList<>(getConfig().getStringList(playerQuestsPath));
        if (done.contains(questId)) {
            return; // Jogador já completou esta conquista nesta partida
        }

        done.add(questId);
        getConfig().set(playerQuestsPath, done);

        String firstPath = "primeiro-conquista." + questId;
        String currentFirst = getConfig().getString(firstPath, null);
        boolean isFirst = (currentFirst == null || currentFirst.isEmpty());

        int pointsEarned = isFirst ? q.firstPoints : q.basePoints;
        if (isFirst) {
            getConfig().set(firstPath, player.getName());
        }

        int newTotal = getPlayerPoints(player.getName()) + pointsEarned;
        getConfig().set("pontuacao." + pKey, newTotal);
        saveConfig();

        // Efeitos sonoros e mensagem global/individual
        player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.8f, 1.2f);

        if (isFirst) {
            Component broadcastMsg = Component.empty()
                    .append(Component.text("🏆 [1º LUGAR] ", NamedTextColor.GOLD).decorate(TextDecoration.BOLD))
                    .append(Component.text(player.getName(), NamedTextColor.YELLOW).decorate(TextDecoration.BOLD))
                    .append(Component.text(" foi o PRIMEIRO a concluir ", NamedTextColor.WHITE).decoration(TextDecoration.BOLD, false))
                    .append(Component.text("[" + q.title + "]", q.diffColor).decorate(TextDecoration.BOLD))
                    .append(Component.text(" e ganhou ", NamedTextColor.WHITE).decoration(TextDecoration.BOLD, false))
                    .append(Component.text("+" + pointsEarned + " pts", NamedTextColor.GREEN).decorate(TextDecoration.BOLD))
                    .append(Component.text("!", NamedTextColor.WHITE).decoration(TextDecoration.BOLD, false));
            Bukkit.broadcast(broadcastMsg);
        } else {
            player.sendMessage(
                    Component.text("✔ Conquista concluída: ", NamedTextColor.GREEN)
                            .append(Component.text(q.title, q.diffColor).decorate(TextDecoration.BOLD))
                            .append(Component.text(" (+" + pointsEarned + " pts)", NamedTextColor.AQUA))
            );
        }

        // Atualiza a Scoreboard de todo mundo imediatamente
        for (Player p : Bukkit.getOnlinePlayers()) {
            updatePlayerScoreboard(p);
        }
    }

    // =========================================================================
    // EVENTOS DO JOGO PARA DETECTAR CONQUISTAS AUTOMATICAMENTE
    // =========================================================================

    @EventHandler
    public void onInteractBook(PlayerInteractEvent event) {
        if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            ItemStack item = event.getItem();
            if (isCodexBook(item)) {
                event.setCancelled(true);
                openCodexGui(event.getPlayer());
            }
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        String plainTitle = event.getView().getOriginalTitle();
        if (plainTitle != null && plainTitle.contains("QuestCodex")) {
            event.setCancelled(true);
            return;
        }

        // Verifica se clicou em algum item dentro do inventário que destrava conquista
        if (event.getWhoClicked() instanceof Player player && event.getCurrentItem() != null) {
            checkItemAcquisition(player, event.getCurrentItem().getType());
        }
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        Bukkit.getScheduler().runTaskLater(this, () -> ensurePlayerHasCodexBook(event.getPlayer()), 10L);
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();
        Material type = block.getType();
        String name = type.name();

        if (name.endsWith("_LOG") || name.endsWith("_STEM")) {
            String key = player.getName().toLowerCase();
            int count = blockBreakProgress.getOrDefault(key, 0) + 1;
            blockBreakProgress.put(key, count);
            if (count >= 10) {
                completeQuest(player, "madeira");
            }
        } else if (type == Material.COAL_ORE || type == Material.DEEPSLATE_COAL_ORE) {
            completeQuest(player, "carvao");
        } else if (type == Material.REDSTONE_ORE || type == Material.DEEPSLATE_REDSTONE_ORE) {
            completeQuest(player, "redstone");
        } else if (type == Material.DIAMOND_ORE || type == Material.DEEPSLATE_DIAMOND_ORE) {
            completeQuest(player, "diamante");
        } else if (type == Material.OBSIDIAN || type == Material.CRYING_OBSIDIAN) {
            completeQuest(player, "obsidiana");
        } else if (type == Material.ANCIENT_DEBRIS) {
            completeQuest(player, "ancestral");
        }
    }

    @EventHandler
    public void onCraftItem(CraftItemEvent event) {
        if (event.getWhoClicked() instanceof Player player) {
            Material result = event.getRecipe().getResult().getType();
            checkItemAcquisition(player, result);
        }
    }

    @EventHandler
    public void onFurnaceExtract(FurnaceExtractEvent event) {
        checkItemAcquisition(event.getPlayer(), event.getItemType());
    }

    @EventHandler
    public void onPickupItem(EntityPickupItemEvent event) {
        if (event.getEntity() instanceof Player player) {
            checkItemAcquisition(player, event.getItem().getItemStack().getType());
        }
    }

    @EventHandler
    public void onPlayerConsume(PlayerItemConsumeEvent event) {
        Material type = event.getItem().getType();
        if (type == Material.BREAD) {
            completeQuest(event.getPlayer(), "pao");
        } else if (type == Material.GOLDEN_APPLE || type == Material.ENCHANTED_GOLDEN_APPLE) {
            completeQuest(event.getPlayer(), "maca_dourada");
        }
    }

    @EventHandler
    public void onPlayerFish(PlayerFishEvent event) {
        if (event.getState() == PlayerFishEvent.State.CAUGHT_FISH) {
            completeQuest(event.getPlayer(), "pescador");
        }
    }

    @EventHandler
    public void onEnchant(EnchantItemEvent event) {
        completeQuest(event.getEnchanter(), "encantador");
    }

    @EventHandler
    public void onEntityTame(EntityTameEvent event) {
        if (event.getOwner() instanceof Player player) {
            completeQuest(player, "domador");
        }
    }

    @EventHandler
    public void onEntityBreed(EntityBreedEvent event) {
        if (event.getBreeder() instanceof Player player) {
            completeQuest(player, "domador");
        }
    }

    @EventHandler
    public void onBucketFill(PlayerInteractEvent event) {
        if (event.getItem() != null && event.getItem().getType() == Material.BUCKET) {
            Bukkit.getScheduler().runTaskLater(this, () -> {
                for (ItemStack item : event.getPlayer().getInventory().getContents()) {
                    if (item != null && item.getType() == Material.LAVA_BUCKET) {
                        completeQuest(event.getPlayer(), "balde_lava");
                        break;
                    }
                }
            }, 3L);
        }
    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();
        World.Environment env = player.getWorld().getEnvironment();
        if (env == World.Environment.NETHER) {
            completeQuest(player, "nether");
        } else if (env == World.Environment.THE_END) {
            completeQuest(player, "the_end");
        }
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();
        Player killer = entity.getKiller();
        if (killer == null) return;

        if (entity.getType() == EntityType.ENDER_DRAGON) {
            completeQuest(killer, "matador_dragao");
        } else if (entity.getType() == EntityType.WITHER) {
            completeQuest(killer, "wither");
        } else if (entity.getType() == EntityType.BLAZE) {
            completeQuest(killer, "blaze");
        }

        if (entity instanceof Monster) {
            String key = killer.getName().toLowerCase();
            int kills = mobKillProgress.getOrDefault(key, 0) + 1;
            mobKillProgress.put(key, kills);
            if (kills >= 10) {
                completeQuest(killer, "cacador");
            }
        }
    }

    private void checkItemAcquisition(Player player, Material type) {
        switch (type) {
            case CRAFTING_TABLE -> completeQuest(player, "bancada");
            case STONE_PICKAXE -> completeQuest(player, "picareta_pedra");
            case FURNACE -> completeQuest(player, "fornalha");
            case COAL -> completeQuest(player, "carvao");
            case BREAD -> completeQuest(player, "pao");
            case IRON_INGOT -> completeQuest(player, "ferro");
            case SHIELD -> completeQuest(player, "escudo");
            case LAVA_BUCKET -> completeQuest(player, "balde_lava");
            case GOLD_INGOT -> completeQuest(player, "ouro");
            case REDSTONE -> completeQuest(player, "redstone");
            case DIAMOND -> completeQuest(player, "diamante");
            case DIAMOND_PICKAXE -> completeQuest(player, "picareta_diamante");
            case OBSIDIAN -> completeQuest(player, "obsidiana");
            case ENCHANTING_TABLE -> completeQuest(player, "encantador");
            case BLAZE_ROD -> completeQuest(player, "blaze");
            case GOLDEN_APPLE, ENCHANTED_GOLDEN_APPLE -> completeQuest(player, "maca_dourada");
            case ENDER_EYE -> completeQuest(player, "olho_ender");
            case ANCIENT_DEBRIS -> completeQuest(player, "ancestral");
            case NETHERITE_INGOT -> completeQuest(player, "netherite");
            case NETHER_STAR -> completeQuest(player, "wither");
            case ELYTRA -> completeQuest(player, "elytra");
            default -> {}
        }
    }

    // =========================================================================
    // SISTEMA DE TAGS E SCOREBOARD (INTEGRADO COM PONTOS)
    // =========================================================================

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        applyPlayerTag(player);
        ensurePlayerHasCodexBook(player);
        Bukkit.getScheduler().runTaskLater(this, () -> {
            for (Player p : Bukkit.getOnlinePlayers()) {
                updatePlayerScoreboard(p);
            }
        }, 5L);
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
        updatePlayerTab(player);
        for (Player online : Bukkit.getOnlinePlayers()) {
            updatePlayerScoreboard(online);
        }
    }

    public void updatePlayerTab(Player player) {
        TagInfo info = getPlayerTagInfo(player.getName());
        int pts = getPlayerPoints(player.getName());
        int ping = player.getPing();

        Component prefixComp = Component.text(info.prefix, info.color);
        if (info.bold) {
            prefixComp = prefixComp.decorate(TextDecoration.BOLD);
        }

        // Nome do jogador no TAB: TAG Nick ✦ 150 pts
        Component tabName = Component.text(" ", NamedTextColor.DARK_GRAY)
                .append(prefixComp)
                .append(Component.text(player.getName(), info.color).decoration(TextDecoration.BOLD, false))
                .append(Component.text(" ✦ ", NamedTextColor.DARK_GRAY).decoration(TextDecoration.BOLD, false))
                .append(Component.text(pts + " pts ", NamedTextColor.AQUA).decorate(TextDecoration.BOLD));

        player.playerListName(tabName);
        player.displayName(
                prefixComp.append(Component.text(player.getName(), info.color).decoration(TextDecoration.BOLD, false))
        );

        // Cabeçalho (Header) e Rodapé (Footer) estilizados do TAB
        int onlineCount = Bukkit.getOnlinePlayers().size();
        int completedCount = getConfig().getStringList("conquistas-jogador." + player.getName().toLowerCase()).size();
        List<Map.Entry<String, Integer>> top1 = getTopPlayers(1);
        String leaderText = top1.isEmpty() ? "Ninguém (0 pts)" : top1.get(0).getKey() + " (" + top1.get(0).getValue() + " pts)";

        NamedTextColor pingColor;
        if (ping < 60) pingColor = NamedTextColor.GREEN;
        else if (ping < 150) pingColor = NamedTextColor.YELLOW;
        else if (ping < 250) pingColor = NamedTextColor.GOLD;
        else pingColor = NamedTextColor.RED;

        Component header = Component.empty()
                .append(Component.newline())
                .append(Component.text("§8§m⎯⎯⎯⎯⎯⎯⎯⎯⎯§r ", NamedTextColor.DARK_GRAY))
                .append(Component.text("⚔ ", NamedTextColor.YELLOW))
                .append(Component.text("QUEST CODEX", NamedTextColor.GOLD).decorate(TextDecoration.BOLD))
                .append(Component.text(" ⚔", NamedTextColor.YELLOW))
                .append(Component.text(" §8§m⎯⎯⎯⎯⎯⎯⎯⎯⎯", NamedTextColor.DARK_GRAY))
                .append(Component.newline())
                .append(Component.text("Survival Competitivo por Conquistas", NamedTextColor.GRAY))
                .append(Component.newline())
                .append(Component.newline())
                .append(Component.text(" 🏆 Líder da Partida: ", NamedTextColor.YELLOW))
                .append(Component.text(leaderText, NamedTextColor.GOLD).decorate(TextDecoration.BOLD))
                .append(Component.text(" ", NamedTextColor.WHITE))
                .append(Component.newline());

        Component footer = Component.empty()
                .append(Component.newline())
                .append(Component.text("§8§m⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯", NamedTextColor.DARK_GRAY))
                .append(Component.newline());

        player.sendPlayerListHeaderAndFooter(header, footer);
    }

    public void updatePlayerScoreboard(Player viewer) {
        updatePlayerTab(viewer);
        Scoreboard mainSb = Bukkit.getScoreboardManager().getMainScoreboard();
        Scoreboard sb = viewer.getScoreboard();
        if (sb == null || sb == mainSb) {
            sb = Bukkit.getScoreboardManager().getNewScoreboard();
            viewer.setScoreboard(sb);
        }

        for (Player target : Bukkit.getOnlinePlayers()) {
            TagInfo tInfo = getPlayerTagInfo(target.getName());
            int targetPts = getPlayerPoints(target.getName());
            String teamName = "t_" + tInfo.order + "_" + tInfo.id;
            if (teamName.length() > 16) {
                teamName = teamName.substring(0, 16);
            }

            for (Team existing : sb.getTeams()) {
                if (existing.getName().startsWith("t_") && !existing.getName().equals(teamName) && existing.hasEntry(target.getName())) {
                    existing.removeEntry(target.getName());
                }
            }

            Team team = sb.getTeam(teamName);
            if (team == null) {
                team = sb.registerNewTeam(teamName);
            }

            Component prefixComp = Component.text(tInfo.prefix, tInfo.color);
            if (tInfo.bold) {
                prefixComp = prefixComp.decorate(TextDecoration.BOLD);
            }

            team.prefix(prefixComp);
            team.color(tInfo.color);
            if (!team.hasEntry(target.getName())) {
                team.addEntry(target.getName());
            }
        }

        boolean enabledGlobal = getConfig().getBoolean("scoreboard.ativada", true);
        if (!enabledGlobal || hiddenScoreboard.contains(viewer.getUniqueId())) {
            Objective old = sb.getObjective(DisplaySlot.SIDEBAR);
            if (old != null) {
                old.unregister();
            }
            return;
        }

        String titleText = getConfig().getString("scoreboard.titulo", "QUEST CODEX");
        String titleColorName = getConfig().getString("scoreboard.cor-titulo", "GOLD");
        boolean titleBold = getConfig().getBoolean("scoreboard.negrito-titulo", true);

        NamedTextColor titleColor = NamedTextColor.NAMES.value(titleColorName.toLowerCase());
        if (titleColor == null) titleColor = NamedTextColor.GOLD;

        Component titleComp = Component.text("  " + titleText + "  ", titleColor);
        if (titleBold) {
            titleComp = titleComp.decorate(TextDecoration.BOLD);
        }

        Objective obj = sb.getObjective("qc_sidebar");
        if (obj == null) {
            obj = sb.registerNewObjective("qc_sidebar", Criteria.DUMMY, titleComp);
            obj.setDisplaySlot(DisplaySlot.SIDEBAR);
        } else {
            obj.displayName(titleComp);
        }

        obj.numberFormat(NumberFormat.blank());

        TagInfo viewerTag = getPlayerTagInfo(viewer.getName());
        String tagDisplay = viewerTag.prefix.trim();
        int ping = viewer.getPing();
        int onlineCount = Bukkit.getOnlinePlayers().size();
        int pts = getPlayerPoints(viewer.getName());
        int completedCount = getConfig().getStringList("conquistas-jogador." + viewer.getName().toLowerCase()).size();

        NamedTextColor pingColor;
        if (ping < 60) pingColor = NamedTextColor.GREEN;
        else if (ping < 150) pingColor = NamedTextColor.YELLOW;
        else if (ping < 250) pingColor = NamedTextColor.GOLD;
        else pingColor = NamedTextColor.RED;

        List<Map.Entry<String, Integer>> top1 = getTopPlayers(1);
        String leaderText = top1.isEmpty() ? "Ninguém" : top1.get(0).getKey() + " (" + top1.get(0).getValue() + ")";

        // Linhas da Scoreboard (de 9 até 1)
        setSidebarLine(obj, "line9", 9, Component.empty());

        setSidebarLine(obj, "line8", 8,
                Component.text("Cargo: ", NamedTextColor.WHITE)
                        .append(Component.text(tagDisplay, viewerTag.color).decorate(TextDecoration.BOLD))
        );

        setSidebarLine(obj, "line7", 7,
                Component.text("Pontos: ", NamedTextColor.WHITE)
                        .append(Component.text(String.valueOf(pts), NamedTextColor.AQUA).decorate(TextDecoration.BOLD))
        );

        setSidebarLine(obj, "line6", 6,
                Component.text("Quests: ", NamedTextColor.WHITE)
                        .append(Component.text(completedCount + "/" + quests.size(), NamedTextColor.YELLOW))
        );

        setSidebarLine(obj, "line5", 5,
                Component.text("Líder: ", NamedTextColor.WHITE)
                        .append(Component.text(leaderText, NamedTextColor.GOLD))
        );

        setSidebarLine(obj, "line4", 4, Component.empty());

        setSidebarLine(obj, "line3", 3,
                Component.text("Ping: ", NamedTextColor.WHITE)
                        .append(Component.text(ping + "ms", pingColor))
        );

        setSidebarLine(obj, "line2", 2,
                Component.text("Players: ", NamedTextColor.WHITE)
                        .append(Component.text(String.valueOf(onlineCount), NamedTextColor.GREEN))
        );

        setSidebarLine(obj, "line1", 1, Component.empty());
    }

    private void setSidebarLine(Objective obj, String entryKey, int scoreValue, Component content) {
        Score score = obj.getScore(entryKey);
        score.setScore(scoreValue);
        score.customName(content);
        score.numberFormat(NumberFormat.blank());
    }

    // =========================================================================
    // COMANDOS (/codex, /partida, /pontos, /tag, /sb)
    // =========================================================================

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String cmdName = command.getName().toLowerCase();

        if (cmdName.equals("codex")) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(Component.text("Apenas jogadores podem abrir o QuestCodex.", NamedTextColor.RED));
                return true;
            }
            if (args.length > 0 && args[0].equalsIgnoreCase("livro")) {
                player.getInventory().addItem(createCodexBook());
                player.sendMessage(Component.text("Você recebeu o livro do QuestCodex!", NamedTextColor.GREEN));
                return true;
            }
            openCodexGui(player);
            return true;
        }

        if (cmdName.equals("pontos")) {
            showRankingChat(sender);
            return true;
        }

        if (cmdName.equals("partida")) {
            if (!sender.isOp() && !sender.hasPermission("tags.admin")) {
                sender.sendMessage(Component.text("Você precisa ser OP para gerenciar a partida!", NamedTextColor.RED));
                return true;
            }
            if (args.length == 0) {
                sender.sendMessage(Component.text("=== Comandos da Partida (QuestCodex) ===", NamedTextColor.GOLD).decorate(TextDecoration.BOLD));
                sender.sendMessage(Component.text("/partida iniciar", NamedTextColor.YELLOW).append(Component.text(" - Inicia/ativa a contagem de pontos e entrega o livro", NamedTextColor.GRAY)));
                sender.sendMessage(Component.text("/partida parar", NamedTextColor.YELLOW).append(Component.text(" - Pausa a partida e anuncia o vencedor atual", NamedTextColor.GRAY)));
                sender.sendMessage(Component.text("/partida resetar", NamedTextColor.YELLOW).append(Component.text(" - Zera todos os pontos e conquistas para nova partida", NamedTextColor.GRAY)));
                sender.sendMessage(Component.text("/partida darlivro", NamedTextColor.YELLOW).append(Component.text(" - Entrega o livro QuestCodex para todos", NamedTextColor.GRAY)));
                sender.sendMessage(Component.text("/partida top", NamedTextColor.YELLOW).append(Component.text(" - Mostra o ranking de pontos no chat", NamedTextColor.GRAY)));
                return true;
            }

            switch (args[0].toLowerCase()) {
                case "iniciar", "start" -> {
                    getConfig().set("partida.ativa", true);
                    saveConfig();
                    for (Player p : Bukkit.getOnlinePlayers()) {
                        ensurePlayerHasCodexBook(p);
                        p.playSound(p.getLocation(), Sound.EVENT_RAID_HORN, 1.0f, 1.0f);
                        updatePlayerScoreboard(p);
                    }
                    Bukkit.broadcast(
                            Component.text("⚔ [QUEST CODEX] ", NamedTextColor.GOLD).decorate(TextDecoration.BOLD)
                                    .append(Component.text("A partida começou! Abram o livro ", NamedTextColor.GREEN).decoration(TextDecoration.BOLD, false))
                                    .append(Component.text("QuestCodex", NamedTextColor.YELLOW).decorate(TextDecoration.BOLD))
                                    .append(Component.text(" para ver as conquistas!", NamedTextColor.GREEN).decoration(TextDecoration.BOLD, false))
                    );
                    return true;
                }
                case "parar", "stop", "encerrar" -> {
                    getConfig().set("partida.ativa", false);
                    saveConfig();
                    Bukkit.broadcast(
                            Component.text("🏁 [QUEST CODEX] ", NamedTextColor.GOLD).decorate(TextDecoration.BOLD)
                                    .append(Component.text("A partida foi encerrada!", NamedTextColor.RED).decoration(TextDecoration.BOLD, false))
                    );
                    showRankingChat(Bukkit.getConsoleSender());
                    for (Player p : Bukkit.getOnlinePlayers()) {
                        showRankingChat(p);
                        p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
                    }
                    return true;
                }
                case "resetar", "reset" -> {
                    getConfig().set("pontuacao", null);
                    getConfig().set("conquistas-jogador", null);
                    getConfig().set("primeiro-conquista", null);
                    getConfig().set("partida.ativa", true);
                    blockBreakProgress.clear();
                    mobKillProgress.clear();
                    saveConfig();

                    for (Player p : Bukkit.getOnlinePlayers()) {
                        ensurePlayerHasCodexBook(p);
                        updatePlayerScoreboard(p);
                    }
                    Bukkit.broadcast(
                            Component.text("♻ [QUEST CODEX] ", NamedTextColor.GOLD).decorate(TextDecoration.BOLD)
                                    .append(Component.text("Todos os pontos e conquistas foram resetados para uma nova partida!", NamedTextColor.YELLOW).decoration(TextDecoration.BOLD, false))
                    );
                    return true;
                }
                case "darlivro" -> {
                    for (Player p : Bukkit.getOnlinePlayers()) {
                        p.getInventory().addItem(createCodexBook());
                    }
                    sender.sendMessage(Component.text("Livro QuestCodex entregue para todos os jogadores online!", NamedTextColor.GREEN));
                    return true;
                }
                case "top", "ranking" -> {
                    showRankingChat(sender);
                    return true;
                }
                default -> {
                    sender.sendMessage(Component.text("Subcomando inválido. Use /partida", NamedTextColor.RED));
                    return true;
                }
            }
        }

        if (cmdName.equals("sb")) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(Component.text("Apenas jogadores podem usar /sb.", NamedTextColor.RED));
                return true;
            }
            if (hiddenScoreboard.contains(player.getUniqueId())) {
                hiddenScoreboard.remove(player.getUniqueId());
                updatePlayerScoreboard(player);
                player.sendMessage(Component.text("Scoreboard ativada!", NamedTextColor.GREEN));
            } else {
                hiddenScoreboard.add(player.getUniqueId());
                updatePlayerScoreboard(player);
                player.sendMessage(Component.text("Scoreboard ocultada! Digite /sb novamente para mostrar.", NamedTextColor.YELLOW));
            }
            return true;
        }

        // Comando /tag
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
                sender.sendMessage(Component.text("Configuração de tags e scoreboard recarregada com sucesso!", NamedTextColor.GREEN));
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

    private void showRankingChat(CommandSender sender) {
        sender.sendMessage(Component.empty());
        sender.sendMessage(Component.text("🏆 === RANKING DE PONTOS (QUEST CODEX) === 🏆", NamedTextColor.GOLD).decorate(TextDecoration.BOLD));
        List<Map.Entry<String, Integer>> top = getTopPlayers(10);
        if (top.isEmpty()) {
            sender.sendMessage(Component.text("Nenhum jogador pontuou nesta partida ainda!", NamedTextColor.GRAY));
        } else {
            for (int i = 0; i < top.size(); i++) {
                var entry = top.get(i);
                NamedTextColor posColor = (i == 0) ? NamedTextColor.GOLD : (i == 1) ? NamedTextColor.YELLOW : (i == 2) ? NamedTextColor.GREEN : NamedTextColor.GRAY;
                sender.sendMessage(
                        Component.text("#" + (i + 1) + " ", posColor).decorate(TextDecoration.BOLD)
                                .append(Component.text(entry.getKey() + " — ", NamedTextColor.WHITE).decoration(TextDecoration.BOLD, false))
                                .append(Component.text(entry.getValue() + " pts", NamedTextColor.AQUA).decorate(TextDecoration.BOLD))
                );
            }
        }
        sender.sendMessage(Component.empty());
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

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(Component.empty());
        sender.sendMessage(Component.text("====== Menu de Ajuda: QuestCodex ======", NamedTextColor.GOLD).decorate(TextDecoration.BOLD));
        sender.sendMessage(Component.text("/codex", NamedTextColor.YELLOW).append(Component.text(" - Abre o menu de conquistas e pontos", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/codex livro", NamedTextColor.YELLOW).append(Component.text(" - Recebe o livro QuestCodex novamente", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/pontos", NamedTextColor.YELLOW).append(Component.text(" - Mostra o ranking de pontos da partida", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/tag", NamedTextColor.YELLOW).append(Component.text(" - Abre suas tags no chat para escolher clicando", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/tag <nome>", NamedTextColor.YELLOW).append(Component.text(" - Equipa uma tag direto (ex: /tag dono)", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/sb", NamedTextColor.YELLOW).append(Component.text(" - Mostra ou oculta a Scoreboard lateral", NamedTextColor.GRAY)));
        if (sender.isOp() || sender.hasPermission("tags.admin")) {
            sender.sendMessage(Component.text("--- Comandos de Admin (OP) ---", NamedTextColor.RED));
            sender.sendMessage(Component.text("/partida <iniciar|parar|resetar|darlivro>", NamedTextColor.AQUA).append(Component.text(" - Controla a partida", NamedTextColor.GRAY)));
            sender.sendMessage(Component.text("/tag add <nick> <tag|*>", NamedTextColor.AQUA).append(Component.text(" - Libera uma tag para o jogador usar", NamedTextColor.GRAY)));
            sender.sendMessage(Component.text("/tag set <nick> <tag>", NamedTextColor.AQUA).append(Component.text(" - Coloca e libera a tag no jogador na hora", NamedTextColor.GRAY)));
            sender.sendMessage(Component.text("/tag remover <nick> [tag]", NamedTextColor.AQUA).append(Component.text(" - Reseta ou remove uma tag do jogador", NamedTextColor.GRAY)));
            sender.sendMessage(Component.text("/tag criar <id> <COR> <Nome>", NamedTextColor.AQUA).append(Component.text(" - Cria uma nova tag no servidor", NamedTextColor.GRAY)));
            sender.sendMessage(Component.text("/tag reload", NamedTextColor.AQUA).append(Component.text(" - Recarrega o config.yml", NamedTextColor.GRAY)));
        }
        sender.sendMessage(Component.empty());
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        String cmdName = command.getName().toLowerCase();

        if (cmdName.equals("codex")) {
            if (args.length == 1) {
                return Collections.singletonList("livro");
            }
            return Collections.emptyList();
        }

        if (cmdName.equals("partida")) {
            if (args.length == 1 && (sender.isOp() || sender.hasPermission("tags.admin"))) {
                String p = args[0].toLowerCase();
                List<String> opts = new ArrayList<>(Arrays.asList("iniciar", "parar", "resetar", "darlivro", "top"));
                opts.removeIf(o -> !o.startsWith(p));
                return opts;
            }
            return Collections.emptyList();
        }

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

    private record QuestDef(
            String id,
            int slot,
            Material icon,
            String title,
            String description,
            String difficulty,
            NamedTextColor diffColor,
            int basePoints,
            int firstPoints
    ) {}
}
