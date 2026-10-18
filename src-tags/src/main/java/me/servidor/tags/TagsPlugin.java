package me.servidor.tags;

import io.papermc.paper.event.player.AsyncChatEvent;
import io.papermc.paper.scoreboard.numbers.NumberFormat;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Difficulty;
import org.bukkit.FireworkEffect;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Chest;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Firework;
import org.bukkit.entity.FishHook;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.entity.EntityBreedEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.EntityTameEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.FurnaceExtractEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import org.bukkit.util.Vector;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.Duration;
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
import java.util.Random;
import java.util.Set;
import java.util.UUID;

public final class TagsPlugin extends JavaPlugin implements Listener, CommandExecutor, TabCompleter {

    private final Set<UUID> hiddenScoreboard = new HashSet<>();
    private final Set<UUID> insideSafeZone = new HashSet<>();
    private final Map<String, QuestDef> quests = new LinkedHashMap<>();
    private final Map<String, KitDef> kits = new LinkedHashMap<>();
    private final Map<UUID, String> selectedKits = new HashMap<>();
    private final Map<UUID, Long> kitCooldownUntil = new HashMap<>();
    private final Map<UUID, Long> noFallDamageUntil = new HashMap<>();
    private final Map<UUID, Integer> respawnProtectionSeconds = new HashMap<>();
    private final Map<UUID, UUID> compassTargetMap = new HashMap<>();
    private final Map<String, Long> bountyKillCooldown = new HashMap<>();
    private final Map<String, Integer> blockBreakProgress = new HashMap<>();
    private final Map<String, Integer> mobKillProgress = new HashMap<>();
    private final Random random = new Random();

    private NamespacedKey codexBookKey;
    private NamespacedKey kitSelectorKey;
    private NamespacedKey hunterCompassKey;
    private NamespacedKey kitAbilityKey;

    // Cronômetro da partida (45 min = 2700s)
    private int matchRemainingSeconds = 45 * 60;
    private boolean timerRunning = false;
    private boolean celebratingWinner = false;

    // Controle do Coliseu e Invencibilidade Global (2 min = 120s)
    private boolean gatesOpen = false;
    private int invincibilityRemainingSeconds = 0;

    // Coordenadas do Coliseu nas alturas
    private int coliseumCenterX = 0;
    private int coliseumCenterZ = 0;
    private int coliseumFloorY = 165;
    private int coliseumRadius = 16;

    private static final String GUI_TITLE_CODEX = "QuestCodex — Conquistas";
    private static final String GUI_TITLE_KITS = "Escolha seu Kit (Estilo HG)";

    @Override
    public void onEnable() {
        saveDefaultConfig();
        codexBookKey = new NamespacedKey(this, "quest_codex_book");
        kitSelectorKey = new NamespacedKey(this, "kit_selector_chest");
        hunterCompassKey = new NamespacedKey(this, "hunter_compass");
        kitAbilityKey = new NamespacedKey(this, "kit_ability_item");

        if (!getConfig().contains("scoreboard.titulo")) {
            getConfig().set("scoreboard.ativada", true);
            getConfig().set("scoreboard.titulo", "QUEST CODEX");
            getConfig().set("scoreboard.cor-titulo", "GOLD");
            getConfig().set("scoreboard.negrito-titulo", true);
        }
        if (!getConfig().contains("partida.respawn-invencibilidade-segundos")) {
            getConfig().set("partida.ativa", false);
            getConfig().set("partida.duracao-minutos", 45);
            getConfig().set("partida.invencibilidade-segundos", 120);
            getConfig().set("partida.respawn-invencibilidade-segundos", 30);
            getConfig().set("partida.respawn-speed2-segundos", 15);
            getConfig().set("partida.porcentagem-perda-itens-morte", 30);
            getConfig().set("partida.porcentagem-dano-durabilidade-morte", 20);
            getConfig().set("partida.porcentagem-roubo-pontos-pvp", 15);
            getConfig().set("partida.bonus-primeiro-porcentagem", 50);
            getConfig().set("partida.borda-mapa-blocos", 700);
        } else if (getConfig().getInt("partida.borda-mapa-blocos", 700) == 350) {
            getConfig().set("partida.borda-mapa-blocos", 700);
        }
        if (!getConfig().contains("coliseu.altura-y")) {
            getConfig().set("coliseu.altura-y", 165);
            getConfig().set("coliseu.raio", 16);
        }
        if (!getConfig().contains("safezone.raio-blocos")) {
            getConfig().set("safezone.ativada", true);
            getConfig().set("safezone.raio-blocos", 25);
        }
        saveConfig();

        int defaultMins = getConfig().getInt("partida.duracao-minutos", 45);
        matchRemainingSeconds = defaultMins * 60;
        gatesOpen = getConfig().getBoolean("partida.ativa", false);

        registerDefaultQuests();
        registerDefaultKits();
        setupWorldAndColiseum();

        Bukkit.getPluginManager().registerEvents(this, this);

        for (String cmdName : Arrays.asList("tag", "sb", "codex", "kit", "partida", "pontos")) {
            var cmd = getCommand(cmdName);
            if (cmd != null) {
                cmd.setExecutor(this);
                cmd.setTabCompleter(this);
            }
        }

        for (Player player : Bukkit.getOnlinePlayers()) {
            applyPlayerTag(player);
            giveEssentialItems(player);
            if (isInSafeZone(player.getLocation())) {
                insideSafeZone.add(player.getUniqueId());
            }
        }

        // Loop principal de 1 segundo (Cronômetro 45m + Invencibilidade + Cooldowns + Bússola + Verificação de Inventário + Scoreboard)
        Bukkit.getScheduler().runTaskTimer(this, () -> {
            tickMatchTimer();
            tickPlayerStatesAndCompass();
            for (Player player : Bukkit.getOnlinePlayers()) {
                scanPlayerInventoryForQuests(player);
                updatePlayerScoreboard(player);
            }
        }, 20L, 20L);

        getLogger().info("QuestCodex v4.0 (21 Kits HG com Habilidades/Cooldown + Sistema de Pontos Revisado) ativado!");
    }

    // =========================================================================
    // 21 KITS ESTILO HG (APENAS HABILIDADES ATIVAS/PASSIVAS COM COOLDOWN, ZERO ITENS EXTRAS)
    // =========================================================================

    private void registerDefaultKits() {
        kits.clear();

        // Fileira 1 (Slots 10 a 16)
        addKit("kangaroo", 10, Material.FIREWORK_ROCKET, "Kangaroo", NamedTextColor.AQUA, 7,
                "Foguete Kangaroo",
                Arrays.asList(
                        "Clássico do HG! Clique direito no foguete para",
                        "dar um Salto Duplo (Impulso Aéreo)!",
                        "• Segure SHIFT ao usar para avançar pra frente",
                        "• Reduz o dano de queda após o salto",
                        "• Cooldown: 7 segundos"
                ));

        addKit("thor", 11, Material.WOODEN_AXE, "Thor", NamedTextColor.GOLD, 18,
                "Martelo de Thor",
                Arrays.asList(
                        "Clique direito mirando em um bloco (até 25m)",
                        "para invocar um Raio Mjölnir no local!",
                        "• Causa 5 de dano real nos inimigos próximos",
                        "• Você não recebe dano do seu próprio raio",
                        "• Cooldown: 18 segundos"
                ));

        addKit("phantom", 12, Material.FEATHER, "Phantom", NamedTextColor.WHITE, 35,
                "Pena Phantom",
                Arrays.asList(
                        "Clique direito na Pena para ganhar Voo Livre",
                        "durante 5 segundos!",
                        "• Avisa todo mundo por som quando ativado",
                        "• Imune ao primeiro dano de queda ao pousar",
                        "• Cooldown: 35 segundos"
                ));

        addKit("ninja", 13, Material.NETHER_STAR, "Ninja", NamedTextColor.DARK_PURPLE, 16,
                "Estrela Ninja",
                Arrays.asList(
                        "Clique direito para se teleportar nas costas",
                        "do rival mais próximo (até 25 blocos)!",
                        "• Ganha Speed I por 3 segundos após o TP",
                        "• Cooldown: 16 segundos"
                ));

        addKit("scout", 14, Material.SUGAR, "Scout", NamedTextColor.BLUE, 22,
                "Pó de Velocidade (Scout)",
                Arrays.asList(
                        "Clique direito para ativar um turbo de",
                        "Velocidade II (Speed II) por 7 segundos!",
                        "• Perfeito para alcançar alvos ou escapar",
                        "• Cooldown: 22 segundos"
                ));

        addKit("stomper", 15, Material.IRON_BOOTS, "Stomper", NamedTextColor.RED, 28,
                "Botas Stomper",
                Arrays.asList(
                        "Clique direito para ser lançado 16 blocos",
                        "para o alto e esmagar o chão ao cair!",
                        "• Transfere dano em área (até 5 blocos)",
                        "• Inimigos agachados (SHIFT) recebem metade",
                        "• Cooldown: 28 segundos"
                ));

        addKit("fisherman", 16, Material.FISHING_ROD, "Fisherman", NamedTextColor.GREEN, 8,
                "Vara do Fisherman",
                Arrays.asList(
                        "Fisgue qualquer jogador ou criatura e puxe",
                        "instantaneamente para a sua posição!",
                        "• Item indestrutível focado em habilidade",
                        "• Cooldown de puxão: 8 segundos"
                ));

        // Fileira 2 (Slots 19 a 25)
        addKit("grappler", 19, Material.LEAD, "Grappler", NamedTextColor.YELLOW, 9,
                "Corda do Grappler",
                Arrays.asList(
                        "Clique direito para lançar seu gancho e se",
                        "impulsionar rapidamente na direção que olha!",
                        "• Reduz dano de queda por 4 segundos",
                        "• Cooldown: 9 segundos"
                ));

        addKit("monk", 20, Material.BLAZE_ROD, "Monk", NamedTextColor.GOLD, 18,
                "Cajado do Monk",
                Arrays.asList(
                        "Clique direito diretamente em um jogador rival",
                        "para embaralhar o item da mão dele com o",
                        "inventário dele!",
                        "• Desarma o inimigo no meio do PvP",
                        "• Cooldown: 18 segundos"
                ));

        addKit("viper", 21, Material.SPIDER_EYE, "Viper", NamedTextColor.DARK_GREEN, 20,
                "Presa da Viper",
                Arrays.asList(
                        "Ative para envenenar seus ataques (ou aplique",
                        "Veneno I por 5s nos inimigos a até 6 blocos)!",
                        "• Passiva: 25% de chance de envenenar ao bater",
                        "• Cooldown da ativa: 20 segundos"
                ));

        addKit("snail", 22, Material.SLIME_BALL, "Snail", NamedTextColor.GREEN, 20,
                "Gosma do Snail",
                Arrays.asList(
                        "Ative para lançar uma onda de Lentidão II por",
                        "5 segundos nos rivais próximos (até 6 blocos)!",
                        "• Passiva: 25% de chance de dar Slow ao bater",
                        "• Cooldown da ativa: 20 segundos"
                ));

        addKit("flash", 23, Material.REDSTONE_TORCH, "Flash", NamedTextColor.RED, 24,
                "Tocha do Flash",
                Arrays.asList(
                        "Clique direito para se teleportar na hora",
                        "para o bloco onde você está mirando (até 22m)!",
                        "• Solta partículas de raio vermelho no trajeto",
                        "• Cooldown: 24 segundos"
                ));

        addKit("endermage", 24, Material.ENDER_EYE, "Endermage", NamedTextColor.LIGHT_PURPLE, 26,
                "Portal do Endermage",
                Arrays.asList(
                        "Clique direito para puxar jogadores que estão",
                        "acima ou abaixo de você (coluna vertical de",
                        "5 blocos de raio) para o seu nível!",
                        "• Ambos ficam 3s invulneráveis após o puxão",
                        "• Cooldown: 26 segundos"
                ));

        addKit("poseidon", 25, Material.HEART_OF_THE_SEA, "Poseidon", NamedTextColor.DARK_AQUA, 22,
                "Orbe de Poseidon",
                Arrays.asList(
                        "Ative para ganhar Força I, Graça do Golfinho e",
                        "Respiração Aquática por 6s (e empurrar rivais",
                        "com uma onda de água)!",
                        "• Passiva: Sempre ganha Speed na água/chuva",
                        "• Cooldown da ativa: 22 segundos"
                ));

        // Fileira 3 (Slots 28 a 34)
        addKit("fireman", 28, Material.MAGMA_CREAM, "Fireman", NamedTextColor.GOLD, 22,
                "Núcleo do Fireman",
                Arrays.asList(
                        "Passiva: 100% Imune a Fogo e Lava!",
                        "Ativa: Clique direito para criar um anel de",
                        "chamas que incendeia inimigos em 5 blocos!",
                        "• Cooldown da ativa: 22 segundos"
                ));

        addKit("boxer", 29, Material.BRICK, "Boxer", NamedTextColor.RED, 25,
                "Luva do Boxer",
                Arrays.asList(
                        "Passiva: Seus socos dão dano de Espada de Pedra",
                        "e você recebe -15% de dano corpo-a-corpo!",
                        "Ativa: Ganha Resistência I + Força I por 5s!",
                        "• Cooldown da ativa: 25 segundos"
                ));

        addKit("lumberjack", 30, Material.BONE_MEAL, "Lumberjack", NamedTextColor.GREEN, 15,
                "Totem do Lumberjack",
                Arrays.asList(
                        "Passiva: Quebrar 1 tronco derruba a árvore",
                        "inteira instantaneamente!",
                        "Ativa: Ganha Pressa II (Haste II) por 8s!",
                        "• Cooldown da ativa: 15 segundos"
                ));

        addKit("miner", 31, Material.FLINT, "Miner", NamedTextColor.GRAY, 18,
                "Amuleto do Miner",
                Arrays.asList(
                        "Passiva: Quebrar minérios quebra os minérios",
                        "conectados da mesma veia automaticamente!",
                        "Ativa: Pressa II + Visão Noturna por 12s!",
                        "• Cooldown da ativa: 18 segundos"
                ));

        addKit("ghost", 32, Material.QUARTZ, "Ghost", NamedTextColor.WHITE, 30,
                "Manto Fantasma (Ghost)",
                Arrays.asList(
                        "Clique direito para ficar Invisível + Speed II",
                        "durante 6 segundos!",
                        "• Ótimo para emboscadas ou fugir de caçadores",
                        "• Cooldown: 30 segundos"
                ));

        addKit("gladiator", 33, Material.IRON_BARS, "Arena (Gladiator)", NamedTextColor.AQUA, 32,
                "Grades do Gladiator",
                Arrays.asList(
                        "Clique direito para puxar o rival mais próximo",
                        "(até 10m) para o duelo e ganhar Absorção",
                        "(+2 corações dourados) por 8 segundos!",
                        "• Cooldown: 32 segundos"
                ));

        addKit("anchor", 34, Material.ANVIL, "Anchor", NamedTextColor.DARK_GRAY, 20,
                "Âncora Pesada (Anchor)",
                Arrays.asList(
                        "Passiva: Você e quem você ataca não sofrem",
                        "repulsão (Knockback zero) no combate!",
                        "Ativa: Causa tremor que dá Lentidão II ao redor",
                        "• Cooldown da ativa: 20 segundos"
                ));
    }

    private void addKit(String id, int slot, Material icon, String name, NamedTextColor color,
                        int cooldownSeconds, String abilityItemName, List<String> description) {
        kits.put(id, new KitDef(id, slot, icon, name, color, cooldownSeconds, abilityItemName, description));
    }

    public ItemStack createKitSelectorChest() {
        ItemStack item = new ItemStack(Material.CHEST);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(
                    Component.text("🎁 Escolher Kit HG ", NamedTextColor.GOLD).decorate(TextDecoration.BOLD)
                            .append(Component.text("(Clique Direito)", NamedTextColor.YELLOW).decoration(TextDecoration.BOLD, false))
                            .decoration(TextDecoration.ITALIC, false)
            );
            List<Component> lore = new ArrayList<>();
            lore.add(Component.empty());
            lore.add(Component.text("Escolha sua habilidade especial estilo HG!", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
            lore.add(Component.text("Nenhum kit dá itens apelões — cada kit dá", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
            lore.add(Component.text("1 item ativador de habilidade com Cooldown!", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false));
            meta.lore(lore);
            meta.getPersistentDataContainer().set(kitSelectorKey, PersistentDataType.BYTE, (byte) 1);
            item.setItemMeta(meta);
        }
        return item;
    }

    public ItemStack createKitAbilityItem(String kitId) {
        KitDef kit = kits.getOrDefault(kitId, kits.get("kangaroo"));
        if (kit == null) return null;

        ItemStack item = new ItemStack(kit.icon);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(
                    Component.text("⚡ " + kit.abilityItemName + " ", kit.color).decorate(TextDecoration.BOLD)
                            .append(Component.text("(Kit " + kit.name + ")", NamedTextColor.GRAY).decoration(TextDecoration.BOLD, false))
                            .decoration(TextDecoration.ITALIC, false)
            );
            List<Component> lore = new ArrayList<>();
            lore.add(Component.empty());
            for (String line : kit.description) {
                lore.add(Component.text(line, NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false));
            }
            lore.add(Component.empty());
            lore.add(Component.text("🔒 Item de Habilidade Fixo (Não dropa)", NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false));
            meta.lore(lore);
            meta.setUnbreakable(true);
            meta.addEnchant(Enchantment.UNBREAKING, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_UNBREAKABLE);
            meta.getPersistentDataContainer().set(kitAbilityKey, PersistentDataType.STRING, kit.id);
            item.setItemMeta(meta);
        }
        return item;
    }

    public boolean isKitSelectorChest(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;
        return meta.getPersistentDataContainer().has(kitSelectorKey, PersistentDataType.BYTE);
    }

    public boolean isKitAbilityItem(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;
        return meta.getPersistentDataContainer().has(kitAbilityKey, PersistentDataType.STRING);
    }

    public String getKitIdFromAbilityItem(ItemStack item) {
        if (!isKitAbilityItem(item)) return null;
        return item.getItemMeta().getPersistentDataContainer().get(kitAbilityKey, PersistentDataType.STRING);
    }

    public void removeKitSelectorChests(Player player) {
        Inventory inv = player.getInventory();
        for (int i = 0; i < inv.getSize(); i++) {
            if (isKitSelectorChest(inv.getItem(i))) {
                inv.setItem(i, null);
            }
        }
    }

    public void removeKitAbilityItems(Player player) {
        Inventory inv = player.getInventory();
        for (int i = 0; i < inv.getSize(); i++) {
            if (isKitAbilityItem(inv.getItem(i))) {
                inv.setItem(i, null);
            }
        }
    }

    public void openKitSelectorGui(Player player) {
        if (gatesOpen) {
            player.sendMessage(Component.text("A partida já começou! Não é mais possível trocar de Kit.", NamedTextColor.RED));
            removeKitSelectorChests(player);
            return;
        }

        Inventory inv = Bukkit.createInventory(
                null,
                54,
                Component.text(GUI_TITLE_KITS, NamedTextColor.GOLD).decorate(TextDecoration.BOLD)
        );

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

        String currentKit = selectedKits.getOrDefault(player.getUniqueId(), "kangaroo");

        for (KitDef kit : kits.values()) {
            boolean isSelected = kit.id.equalsIgnoreCase(currentKit);
            ItemStack icon = new ItemStack(kit.icon);
            ItemMeta meta = icon.getItemMeta();
            if (meta != null) {
                meta.displayName(
                        Component.text((isSelected ? "✔ " : "⚡ ") + "Kit " + kit.name, kit.color)
                                .decorate(TextDecoration.BOLD)
                                .decoration(TextDecoration.ITALIC, false)
                );
                List<Component> lore = new ArrayList<>();
                lore.add(Component.text("Item Ativador: ", NamedTextColor.GRAY)
                        .append(Component.text(kit.abilityItemName, kit.color))
                        .decoration(TextDecoration.ITALIC, false));
                lore.add(Component.empty());
                for (String line : kit.description) {
                    lore.add(Component.text(line, NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false));
                }
                lore.add(Component.empty());
                if (isSelected) {
                    lore.add(Component.text("✔ KIT SELECIONADO!", NamedTextColor.GREEN).decorate(TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false));
                    meta.addEnchant(Enchantment.UNBREAKING, 1, true);
                    meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                } else {
                    lore.add(Component.text("👉 Clique para selecionar este Kit!", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false));
                }
                meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
                meta.lore(lore);
                icon.setItemMeta(meta);
            }
            inv.setItem(kit.slot, icon);
        }

        player.openInventory(inv);
        player.playSound(player.getLocation(), Sound.BLOCK_CHEST_OPEN, 0.8f, 1.2f);
    }

    public void deliverSelectedKit(Player player) {
        removeKitSelectorChests(player);
        removeKitAbilityItems(player);

        String kitId = selectedKits.getOrDefault(player.getUniqueId(), "kangaroo");
        KitDef kit = kits.getOrDefault(kitId, kits.get("kangaroo"));
        ItemStack abilityItem = createKitAbilityItem(kitId);
        if (abilityItem != null) {
            player.getInventory().addItem(abilityItem);
        }

        if (kit != null) {
            player.sendMessage(
                    Component.text("⚡ Habilidade do ", NamedTextColor.GREEN)
                            .append(Component.text("Kit " + kit.name, kit.color).decorate(TextDecoration.BOLD))
                            .append(Component.text(" ativada! Use seu item especial para ativar a habilidade!", NamedTextColor.GREEN))
            );
        }
    }

    // =========================================================================
    // EXECUÇÃO DAS HABILIDADES DOS 21 KITS HG E SISTEMA DE COOLDOWN
    // =========================================================================

    private boolean checkAndTriggerKitCooldown(Player player, KitDef kit) {
        long now = System.currentTimeMillis();
        long until = kitCooldownUntil.getOrDefault(player.getUniqueId(), 0L);
        if (now < until) {
            double remaining = (until - now) / 1000.0;
            player.sendActionBar(
                    Component.text("⏳ Habilidade " + kit.name + " em Cooldown: ", NamedTextColor.RED)
                            .append(Component.text(String.format("%.1fs", remaining), NamedTextColor.YELLOW).decorate(TextDecoration.BOLD))
            );
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.7f, 0.8f);
            return false;
        }
        kitCooldownUntil.put(player.getUniqueId(), now + (kit.cooldownSeconds * 1000L));
        return true;
    }

    public void activateKitAbility(Player player, String kitId) {
        if (!gatesOpen || celebratingWinner) {
            player.sendActionBar(Component.text("🔒 Aguarde a abertura dos portões para usar a habilidade do Kit!", NamedTextColor.RED));
            return;
        }
        if (isInSafeZone(player.getLocation())) {
            player.sendActionBar(Component.text("🛡 Saia do Coliseu para usar habilidades de Kit!", NamedTextColor.RED));
            return;
        }

        KitDef kit = kits.get(kitId);
        if (kit == null) return;

        switch (kitId) {
            case "kangaroo" -> {
                if (!checkAndTriggerKitCooldown(player, kit)) return;
                Vector dir = player.getLocation().getDirection().normalize();
                Vector vel;
                if (player.isSneaking()) {
                    // Impulso horizontal longo (frente)
                    vel = dir.multiply(1.35).setY(0.55);
                } else {
                    // Salto duplo alto
                    vel = dir.multiply(0.65).setY(0.95);
                }
                player.setVelocity(vel);
                noFallDamageUntil.put(player.getUniqueId(), System.currentTimeMillis() + 5000L);
                player.getWorld().spawnParticle(Particle.CLOUD, player.getLocation(), 15, 0.3, 0.1, 0.3, 0.05);
                player.playSound(player.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 0.9f, 1.3f);
                player.sendActionBar(Component.text("🦘 Salto Kangaroo utilizado! (Cooldown: " + kit.cooldownSeconds + "s)", NamedTextColor.AQUA));
            }
            case "thor" -> {
                Block targetBlock = player.getTargetBlockExact(25);
                if (targetBlock == null || targetBlock.getType() == Material.AIR) {
                    player.sendActionBar(Component.text("⚡ Mire em um bloco a até 25m para invocar o Raio de Thor!", NamedTextColor.RED));
                    return;
                }
                if (!checkAndTriggerKitCooldown(player, kit)) return;
                Location strikeLoc = targetBlock.getLocation().add(0.5, 1.0, 0.5);
                player.getWorld().strikeLightningEffect(strikeLoc);
                player.getWorld().playSound(strikeLoc, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.0f, 1.0f);
                for (Entity near : player.getWorld().getNearbyEntities(strikeLoc, 3.5, 3.5, 3.5)) {
                    if (near instanceof LivingEntity living && !near.getUniqueId().equals(player.getUniqueId())) {
                        if (living instanceof Player victim && (invincibilityRemainingSeconds > 0 || respawnProtectionSeconds.containsKey(victim.getUniqueId()))) {
                            continue;
                        }
                        living.damage(5.0, player);
                        living.setFireTicks(60);
                    }
                }
                player.sendActionBar(Component.text("⚡ Raio de Thor invocado!", NamedTextColor.GOLD));
            }
            case "phantom" -> {
                if (!checkAndTriggerKitCooldown(player, kit)) return;
                player.setAllowFlight(true);
                player.setFlying(true);
                noFallDamageUntil.put(player.getUniqueId(), System.currentTimeMillis() + 10000L);
                player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PHANTOM_FLAP, 1.5f, 1.0f);
                player.sendMessage(Component.text("👻 [Phantom] Você tem 5 segundos de voo!", NamedTextColor.AQUA));

                for (int s = 1; s <= 4; s++) {
                    final int rem = 5 - s;
                    Bukkit.getScheduler().runTaskLater(this, () -> {
                        if (player.isOnline()) {
                            player.sendActionBar(Component.text("👻 Voo Phantom termina em: " + rem + "s", NamedTextColor.WHITE));
                            player.getWorld().spawnParticle(Particle.CLOUD, player.getLocation(), 8, 0.2, 0.1, 0.2, 0.02);
                        }
                    }, s * 20L);
                }
                Bukkit.getScheduler().runTaskLater(this, () -> {
                    if (player.isOnline() && player.getGameMode() == GameMode.SURVIVAL) {
                        player.setFlying(false);
                        player.setAllowFlight(false);
                        player.sendMessage(Component.text("👻 [Phantom] Seu tempo de voo acabou!", NamedTextColor.YELLOW));
                    }
                }, 5 * 20L);
            }
            case "ninja" -> {
                Player nearest = findNearestEnemy(player, 25.0);
                if (nearest == null) {
                    player.sendActionBar(Component.text("🥷 Nenhum rival a até 25 blocos para usar o Passo Ninja!", NamedTextColor.RED));
                    return;
                }
                if (!checkAndTriggerKitCooldown(player, kit)) return;
                Location behind = nearest.getLocation().clone();
                Vector backDir = behind.getDirection().normalize().multiply(-1.2);
                behind.add(backDir);
                player.getWorld().spawnParticle(Particle.LARGE_SMOKE, player.getLocation().add(0, 1, 0), 20, 0.3, 0.5, 0.3, 0.02);
                player.teleport(behind);
                player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 60, 0, false, true, true));
                player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.4f);
                player.sendActionBar(Component.text("🥷 Teleportado para trás de " + nearest.getName() + "!", NamedTextColor.DARK_PURPLE));
            }
            case "scout" -> {
                if (!checkAndTriggerKitCooldown(player, kit)) return;
                player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 7 * 20, 1, false, true, true));
                player.playSound(player.getLocation(), Sound.ENTITY_BREEZE_JUMP, 1.0f, 1.3f);
                player.sendActionBar(Component.text("💨 Turbo Scout ativado! (Speed II por 7s)", NamedTextColor.BLUE));
            }
            case "stomper" -> {
                if (!checkAndTriggerKitCooldown(player, kit)) return;
                player.setVelocity(new Vector(0, 1.55, 0));
                noFallDamageUntil.put(player.getUniqueId(), System.currentTimeMillis() + 6000L);
                player.playSound(player.getLocation(), Sound.ENTITY_IRON_GOLEM_ATTACK, 1.0f, 0.8f);
                player.sendActionBar(Component.text("🥾 Salto Stomper ativado! Esmague os inimigos ao pousar!", NamedTextColor.RED));

                Bukkit.getScheduler().runTaskLater(this, () -> {
                    if (!player.isOnline()) return;
                    Location land = player.getLocation();
                    land.getWorld().spawnParticle(Particle.EXPLOSION, land, 6, 1.5, 0.2, 1.5, 0.05);
                    land.getWorld().playSound(land, Sound.ENTITY_GENERIC_EXPLODE, 0.9f, 1.1f);
                    for (Entity near : land.getWorld().getNearbyEntities(land, 5.0, 4.0, 5.0)) {
                        if (near instanceof LivingEntity living && !near.getUniqueId().equals(player.getUniqueId())) {
                            if (living instanceof Player victim) {
                                if (invincibilityRemainingSeconds > 0 || respawnProtectionSeconds.containsKey(victim.getUniqueId())) continue;
                                double dmg = victim.isSneaking() ? 4.0 : 8.0;
                                victim.damage(dmg, player);
                            } else {
                                living.damage(10.0, player);
                            }
                        }
                    }
                }, 38L);
            }
            case "grappler" -> {
                if (!checkAndTriggerKitCooldown(player, kit)) return;
                Vector pull = player.getLocation().getDirection().normalize().multiply(1.55).setY(0.65);
                player.setVelocity(pull);
                noFallDamageUntil.put(player.getUniqueId(), System.currentTimeMillis() + 4500L);
                player.playSound(player.getLocation(), Sound.ENTITY_FISHING_BOBBER_RETRIEVE, 1.0f, 1.2f);
                player.sendActionBar(Component.text("🪝 Gancho Grappler lançado!", NamedTextColor.YELLOW));
            }
            case "viper" -> {
                if (!checkAndTriggerKitCooldown(player, kit)) return;
                player.getWorld().spawnParticle(Particle.SNEEZE, player.getLocation().add(0, 1, 0), 30, 2.0, 0.5, 2.0, 0.02);
                player.playSound(player.getLocation(), Sound.ENTITY_SPIDER_AMBIENT, 1.0f, 1.2f);
                for (Entity near : player.getNearbyEntities(6.0, 4.0, 6.0)) {
                    if (near instanceof LivingEntity living && !near.getUniqueId().equals(player.getUniqueId())) {
                        living.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 5 * 20, 0, false, true, true));
                    }
                }
                player.sendActionBar(Component.text("🐍 Nuvem de Veneno Viper ativada!", NamedTextColor.DARK_GREEN));
            }
            case "snail" -> {
                if (!checkAndTriggerKitCooldown(player, kit)) return;
                player.getWorld().spawnParticle(Particle.ITEM_SLIME, player.getLocation().add(0, 1, 0), 30, 2.0, 0.5, 2.0, 0.02);
                player.playSound(player.getLocation(), Sound.ENTITY_SLIME_SQUISH, 1.0f, 0.9f);
                for (Entity near : player.getNearbyEntities(6.0, 4.0, 6.0)) {
                    if (near instanceof LivingEntity living && !near.getUniqueId().equals(player.getUniqueId())) {
                        living.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 5 * 20, 1, false, true, true));
                    }
                }
                player.sendActionBar(Component.text("🐌 Onda de Lentidão Snail ativada!", NamedTextColor.GREEN));
            }
            case "flash" -> {
                Block target = player.getTargetBlockExact(22);
                if (target == null || target.getType() == Material.AIR) {
                    player.sendActionBar(Component.text("⚡ Mire em um bloco a até 22 blocos para usar o Flash!", NamedTextColor.RED));
                    return;
                }
                if (!checkAndTriggerKitCooldown(player, kit)) return;
                Location dest = target.getLocation().add(0.5, 1.0, 0.5);
                dest.setYaw(player.getLocation().getYaw());
                dest.setPitch(player.getLocation().getPitch());
                player.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, player.getLocation().add(0, 1, 0), 25, 0.3, 0.5, 0.3, 0.1);
                player.teleport(dest);
                noFallDamageUntil.put(player.getUniqueId(), System.currentTimeMillis() + 3000L);
                player.getWorld().playSound(dest, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.6f);
                player.sendActionBar(Component.text("⚡ Teleporte Flash realizado!", NamedTextColor.RED));
            }
            case "endermage" -> {
                List<Player> pulled = new ArrayList<>();
                Location pLoc = player.getLocation();
                for (Player other : player.getWorld().getPlayers()) {
                    if (other.getUniqueId().equals(player.getUniqueId())) continue;
                    if (isInSafeZone(other.getLocation())) continue;
                    double dx = Math.abs(other.getLocation().getX() - pLoc.getX());
                    double dz = Math.abs(other.getLocation().getZ() - pLoc.getZ());
                    double dy = Math.abs(other.getLocation().getY() - pLoc.getY());
                    if (dx <= 5.0 && dz <= 5.0 && dy >= 3.0 && dy <= 60.0) {
                        pulled.add(other);
                    }
                }
                if (pulled.isEmpty()) {
                    player.sendActionBar(Component.text("🔮 Nenhum jogador acima/abaixo na coluna de 5 blocos para puxar!", NamedTextColor.RED));
                    return;
                }
                if (!checkAndTriggerKitCooldown(player, kit)) return;
                for (Player target : pulled) {
                    target.teleport(pLoc);
                    target.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 60, 4, false, true, true));
                    target.sendMessage(Component.text("🔮 Você foi puxado pelo Endermage " + player.getName() + "! (3s de invulnerabilidade)", NamedTextColor.LIGHT_PURPLE));
                }
                player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 60, 4, false, true, true));
                player.playSound(pLoc, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 0.8f);
                player.sendActionBar(Component.text("🔮 Puxou " + pulled.size() + " jogador(es) com o Endermage!", NamedTextColor.LIGHT_PURPLE));
            }
            case "poseidon" -> {
                if (!checkAndTriggerKitCooldown(player, kit)) return;
                player.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 6 * 20, 0, false, true, true));
                player.addPotionEffect(new PotionEffect(PotionEffectType.DOLPHINS_GRACE, 6 * 20, 0, false, true, true));
                player.addPotionEffect(new PotionEffect(PotionEffectType.WATER_BREATHING, 6 * 20, 0, false, true, true));
                player.getWorld().spawnParticle(Particle.SPLASH, player.getLocation().add(0, 1, 0), 40, 1.5, 0.5, 1.5, 0.2);
                player.playSound(player.getLocation(), Sound.ITEM_TRIDENT_RIPTIDE_3, 1.0f, 1.0f);
                for (Entity near : player.getNearbyEntities(5.0, 3.0, 5.0)) {
                    if (near instanceof LivingEntity living && !near.getUniqueId().equals(player.getUniqueId())) {
                        Vector push = living.getLocation().toVector().subtract(player.getLocation().toVector()).normalize().multiply(1.1).setY(0.4);
                        living.setVelocity(push);
                    }
                }
                player.sendActionBar(Component.text("🌊 Fúria de Poseidon ativada! (Força I + Onda)", NamedTextColor.DARK_AQUA));
            }
            case "fireman" -> {
                if (!checkAndTriggerKitCooldown(player, kit)) return;
                player.getWorld().spawnParticle(Particle.FLAME, player.getLocation().add(0, 1, 0), 35, 1.5, 0.4, 1.5, 0.05);
                player.playSound(player.getLocation(), Sound.ITEM_FIRECHARGE_USE, 1.0f, 1.0f);
                for (Entity near : player.getNearbyEntities(5.0, 3.0, 5.0)) {
                    if (near instanceof LivingEntity living && !near.getUniqueId().equals(player.getUniqueId())) {
                        living.setFireTicks(100);
                    }
                }
                player.sendActionBar(Component.text("🔥 Anel de Fogo Fireman ativado!", NamedTextColor.GOLD));
            }
            case "boxer" -> {
                if (!checkAndTriggerKitCooldown(player, kit)) return;
                player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 5 * 20, 0, false, true, true));
                player.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 5 * 20, 0, false, true, true));
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_STRONG, 1.0f, 0.8f);
                player.sendActionBar(Component.text("🥊 Fúria Boxer ativada! (Força I + Resistência I por 5s)", NamedTextColor.RED));
            }
            case "lumberjack" -> {
                if (!checkAndTriggerKitCooldown(player, kit)) return;
                player.addPotionEffect(new PotionEffect(PotionEffectType.HASTE, 8 * 20, 1, false, true, true));
                player.playSound(player.getLocation(), Sound.ITEM_AXE_STRIP, 1.0f, 1.1f);
                player.sendActionBar(Component.text("🪓 Pressa II do Lumberjack ativada por 8s!", NamedTextColor.GREEN));
            }
            case "miner" -> {
                if (!checkAndTriggerKitCooldown(player, kit)) return;
                player.addPotionEffect(new PotionEffect(PotionEffectType.HASTE, 12 * 20, 1, false, true, true));
                player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, 15 * 20, 0, false, true, true));
                player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_LAND, 0.6f, 1.4f);
                player.sendActionBar(Component.text("⛏ Pressa II + Visão Noturna do Miner ativadas!", NamedTextColor.GRAY));
            }
            case "ghost" -> {
                if (!checkAndTriggerKitCooldown(player, kit)) return;
                player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 6 * 20, 0, false, false, true));
                player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 6 * 20, 1, false, false, true));
                player.getWorld().spawnParticle(Particle.SMOKE, player.getLocation().add(0, 1, 0), 25, 0.4, 0.6, 0.4, 0.02);
                player.playSound(player.getLocation(), Sound.ENTITY_ILLUSIONER_MIRROR_MOVE, 1.0f, 1.2f);
                player.sendActionBar(Component.text("👻 Modo Ghost ativado! (Invisível + Speed II por 6s)", NamedTextColor.WHITE));
            }
            case "gladiator" -> {
                Player target = findNearestEnemy(player, 10.0);
                if (target == null) {
                    player.sendActionBar(Component.text("⚔ Nenhum rival a até 10 blocos para desafiar com o Gladiator!", NamedTextColor.RED));
                    return;
                }
                if (!checkAndTriggerKitCooldown(player, kit)) return;
                target.teleport(player.getLocation().add(player.getLocation().getDirection().normalize().multiply(1.5)));
                player.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 8 * 20, 0, false, true, true));
                target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40, 0, false, true, true));
                player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_PLACE, 0.9f, 1.1f);
                player.sendActionBar(Component.text("⚔ Você puxou " + target.getName() + " para o duelo Gladiator!", NamedTextColor.AQUA));
            }
            case "anchor" -> {
                if (!checkAndTriggerKitCooldown(player, kit)) return;
                player.getWorld().playSound(player.getLocation(), Sound.BLOCK_ANVIL_LAND, 0.9f, 0.7f);
                for (Entity near : player.getNearbyEntities(5.0, 3.0, 5.0)) {
                    if (near instanceof LivingEntity living && !near.getUniqueId().equals(player.getUniqueId())) {
                        living.setVelocity(new Vector(0, -0.5, 0));
                        living.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 4 * 20, 1, false, true, true));
                    }
                }
                player.sendActionBar(Component.text("⚓ Impacto Anchor ativado!", NamedTextColor.DARK_GRAY));
            }
            case "monk" -> {
                player.sendActionBar(Component.text("🦯 Clique direito diretamente em um jogador rival para desarmá-lo com o Monk!", NamedTextColor.GOLD));
            }
            default -> {}
        }
    }

    private Player findNearestEnemy(Player source, double maxDist) {
        Player closest = null;
        double bestDistSq = maxDist * maxDist;
        for (Player p : source.getWorld().getPlayers()) {
            if (p.getUniqueId().equals(source.getUniqueId())) continue;
            if (p.getGameMode() != GameMode.SURVIVAL) continue;
            if (isInSafeZone(p.getLocation())) continue;
            if (invincibilityRemainingSeconds > 0 || respawnProtectionSeconds.containsKey(p.getUniqueId())) continue;
            double dSq = source.getLocation().distanceSquared(p.getLocation());
            if (dSq <= bestDistSq) {
                bestDistSq = dSq;
                closest = p;
            }
        }
        return closest;
    }

    // Habilidade ativa do Monk ao clicar direito em um jogador
    @EventHandler
    public void onInteractEntityMonk(PlayerInteractEntityEvent event) {
        Player player = event.getPlayer();
        ItemStack hand = player.getInventory().getItemInMainHand();
        if (!"monk".equals(getKitIdFromAbilityItem(hand))) return;

        if (event.getRightClicked() instanceof Player target) {
            event.setCancelled(true);
            if (!gatesOpen || invincibilityRemainingSeconds > 0 || isInSafeZone(target.getLocation())) {
                player.sendActionBar(Component.text("🛡 Você não pode usar o Monk agora!", NamedTextColor.RED));
                return;
            }
            KitDef kit = kits.get("monk");
            if (!checkAndTriggerKitCooldown(player, kit)) return;

            Inventory tInv = target.getInventory();
            int heldSlot = target.getInventory().getHeldItemSlot();
            int randomSlot = 9 + random.nextInt(27); // Slots internos da mochila (9..35)
            ItemStack heldItem = tInv.getItem(heldSlot);
            ItemStack swapItem = tInv.getItem(randomSlot);

            if (isCodexBook(heldItem) || isHunterCompass(heldItem) || isKitAbilityItem(heldItem)) {
                player.sendActionBar(Component.text("🦯 O alvo está segurando um item fixo!", NamedTextColor.YELLOW));
                return;
            }

            tInv.setItem(heldSlot, swapItem);
            tInv.setItem(randomSlot, heldItem);
            player.playSound(player.getLocation(), Sound.ENTITY_ILLUSIONER_CAST_SPELL, 1.0f, 1.2f);
            target.playSound(target.getLocation(), Sound.ITEM_SHIELD_BREAK, 1.0f, 1.2f);
            player.sendActionBar(Component.text("🦯 Você embaralhou a mão de " + target.getName() + " com o Monk!", NamedTextColor.GOLD));
            target.sendActionBar(Component.text("🦯 O Monk " + player.getName() + " embaralhou o item da sua mão!", NamedTextColor.RED));
        }
    }

    // Passivas de Combate (Viper, Snail, Boxer, Anchor)
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onCombatKitPassives(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player attacker && event.getEntity() instanceof LivingEntity victim) {
            String attackerKit = selectedKits.getOrDefault(attacker.getUniqueId(), "kangaroo");

            if (attackerKit.equals("viper") && random.nextInt(100) < 25) {
                victim.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 4 * 20, 0, false, true, true));
            } else if (attackerKit.equals("snail") && random.nextInt(100) < 25) {
                victim.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 4 * 20, 0, false, true, true));
            } else if (attackerKit.equals("boxer")) {
                ItemStack hand = attacker.getInventory().getItemInMainHand();
                if (hand.getType() == Material.AIR || isKitAbilityItem(hand)) {
                    event.setDamage(event.getDamage() + 3.5);
                }
            } else if (attackerKit.equals("anchor")) {
                Bukkit.getScheduler().runTaskLater(this, () -> victim.setVelocity(new Vector(0, 0, 0)), 1L);
            }
        }

        if (event.getEntity() instanceof Player defender) {
            String defenderKit = selectedKits.getOrDefault(defender.getUniqueId(), "kangaroo");
            if (defenderKit.equals("boxer")) {
                event.setDamage(event.getDamage() * 0.85);
            } else if (defenderKit.equals("anchor")) {
                Bukkit.getScheduler().runTaskLater(this, () -> defender.setVelocity(new Vector(0, 0, 0)), 1L);
            }
        }
    }

    // =========================================================================
    // BÚSSOLA DO CAÇADOR DE PONTOS (NÃO DROPÁVEL + RASTREIO + ROUBO EQUILIBRADO)
    // =========================================================================

    public ItemStack createHunterCompass() {
        ItemStack item = new ItemStack(Material.COMPASS);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(
                    Component.text("🧭 Bússola Caça-Pontos ", NamedTextColor.RED).decorate(TextDecoration.BOLD)
                            .append(Component.text("(Clique Direito)", NamedTextColor.GRAY).decoration(TextDecoration.BOLD, false))
                            .decoration(TextDecoration.ITALIC, false)
            );
            List<Component> lore = new ArrayList<>();
            lore.add(Component.empty());
            lore.add(Component.text("Aponta em tempo real para o Líder ou", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
            lore.add(Component.text("para o jogador que você escolher caçar!", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
            lore.add(Component.empty());
            lore.add(Component.text("• Clique Direito: Alterna o alvo rastreado", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false));
            lore.add(Component.text("• Abater rivais rouba 15% dos pontos deles!", NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false));
            meta.lore(lore);
            meta.getPersistentDataContainer().set(hunterCompassKey, PersistentDataType.BYTE, (byte) 1);
            item.setItemMeta(meta);
        }
        return item;
    }

    public boolean isHunterCompass(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;
        return meta.getPersistentDataContainer().has(hunterCompassKey, PersistentDataType.BYTE);
    }

    public void giveEssentialItems(Player player) {
        boolean hasBook = false;
        boolean hasCompass = false;
        boolean hasKitChest = false;
        boolean hasKitAbility = false;

        for (ItemStack item : player.getInventory().getContents()) {
            if (isCodexBook(item)) hasBook = true;
            if (isHunterCompass(item)) hasCompass = true;
            if (isKitSelectorChest(item)) hasKitChest = true;
            if (isKitAbilityItem(item)) hasKitAbility = true;
        }

        if (!hasBook) {
            player.getInventory().addItem(createCodexBook());
        }
        if (!hasCompass) {
            player.getInventory().addItem(createHunterCompass());
        }
        if (!gatesOpen && !hasKitChest) {
            player.getInventory().setItem(4, createKitSelectorChest());
        } else if (gatesOpen) {
            if (hasKitChest) removeKitSelectorChests(player);
            if (!hasKitAbility) {
                String kitId = selectedKits.getOrDefault(player.getUniqueId(), "kangaroo");
                ItemStack ability = createKitAbilityItem(kitId);
                if (ability != null) player.getInventory().addItem(ability);
            }
        }
    }

    private void cycleCompassTarget(Player hunter) {
        List<Player> candidates = new ArrayList<>();
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (!p.getUniqueId().equals(hunter.getUniqueId())
                    && p.getWorld().equals(hunter.getWorld())
                    && p.getGameMode() == GameMode.SURVIVAL
                    && !isInSafeZone(p.getLocation())) {
                candidates.add(p);
            }
        }

        if (candidates.isEmpty()) {
            hunter.sendActionBar(Component.text("🧭 Nenhum jogador fora do Coliseu nesta dimensão para rastrear!", NamedTextColor.RED));
            return;
        }

        candidates.sort((a, b) -> Integer.compare(getPlayerPoints(b.getName()), getPlayerPoints(a.getName())));

        UUID currentTargetId = compassTargetMap.get(hunter.getUniqueId());
        Player nextTarget = candidates.get(0);
        if (currentTargetId != null) {
            for (int i = 0; i < candidates.size(); i++) {
                if (candidates.get(i).getUniqueId().equals(currentTargetId)) {
                    nextTarget = candidates.get((i + 1) % candidates.size());
                    break;
                }
            }
        }

        compassTargetMap.put(hunter.getUniqueId(), nextTarget.getUniqueId());
        hunter.setCompassTarget(nextTarget.getLocation());
        int dist = (int) hunter.getLocation().distance(nextTarget.getLocation());
        int targetPts = getPlayerPoints(nextTarget.getName());

        hunter.playSound(hunter.getLocation(), Sound.UI_BUTTON_CLICK, 0.8f, 1.4f);
        hunter.sendActionBar(
                Component.text("🧭 Alvo: ", NamedTextColor.RED).decorate(TextDecoration.BOLD)
                        .append(Component.text(nextTarget.getName(), NamedTextColor.YELLOW).decorate(TextDecoration.BOLD))
                        .append(Component.text(" (" + targetPts + " pts) — ", NamedTextColor.AQUA).decoration(TextDecoration.BOLD, false))
                        .append(Component.text(dist + "m de distância", NamedTextColor.GREEN).decorate(TextDecoration.BOLD))
        );
    }

    private Player resolveCompassTarget(Player hunter) {
        UUID savedId = compassTargetMap.get(hunter.getUniqueId());
        if (savedId != null) {
            Player t = Bukkit.getPlayer(savedId);
            if (t != null && t.isOnline() && t.getWorld().equals(hunter.getWorld()) && !isInSafeZone(t.getLocation())) {
                return t;
            }
        }

        Player best = null;
        int bestPts = -1;
        double bestDist = Double.MAX_VALUE;

        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.getUniqueId().equals(hunter.getUniqueId())) continue;
            if (!p.getWorld().equals(hunter.getWorld())) continue;
            if (p.getGameMode() != GameMode.SURVIVAL) continue;
            if (isInSafeZone(p.getLocation())) continue;

            int pts = getPlayerPoints(p.getName());
            double dist = hunter.getLocation().distanceSquared(p.getLocation());
            if (pts > bestPts || (pts == bestPts && dist < bestDist)) {
                best = p;
                bestPts = pts;
                bestDist = dist;
            }
        }
        if (best != null) {
            compassTargetMap.put(hunter.getUniqueId(), best.getUniqueId());
        }
        return best;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerDropEssential(PlayerDropItemEvent event) {
        ItemStack dropped = event.getItemDrop().getItemStack();
        if (isCodexBook(dropped) || isHunterCompass(dropped) || isKitSelectorChest(dropped) || isKitAbilityItem(dropped)) {
            event.setCancelled(true);
            event.getPlayer().sendActionBar(Component.text("🔒 Este item é fixo do QuestCodex/Kit e não pode ser dropado!", NamedTextColor.RED));
        }
    }

    // =========================================================================
    // MORTE JUSTA (BAÚ COM 30% DOS ITENS + DESGASTE DE DURABILIDADE) E ROUBO PVP EQUILIBRADO
    // =========================================================================

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();

        event.setKeepInventory(true);
        event.getDrops().clear();
        event.setKeepLevel(true);
        event.setDroppedExp(0);

        // Sistema de Caça aos Pontos Revisado (Zero Inflação de Pontos!)
        Player killer = victim.getKiller();
        if (killer != null && !killer.getUniqueId().equals(victim.getUniqueId()) && getConfig().getBoolean("partida.ativa", false)) {
            String pairKey = killer.getName().toLowerCase() + "->" + victim.getName().toLowerCase();
            long now = System.currentTimeMillis();
            long lastKill = bountyKillCooldown.getOrDefault(pairKey, 0L);

            int victimPts = getPlayerPoints(victim.getName());

            if (now - lastKill < 60_000L) {
                killer.sendMessage(Component.text("⚠ Você abateu " + victim.getName() + " há menos de 60s! Aguarde para roubar pontos dele novamente.", NamedTextColor.YELLOW));
            } else if (victimPts <= 0) {
                killer.sendMessage(Component.text("⚠ " + victim.getName() + " tem 0 pontos! Nenhum ponto foi roubado, mas você pode saquear o baú de espólios dele!", NamedTextColor.YELLOW));
            } else {
                bountyKillCooldown.put(pairKey, now);
                int stealPercent = getConfig().getInt("partida.porcentagem-roubo-pontos-pvp", 15);
                int stolen = Math.max(5, (victimPts * stealPercent) / 100);
                int actualTransfer = Math.min(victimPts, Math.min(120, stolen)); // Teto máximo de 120 pts roubados por kill

                // Se a vítima for o Líder isolado da partida, o caçador ganha um Bônus de Recompensa (+15 pts da banca)!
                List<Map.Entry<String, Integer>> top1 = getTopPlayers(1);
                boolean isLeaderKill = !top1.isEmpty() && top1.get(0).getKey().equalsIgnoreCase(victim.getName()) && victimPts >= 50;
                int leaderBountyBonus = isLeaderKill ? 15 : 0;

                setPlayerPoints(victim.getName(), victimPts - actualTransfer);
                setPlayerPoints(killer.getName(), getPlayerPoints(killer.getName()) + actualTransfer + leaderBountyBonus);

                Bukkit.broadcast(
                        Component.text("🗡 [CAÇADOR DE RECOMPENSAS] ", NamedTextColor.RED).decorate(TextDecoration.BOLD)
                                .append(Component.text(killer.getName(), NamedTextColor.YELLOW).decorate(TextDecoration.BOLD))
                                .append(Component.text(" abateu " + (isLeaderKill ? "o LÍDER " : ""), NamedTextColor.WHITE).decoration(TextDecoration.BOLD, false))
                                .append(Component.text(victim.getName(), NamedTextColor.GOLD).decorate(TextDecoration.BOLD))
                                .append(Component.text(" e roubou ", NamedTextColor.WHITE).decoration(TextDecoration.BOLD, false))
                                .append(Component.text("+" + actualTransfer + " pts", NamedTextColor.AQUA).decorate(TextDecoration.BOLD))
                                .append(isLeaderKill ? Component.text(" (+" + leaderBountyBonus + " Bônus Líder!)", NamedTextColor.GREEN).decorate(TextDecoration.BOLD) : Component.empty())
                );
                killer.playSound(killer.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 0.8f);
            }
        }

        // Desgasta 20% da durabilidade máxima das armas/armaduras/ferramentas e separa 30% dos itens para o Baú de Morte
        Inventory inv = victim.getInventory();
        List<Integer> stealableSlots = new ArrayList<>();
        int durabilityPenaltyPct = getConfig().getInt("partida.porcentagem-dano-durabilidade-morte", 20);

        for (int i = 0; i < inv.getSize(); i++) {
            ItemStack item = inv.getItem(i);
            if (item == null || item.getType() == Material.AIR) continue;
            if (isCodexBook(item) || isHunterCompass(item) || isKitSelectorChest(item) || isKitAbilityItem(item)) continue;

            if (item.getItemMeta() instanceof Damageable dmgMeta && item.getType().getMaxDurability() > 0) {
                int maxDur = item.getType().getMaxDurability();
                int penalty = Math.max(1, (maxDur * durabilityPenaltyPct) / 100);
                int newDamage = Math.min(maxDur - 1, dmgMeta.getDamage() + penalty);
                dmgMeta.setDamage(newDamage);
                item.setItemMeta((ItemMeta) dmgMeta);
            }

            // Não rouba a armadura que está vestida nos slots 36..39, apenas desgasta a durabilidade dela!
            if (i < 36) {
                stealableSlots.add(i);
            }
        }

        Collections.shuffle(stealableSlots, random);
        int lossPct = getConfig().getInt("partida.porcentagem-perda-itens-morte", 30);
        int slotsToTake = Math.max(1, (int) Math.ceil((stealableSlots.size() * lossPct) / 100.0));
        if (stealableSlots.isEmpty()) slotsToTake = 0;

        List<ItemStack> chestLoot = new ArrayList<>();
        for (int i = 0; i < slotsToTake && i < stealableSlots.size(); i++) {
            int slot = stealableSlots.get(i);
            ItemStack stack = inv.getItem(slot);
            if (stack == null) continue;

            if (stack.getAmount() > 1) {
                int takeAmount = Math.max(1, (stack.getAmount() * 40) / 100);
                ItemStack cloned = stack.clone();
                cloned.setAmount(takeAmount);
                chestLoot.add(cloned);
                stack.setAmount(stack.getAmount() - takeAmount);
                inv.setItem(slot, stack);
            } else {
                chestLoot.add(stack.clone());
                inv.setItem(slot, null);
            }
        }

        if (!chestLoot.isEmpty()) {
            Location deathLoc = victim.getLocation();
            Block targetBlock = findSafeChestBlock(deathLoc);
            targetBlock.setType(Material.CHEST);
            if (targetBlock.getState() instanceof Chest chest) {
                chest.customName(Component.text("💀 Espólios de " + victim.getName(), NamedTextColor.RED).decorate(TextDecoration.BOLD));
                chest.update();
                for (ItemStack loot : chestLoot) {
                    chest.getBlockInventory().addItem(loot);
                }
            }
            victim.sendMessage(
                    Component.text("💀 [MORTE] ", NamedTextColor.RED).decorate(TextDecoration.BOLD)
                            .append(Component.text("30% da sua mochila ficou em um ", NamedTextColor.GRAY).decoration(TextDecoration.BOLD, false))
                            .append(Component.text("Baú em (" + targetBlock.getX() + ", " + targetBlock.getY() + ", " + targetBlock.getZ() + ")", NamedTextColor.YELLOW).decorate(TextDecoration.BOLD))
                            .append(Component.text(" e seus equipamentos perderam 20% de vida útil!", NamedTextColor.GRAY).decoration(TextDecoration.BOLD, false))
            );
        }
    }

    private Block findSafeChestBlock(Location loc) {
        Block b = loc.getBlock();
        if (b.getType() == Material.AIR || b.isReplaceable()) {
            return b;
        }
        for (int dy = 0; dy <= 3; dy++) {
            Block up = b.getRelative(0, dy, 0);
            if (up.getType() == Material.AIR || up.isReplaceable()) {
                return up;
            }
        }
        return b;
    }

    // =========================================================================
    // RESPAWN NO COLISEU (30s IMUNE + 15s SPEED II + AVISO PARA DESCER + SEM ATACAR)
    // =========================================================================

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        Location skySpawn = getColiseumSpawnLocation();
        if (skySpawn != null) {
            event.setRespawnLocation(skySpawn);
        }

        Player player = event.getPlayer();
        int respImmunity = getConfig().getInt("partida.respawn-invencibilidade-segundos", 30);
        int respSpeed2 = getConfig().getInt("partida.respawn-speed2-segundos", 15);

        if (gatesOpen) {
            respawnProtectionSeconds.put(player.getUniqueId(), respImmunity);
        }

        Bukkit.getScheduler().runTaskLater(this, () -> {
            giveEssentialItems(player);
            if (gatesOpen) {
                player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, respSpeed2 * 20, 1, false, true, true));
                Title respawnTitle = Title.title(
                        Component.text("⚡ DESÇA DO COLISEU! ⚡", NamedTextColor.GOLD).decorate(TextDecoration.BOLD),
                        Component.text("30s de Invencibilidade (Sem atacar) + 15s de Speed II!", NamedTextColor.AQUA),
                        Title.Times.times(Duration.ofMillis(200), Duration.ofSeconds(4), Duration.ofSeconds(1))
                );
                player.showTitle(respawnTitle);
                player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.2f);
            }
        }, 5L);
    }

    private void tickPlayerStatesAndCompass() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            UUID uid = p.getUniqueId();

            // Passiva do Kit Poseidon quando está na água ou chuva
            String kitId = selectedKits.getOrDefault(uid, "kangaroo");
            if (gatesOpen && kitId.equals("poseidon") && p.isInWaterOrRain()) {
                p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 40, 1, false, false, true));
                p.addPotionEffect(new PotionEffect(PotionEffectType.WATER_BREATHING, 40, 0, false, false, true));
            }

            // Atualiza o contador de 30s de Invencibilidade pós-morte (Respawn)
            if (respawnProtectionSeconds.containsKey(uid)) {
                int rem = respawnProtectionSeconds.get(uid) - 1;
                if (rem <= 0) {
                    respawnProtectionSeconds.remove(uid);
                    p.sendMessage(Component.text("⚔ Sua proteção de respawn (30s) acabou! Você já pode atacar e receber dano!", NamedTextColor.YELLOW));
                    p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 1.0f);
                } else {
                    respawnProtectionSeconds.put(uid, rem);
                    if (isInsideColiseumBox(p.getLocation())) {
                        p.sendActionBar(
                                Component.text("⬇ DESÇA DO COLISEU! ", NamedTextColor.RED).decorate(TextDecoration.BOLD)
                                        .append(Component.text("Imune (Sem atacar): " + rem + "s", NamedTextColor.YELLOW))
                        );
                    } else {
                        p.sendActionBar(
                                Component.text("🛡 Proteção de Respawn (Sem atacar): ", NamedTextColor.AQUA)
                                        .append(Component.text(rem + "s", NamedTextColor.GREEN).decorate(TextDecoration.BOLD))
                        );
                    }
                }
            }

            // Atualiza a direção da Bússola Caça-Pontos
            Player target = resolveCompassTarget(p);
            if (target != null) {
                p.setCompassTarget(target.getLocation());
                ItemStack mainHand = p.getInventory().getItemInMainHand();
                ItemStack offHand = p.getInventory().getItemInOffHand();
                if ((isHunterCompass(mainHand) || isHunterCompass(offHand))
                        && invincibilityRemainingSeconds <= 0
                        && !respawnProtectionSeconds.containsKey(uid)) {
                    int dist = (int) p.getLocation().distance(target.getLocation());
                    int tPts = getPlayerPoints(target.getName());
                    p.sendActionBar(
                            Component.text("🧭 Caçando: ", NamedTextColor.RED).decorate(TextDecoration.BOLD)
                                    .append(Component.text(target.getName(), NamedTextColor.YELLOW).decorate(TextDecoration.BOLD))
                                    .append(Component.text(" (" + tPts + " pts) ", NamedTextColor.AQUA).decoration(TextDecoration.BOLD, false))
                                    .append(Component.text("➜ " + dist + "m", NamedTextColor.GREEN).decorate(TextDecoration.BOLD))
                    );
                }
            }
        }
    }

    // =========================================================================
    // CONSTRUÇÃO DO COLISEU NAS ALTURAS, PÓDIO E MUNDO (700x700 = 350 DE RAIO)
    // =========================================================================

    private void setupWorldAndColiseum() {
        World overworld = Bukkit.getWorld("world");
        if (overworld == null) return;

        for (World w : Bukkit.getWorlds()) {
            w.setDifficulty(Difficulty.HARD);
        }

        Location currentSpawn = overworld.getSpawnLocation();
        coliseumCenterX = currentSpawn.getBlockX();
        coliseumCenterZ = currentSpawn.getBlockZ();
        coliseumFloorY = getConfig().getInt("coliseu.altura-y", 165);
        coliseumRadius = getConfig().getInt("coliseu.raio", 16);

        int borderSize = getConfig().getInt("partida.borda-mapa-blocos", 700);
        WorldBorder border = overworld.getWorldBorder();
        border.setCenter(coliseumCenterX, coliseumCenterZ);
        border.setSize(borderSize);

        buildSkyColiseum(overworld);
        setColiseumGates(overworld, gatesOpen);

        Location skySpawn = new Location(overworld, coliseumCenterX + 0.5, coliseumFloorY + 1.0, coliseumCenterZ + 0.5, 0f, 0f);
        overworld.setSpawnLocation(skySpawn);

        ensureEndPortalShrine(overworld, coliseumCenterX, coliseumCenterZ);
    }

    public Location getColiseumSpawnLocation() {
        World overworld = Bukkit.getWorld("world");
        if (overworld == null) return null;
        return new Location(overworld, coliseumCenterX + 0.5, coliseumFloorY + 1.0, coliseumCenterZ + 0.5, 0f, 0f);
    }

    private void buildSkyColiseum(World world) {
        int cx = coliseumCenterX;
        int cz = coliseumCenterZ;
        int cy = coliseumFloorY;
        int R = coliseumRadius;

        if (world.getBlockAt(cx, cy, cz).getType() == Material.CHISELED_QUARTZ_BLOCK) {
            return;
        }

        getLogger().info("Construindo o Coliseu nas alturas em (" + cx + ", " + cy + ", " + cz + ")...");

        for (int dx = -R - 1; dx <= R + 1; dx++) {
            for (int dz = -R - 1; dz <= R + 1; dz++) {
                double dist = Math.sqrt(dx * dx + dz * dz);
                if (dist > R + 0.8) continue;

                int x = cx + dx;
                int z = cz + dz;

                for (int dy = 1; dy <= 12; dy++) {
                    world.getBlockAt(x, cy + dy, z).setType(Material.AIR);
                }

                if (dx == 0 && dz == 0) {
                    world.getBlockAt(x, cy, z).setType(Material.CHISELED_QUARTZ_BLOCK);
                    world.getBlockAt(x, cy - 1, z).setType(Material.SEA_LANTERN);
                } else if (dist <= 3.5) {
                    world.getBlockAt(x, cy, z).setType(Material.TINTED_GLASS);
                } else if (Math.abs(dist - 8.0) < 0.7 || Math.abs(dist - 12.0) < 0.7) {
                    world.getBlockAt(x, cy, z).setType(Material.GOLD_BLOCK);
                } else if ((Math.abs(dx) + Math.abs(dz)) % 5 == 0) {
                    world.getBlockAt(x, cy, z).setType(Material.SEA_LANTERN);
                    world.getBlockAt(x, cy + 1, z).setType(Material.WHITE_CARPET);
                } else {
                    world.getBlockAt(x, cy, z).setType(Material.SMOOTH_QUARTZ);
                }

                if (dist > 3.5) {
                    world.getBlockAt(x, cy - 1, z).setType(Material.STONE_BRICKS);
                }

                if (dist >= R - 1.2 && dist <= R + 0.6) {
                    boolean isGateOpening = (Math.abs(dx) <= 2 && Math.abs(dz) >= R - 2)
                            || (Math.abs(dz) <= 2 && Math.abs(dx) >= R - 2);

                    for (int dy = 1; dy <= 8; dy++) {
                        Block wallBlock = world.getBlockAt(x, cy + dy, z);
                        if (isGateOpening && dy <= 4) {
                            wallBlock.setType(Material.IRON_BARS);
                        } else if (dy == 8) {
                            wallBlock.setType(((dx + dz) % 2 == 0) ? Material.STONE_BRICK_SLAB : Material.GOLD_BLOCK);
                        } else if (dy == 5) {
                            wallBlock.setType(Material.CHISELED_STONE_BRICKS);
                        } else if ((dx % 4 == 0 || dz % 4 == 0)) {
                            wallBlock.setType(Material.QUARTZ_PILLAR);
                        } else if (dy >= 3 && dy <= 6 && !isGateOpening) {
                            wallBlock.setType(Material.IRON_BARS);
                        } else {
                            wallBlock.setType(Material.STONE_BRICKS);
                        }
                    }
                }
            }
        }
    }

    public void setColiseumGates(World world, boolean open) {
        this.gatesOpen = open;
        int cx = coliseumCenterX;
        int cz = coliseumCenterZ;
        int cy = coliseumFloorY;
        int R = coliseumRadius;

        Material gateMaterial = open ? Material.AIR : Material.IRON_BARS;

        for (int dx = -R - 1; dx <= R + 1; dx++) {
            for (int dz = -R - 1; dz <= R + 1; dz++) {
                double dist = Math.sqrt(dx * dx + dz * dz);
                if (dist >= R - 1.5 && dist <= R + 0.8) {
                    boolean isGateOpening = (Math.abs(dx) <= 2 && Math.abs(dz) >= R - 2)
                            || (Math.abs(dz) <= 2 && Math.abs(dx) >= R - 2);
                    if (isGateOpening) {
                        for (int dy = 1; dy <= 4; dy++) {
                            world.getBlockAt(cx + dx, cy + dy, cz + dz).setType(gateMaterial);
                        }
                        world.getBlockAt(cx + dx, cy, cz + dz).setType(open ? Material.EMERALD_BLOCK : Material.REDSTONE_BLOCK);
                    }
                }
            }
        }
    }

    public boolean canBypassColiseumLock(Player player) {
        if (player.isOp() || player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR) {
            return true;
        }
        String tagId = getPlayerTagInfo(player.getName()).id.toLowerCase();
        return tagId.equals("dono") || tagId.equals("mod") || tagId.equals("admin") || tagId.equals("adm") || tagId.equals("dev");
    }

    public boolean isInsideColiseumBox(Location loc) {
        if (loc == null || loc.getWorld() == null) return false;
        if (loc.getWorld().getEnvironment() != World.Environment.NORMAL) return false;
        double dx = loc.getX() - (coliseumCenterX + 0.5);
        double dz = loc.getZ() - (coliseumCenterZ + 0.5);
        double dist = Math.sqrt(dx * dx + dz * dz);
        return dist <= (coliseumRadius - 0.5) && loc.getY() >= (coliseumFloorY - 2) && loc.getY() <= (coliseumFloorY + 15);
    }

    private void ensureEndPortalShrine(World world, int spawnX, int spawnZ) {
        int cx = spawnX;
        int cz = spawnZ - 30;
        int cy = getGroundSurfaceY(world, cx, cz);

        if (world.getBlockAt(cx, cy + 1, cz - 2).getType() == Material.END_PORTAL_FRAME) {
            return;
        }

        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                world.getBlockAt(cx + dx, cy, cz + dz).setType(Material.STONE_BRICKS);
                for (int dy = 1; dy <= 4; dy++) {
                    world.getBlockAt(cx + dx, cy + dy, cz + dz).setType(Material.AIR);
                }
            }
        }
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                world.getBlockAt(cx + dx, cy, cz + dz).setType(Material.LAVA);
            }
        }

        placePortalFrame(world, cx - 1, cy + 1, cz - 2, BlockFace.SOUTH);
        placePortalFrame(world, cx,     cy + 1, cz - 2, BlockFace.SOUTH);
        placePortalFrame(world, cx + 1, cy + 1, cz - 2, BlockFace.SOUTH);

        placePortalFrame(world, cx - 1, cy + 1, cz + 2, BlockFace.NORTH);
        placePortalFrame(world, cx,     cy + 1, cz + 2, BlockFace.NORTH);
        placePortalFrame(world, cx + 1, cy + 1, cz + 2, BlockFace.NORTH);

        placePortalFrame(world, cx - 2, cy + 1, cz - 1, BlockFace.EAST);
        placePortalFrame(world, cx - 2, cy + 1, cz,     BlockFace.EAST);
        placePortalFrame(world, cx - 2, cy + 1, cz + 1, BlockFace.EAST);

        placePortalFrame(world, cx + 2, cy + 1, cz - 1, BlockFace.WEST);
        placePortalFrame(world, cx + 2, cy + 1, cz,     BlockFace.WEST);
        placePortalFrame(world, cx + 2, cy + 1, cz + 1, BlockFace.WEST);
    }

    private int getGroundSurfaceY(World world, int x, int z) {
        for (int y = coliseumFloorY - 5; y > world.getMinHeight(); y--) {
            Material m = world.getBlockAt(x, y, z).getType();
            if (m.isSolid() && m != Material.BARRIER) {
                return y;
            }
        }
        return 64;
    }

    private void placePortalFrame(World world, int x, int y, int z, BlockFace facing) {
        Block b = world.getBlockAt(x, y, z);
        b.setType(Material.END_PORTAL_FRAME);
        if (b.getBlockData() instanceof org.bukkit.block.data.type.EndPortalFrame frame) {
            frame.setFacing(facing);
            frame.setEye(false);
            b.setBlockData(frame);
        }
    }

    // =========================================================================
    // CELEBRAÇÃO ÉPICA NO PÓDIO COM FOGOS + FECHAMENTO E RESET DO MAPA
    // =========================================================================

    private void startEpicWinnerCeremony() {
        if (celebratingWinner) return;
        celebratingWinner = true;
        timerRunning = false;
        invincibilityRemainingSeconds = 0;
        respawnProtectionSeconds.clear();
        getConfig().set("partida.ativa", false);
        saveConfig();

        World overworld = Bukkit.getWorld("world");
        if (overworld != null) {
            overworld.setTime(6000L);
            setColiseumGates(overworld, false);
            buildWinnerPodium(overworld);
        }

        List<Map.Entry<String, Integer>> top3 = getTopPlayers(3);
        String firstName = top3.size() >= 1 ? top3.get(0).getKey() : "Ninguém";
        int firstPts = top3.size() >= 1 ? top3.get(0).getValue() : 0;
        String secondName = top3.size() >= 2 ? top3.get(1).getKey() : null;
        String thirdName = top3.size() >= 3 ? top3.get(2).getKey() : null;

        Bukkit.broadcast(Component.empty());
        Bukkit.broadcast(Component.text("=============================================", NamedTextColor.GOLD).decorate(TextDecoration.BOLD));
        Bukkit.broadcast(Component.text("👑 CERIMÔNIA DE COROAÇÃO DO QUEST CODEX! 👑", NamedTextColor.GOLD).decorate(TextDecoration.BOLD));
        Bukkit.broadcast(
                Component.text("🏆 1º LUGAR (CAMPEÃO): ", NamedTextColor.YELLOW).decorate(TextDecoration.BOLD)
                        .append(Component.text(firstName.toUpperCase() + " — " + firstPts + " pts!", NamedTextColor.GREEN).decorate(TextDecoration.BOLD))
        );
        if (secondName != null) {
            Bukkit.broadcast(Component.text("🥈 2º Lugar: " + secondName + " (" + top3.get(1).getValue() + " pts)", NamedTextColor.GRAY));
        }
        if (thirdName != null) {
            Bukkit.broadcast(Component.text("🥉 3º Lugar: " + thirdName + " (" + top3.get(2).getValue() + " pts)", NamedTextColor.GOLD));
        }
        Bukkit.broadcast(Component.text("=============================================", NamedTextColor.GOLD).decorate(TextDecoration.BOLD));

        showRankingChat(Bukkit.getConsoleSender());

        Title ceremonyTitle = Title.title(
                Component.text("👑 CAMPEÃO: " + firstName.toUpperCase() + " 👑", NamedTextColor.GOLD).decorate(TextDecoration.BOLD),
                Component.text("Com " + firstPts + " pontos! Parabéns!", NamedTextColor.YELLOW),
                Title.Times.times(Duration.ofMillis(400), Duration.ofSeconds(10), Duration.ofSeconds(2))
        );

        if (overworld != null) {
            Location goldSpot = new Location(overworld, coliseumCenterX + 0.5, coliseumFloorY + 4.0, coliseumCenterZ - 3.5, 0f, 0f);
            Location silverSpot = new Location(overworld, coliseumCenterX - 2.5, coliseumFloorY + 3.0, coliseumCenterZ - 3.5, 0f, 0f);
            Location bronzeSpot = new Location(overworld, coliseumCenterX + 3.5, coliseumFloorY + 2.0, coliseumCenterZ - 3.5, 0f, 0f);
            Location audienceSpot = new Location(overworld, coliseumCenterX + 0.5, coliseumFloorY + 1.0, coliseumCenterZ + 5.5, 180f, -10f);

            for (Player p : Bukkit.getOnlinePlayers()) {
                p.setHealth(20.0);
                p.setFoodLevel(20);
                p.showTitle(ceremonyTitle);
                p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);

                if (p.getName().equalsIgnoreCase(firstName)) {
                    p.teleport(goldSpot);
                    p.getInventory().setHelmet(new ItemStack(Material.GOLDEN_HELMET));
                    p.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 20 * 25, 0, false, true, true));
                } else if (secondName != null && p.getName().equalsIgnoreCase(secondName)) {
                    p.teleport(silverSpot);
                } else if (thirdName != null && p.getName().equalsIgnoreCase(thirdName)) {
                    p.teleport(bronzeSpot);
                } else {
                    p.teleport(audienceSpot);
                }
            }

            for (int sec = 1; sec <= 15; sec++) {
                Bukkit.getScheduler().runTaskLater(this, () -> {
                    spawnCelebrationFirework(new Location(overworld, coliseumCenterX + 0.5, coliseumFloorY + 4.5, coliseumCenterZ - 3.5));
                    spawnCelebrationFirework(new Location(overworld, coliseumCenterX - 4.5, coliseumFloorY + 2.0, coliseumCenterZ - 3.5));
                    spawnCelebrationFirework(new Location(overworld, coliseumCenterX + 5.5, coliseumFloorY + 2.0, coliseumCenterZ - 3.5));
                }, sec * 20L);
            }
        }

        Bukkit.getScheduler().runTaskLater(this, () -> {
            Bukkit.broadcast(Component.text("♻ [QUEST CODEX] O servidor será fechado em 5 segundos e o mapa será resetado!", NamedTextColor.RED).decorate(TextDecoration.BOLD));
        }, 13 * 20L);

        Bukkit.getScheduler().runTaskLater(this, () -> {
            try {
                File flagFile = new File(Bukkit.getWorldContainer(), "reset-world.flag");
                Files.writeString(flagFile.toPath(), "reset");
                getConfig().set("pontuacao", null);
                getConfig().set("conquistas-jogador", null);
                getConfig().set("primeiro-conquista", null);
                getConfig().set("partida.ativa", false);
                saveConfig();
            } catch (IOException e) {
                getLogger().warning("Falha ao criar reset-world.flag: " + e.getMessage());
            }

            for (Player p : Bukkit.getOnlinePlayers()) {
                p.kick(
                        Component.text("🏆 FIM DA PARTIDA! 🏆\n\n", NamedTextColor.GOLD).decorate(TextDecoration.BOLD)
                                .append(Component.text("Grande Campeão: " + firstName.toUpperCase() + " (" + firstPts + " pts)\n\n", NamedTextColor.GREEN))
                                .append(Component.text("O servidor foi encerrado e o mapa foi resetado para a próxima partida!", NamedTextColor.YELLOW).decoration(TextDecoration.BOLD, false))
                );
            }
            Bukkit.shutdown();
        }, 18 * 20L);
    }

    private void buildWinnerPodium(World world) {
        int cx = coliseumCenterX;
        int cy = coliseumFloorY;
        int cz = coliseumCenterZ - 4;

        for (int dy = 1; dy <= 3; dy++) {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    world.getBlockAt(cx + dx, cy + dy, cz + dz).setType(Material.GOLD_BLOCK);
                }
            }
        }
        world.getBlockAt(cx, cy + 3, cz).setType(Material.SEA_LANTERN);
        world.getBlockAt(cx, cy + 4, cz).setType(Material.YELLOW_CARPET);

        for (int dy = 1; dy <= 2; dy++) {
            for (int dx = -4; dx <= -2; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    world.getBlockAt(cx + dx, cy + dy, cz + dz).setType(Material.IRON_BLOCK);
                }
            }
        }

        for (int dx = 2; dx <= 4; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                world.getBlockAt(cx + dx, cy + 1, cz + dz).setType(Material.COPPER_BLOCK);
            }
        }
    }

    private void spawnCelebrationFirework(Location loc) {
        if (loc.getWorld() == null) return;
        Firework fw = (Firework) loc.getWorld().spawnEntity(loc, EntityType.FIREWORK_ROCKET);
        FireworkMeta fwm = fw.getFireworkMeta();
        Color[] colors = {Color.YELLOW, Color.ORANGE, Color.AQUA, Color.LIME, Color.RED, Color.FUCHSIA};
        Color c1 = colors[random.nextInt(colors.length)];
        Color c2 = colors[random.nextInt(colors.length)];
        fwm.addEffect(
                FireworkEffect.builder()
                        .with(FireworkEffect.Type.BALL_LARGE)
                        .withColor(c1, c2)
                        .withFade(Color.WHITE)
                        .flicker(true)
                        .trail(true)
                        .build()
        );
        fwm.setPower(1);
        fw.setFireworkMeta(fwm);
    }

    // =========================================================================
    // CRONÔMETRO DE 45 MINUTOS E INVENCIBILIDADE GLOBAL DE 2 MINUTOS
    // =========================================================================

    private void tickMatchTimer() {
        if (!timerRunning || !getConfig().getBoolean("partida.ativa", false) || celebratingWinner) {
            return;
        }

        if (invincibilityRemainingSeconds > 0) {
            invincibilityRemainingSeconds--;

            for (Player p : Bukkit.getOnlinePlayers()) {
                p.sendActionBar(
                        Component.text("🛡 Invencibilidade + Speed I: ", NamedTextColor.GOLD).decorate(TextDecoration.BOLD)
                                .append(Component.text(formatTime(invincibilityRemainingSeconds), NamedTextColor.GREEN).decorate(TextDecoration.BOLD))
                );
            }

            if (invincibilityRemainingSeconds == 60 || invincibilityRemainingSeconds == 30 || invincibilityRemainingSeconds == 10) {
                Bukkit.broadcast(
                        Component.text("🛡 [QUEST CODEX] ", NamedTextColor.GOLD).decorate(TextDecoration.BOLD)
                                .append(Component.text("A invencibilidade termina em ", NamedTextColor.YELLOW).decoration(TextDecoration.BOLD, false))
                                .append(Component.text(invincibilityRemainingSeconds + " segundos", NamedTextColor.RED).decorate(TextDecoration.BOLD))
                                .append(Component.text("!", NamedTextColor.YELLOW).decoration(TextDecoration.BOLD, false))
                );
                for (Player p : Bukkit.getOnlinePlayers()) {
                    p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.5f);
                }
            } else if (invincibilityRemainingSeconds == 0) {
                Bukkit.broadcast(
                        Component.text("⚔ [QUEST CODEX] ", NamedTextColor.RED).decorate(TextDecoration.BOLD)
                                .append(Component.text("A invencibilidade de 2 minutos ACABOU! O PvP e a Caça aos Pontos começaram!", NamedTextColor.YELLOW).decoration(TextDecoration.BOLD, false))
                );
                for (Player p : Bukkit.getOnlinePlayers()) {
                    p.playSound(p.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 0.7f, 1.2f);
                }
            }
        }

        if (matchRemainingSeconds > 0) {
            matchRemainingSeconds--;

            if (matchRemainingSeconds == 30 * 60 || matchRemainingSeconds == 15 * 60
                    || matchRemainingSeconds == 10 * 60 || matchRemainingSeconds == 5 * 60
                    || matchRemainingSeconds == 60) {
                int mins = matchRemainingSeconds / 60;
                Bukkit.broadcast(
                        Component.text("⏳ [QUEST CODEX] ", NamedTextColor.GOLD).decorate(TextDecoration.BOLD)
                                .append(Component.text("Restam ", NamedTextColor.YELLOW).decoration(TextDecoration.BOLD, false))
                                .append(Component.text(mins + (mins == 1 ? " minuto" : " minutos"), NamedTextColor.RED).decorate(TextDecoration.BOLD))
                                .append(Component.text(" para o fim da partida!", NamedTextColor.YELLOW).decoration(TextDecoration.BOLD, false))
                );
                for (Player p : Bukkit.getOnlinePlayers()) {
                    p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.2f);
                }
            } else if (matchRemainingSeconds <= 10 && matchRemainingSeconds > 0) {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    p.sendActionBar(
                            Component.text("⏳ Fim da partida em: ", NamedTextColor.YELLOW)
                                    .append(Component.text(matchRemainingSeconds + "s", NamedTextColor.RED).decorate(TextDecoration.BOLD))
                    );
                    p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 1.0f, 1.5f);
                }
            }

            if (matchRemainingSeconds == 0) {
                startEpicWinnerCeremony();
            }
        }
    }

    private String formatTime(int totalSeconds) {
        int mins = totalSeconds / 60;
        int secs = totalSeconds % 60;
        return String.format("%02d:%02d", mins, secs);
    }

    // =========================================================================
    // SAFE ZONE DO COLISEU, RESTRIÇÃO DE SAÍDA E BLOQUEIO DE ATAQUE NO RESPAWN
    // =========================================================================

    public boolean isInSafeZone(Location loc) {
        if (loc == null || loc.getWorld() == null) return false;
        if (!getConfig().getBoolean("safezone.ativada", true)) return false;
        if (loc.getWorld().getEnvironment() != World.Environment.NORMAL) return false;

        double dx = Math.abs(loc.getX() - coliseumCenterX);
        double dz = Math.abs(loc.getZ() - coliseumCenterZ);
        int radius = getConfig().getInt("safezone.raio-blocos", 25);

        return dx <= radius && dz <= radius && loc.getY() >= (coliseumFloorY - 4);
    }

    @EventHandler
    public void onPlayerMoveColiseumAndSafeZone(PlayerMoveEvent event) {
        if (event.getFrom().getBlockX() == event.getTo().getBlockX()
                && event.getFrom().getBlockY() == event.getTo().getBlockY()
                && event.getFrom().getBlockZ() == event.getTo().getBlockZ()) {
            return;
        }
        Player player = event.getPlayer();

        if (!gatesOpen && !celebratingWinner && !canBypassColiseumLock(player)) {
            if (!isInsideColiseumBox(event.getTo())) {
                Location spawnLoc = getColiseumSpawnLocation();
                if (spawnLoc != null) {
                    player.teleport(spawnLoc);
                    player.sendActionBar(
                            Component.text("🔒 Os portões do Coliseu estão fechados! Aguarde o início da partida!", NamedTextColor.RED)
                    );
                    player.playSound(player.getLocation(), Sound.BLOCK_IRON_DOOR_CLOSE, 0.8f, 1.0f);
                    return;
                }
            }
        }

        boolean nowInSafe = isInSafeZone(event.getTo());
        boolean wasInSafe = insideSafeZone.contains(player.getUniqueId());

        if (nowInSafe && !wasInSafe) {
            insideSafeZone.add(player.getUniqueId());
            player.sendActionBar(
                    Component.text("🏛 Você está no ", NamedTextColor.GREEN)
                            .append(Component.text("Coliseu (Safe Zone)", NamedTextColor.GOLD).decorate(TextDecoration.BOLD))
            );
        } else if (!nowInSafe && wasInSafe) {
            insideSafeZone.remove(player.getUniqueId());
            if (invincibilityRemainingSeconds > 0) {
                player.sendActionBar(
                        Component.text("🛡 Você saltou do Coliseu! Invencibilidade ativa por ", NamedTextColor.GREEN)
                                .append(Component.text(formatTime(invincibilityRemainingSeconds), NamedTextColor.GOLD).decorate(TextDecoration.BOLD))
                );
            } else if (respawnProtectionSeconds.containsKey(player.getUniqueId())) {
                int rem = respawnProtectionSeconds.get(player.getUniqueId());
                player.sendActionBar(
                        Component.text("🛡 Você saltou do Coliseu! Imune (Sem atacar) por ", NamedTextColor.AQUA)
                                .append(Component.text(rem + "s", NamedTextColor.GREEN).decorate(TextDecoration.BOLD))
                );
            } else {
                player.sendActionBar(
                        Component.text("⚔ Você saiu do Coliseu — Survival e PvP Ativados!", NamedTextColor.YELLOW)
                );
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDamageProtected(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player victim) {
            if (!gatesOpen || celebratingWinner || invincibilityRemainingSeconds > 0
                    || respawnProtectionSeconds.containsKey(victim.getUniqueId())
                    || isInSafeZone(victim.getLocation())) {
                event.setCancelled(true);
                return;
            }

            String kitId = selectedKits.getOrDefault(victim.getUniqueId(), "kangaroo");

            // Passiva do Kit Fireman: 100% imune a Fogo, Lava e Bloco de Magma
            if (kitId.equals("fireman") && (event.getCause() == EntityDamageEvent.DamageCause.FIRE
                    || event.getCause() == EntityDamageEvent.DamageCause.FIRE_TICK
                    || event.getCause() == EntityDamageEvent.DamageCause.LAVA
                    || event.getCause() == EntityDamageEvent.DamageCause.HOT_FLOOR)) {
                event.setCancelled(true);
                victim.setFireTicks(0);
                return;
            }

            // Proteção contra dano de queda de Kits de mobilidade (Kangaroo, Stomper, Grappler, Phantom, Flash)
            if (event.getCause() == EntityDamageEvent.DamageCause.FALL) {
                long noFallUntil = noFallDamageUntil.getOrDefault(victim.getUniqueId(), 0L);
                if (System.currentTimeMillis() < noFallUntil) {
                    event.setCancelled(true);
                } else if (kitId.equals("kangaroo")) {
                    event.setDamage(Math.min(event.getDamage(), 3.0));
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPvpAndAttackProtected(EntityDamageByEntityEvent event) {
        Player attacker = null;
        if (event.getDamager() instanceof Player p) {
            attacker = p;
        } else if (event.getDamager() instanceof Projectile proj && proj.getShooter() instanceof Player p) {
            attacker = p;
        }

        if (attacker != null) {
            if (!gatesOpen || celebratingWinner || invincibilityRemainingSeconds > 0) {
                if (event.getEntity() instanceof Player) {
                    event.setCancelled(true);
                    attacker.sendActionBar(Component.text("🛡 PvP desativado durante a invencibilidade inicial!", NamedTextColor.RED));
                    return;
                }
            }
            if (respawnProtectionSeconds.containsKey(attacker.getUniqueId())) {
                event.setCancelled(true);
                int rem = respawnProtectionSeconds.get(attacker.getUniqueId());
                attacker.sendActionBar(Component.text("🚫 Você não pode atacar ninguém enquanto estiver com proteção de Respawn (" + rem + "s)!", NamedTextColor.RED));
                return;
            }
            if (isInSafeZone(attacker.getLocation())) {
                event.setCancelled(true);
                attacker.sendActionBar(Component.text("🛡 Você não pode atacar de dentro do Coliseu!", NamedTextColor.RED));
                return;
            }
        }

        if (event.getEntity() instanceof Player victim) {
            if (!gatesOpen || celebratingWinner || invincibilityRemainingSeconds > 0
                    || respawnProtectionSeconds.containsKey(victim.getUniqueId())
                    || isInSafeZone(victim.getLocation())) {
                event.setCancelled(true);
                if (attacker != null) {
                    attacker.sendActionBar(Component.text("🛡 Este jogador está com Invencibilidade ativa!", NamedTextColor.RED));
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onHungerProtected(FoodLevelChangeEvent event) {
        if (event.getEntity() instanceof Player player) {
            if ((!gatesOpen || celebratingWinner || invincibilityRemainingSeconds > 0
                    || respawnProtectionSeconds.containsKey(player.getUniqueId())
                    || isInSafeZone(player.getLocation()))
                    && event.getFoodLevel() < player.getFoodLevel()) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockPlaceInSafeZone(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        if (isKitAbilityItem(event.getItemInHand()) || isKitSelectorChest(event.getItemInHand())) {
            event.setCancelled(true);
            return;
        }
        if (isInSafeZone(event.getBlock().getLocation()) && player.getGameMode() != GameMode.CREATIVE) {
            event.setCancelled(true);
            player.sendActionBar(Component.text("🛡 Você não pode construir no Coliseu!", NamedTextColor.RED));
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onExplodeInSafeZone(EntityExplodeEvent event) {
        event.blockList().removeIf(block -> isInSafeZone(block.getLocation()));
    }

    // =========================================================================
    // SISTEMA REVISADO E EQUILIBRADO DE PONTOS E CONQUISTAS (45 MINUTOS)
    // =========================================================================

    private void registerDefaultQuests() {
        quests.clear();

        // FILEIRA 1 (Slots 10..16): FÁCEIS (Início Rápido) — 15 a 25 pts (1º Lugar: +50%)
        addQuest("madeira", 10, Material.OAK_LOG, "Lenha na Fogueira",
                "Quebre 10 troncos de madeira de qualquer árvore.",
                "Fácil", NamedTextColor.GREEN, 15, 25);
        addQuest("bancada", 11, Material.CRAFTING_TABLE, "Mãos à Obra",
                "Crie ou tenha uma Bancada de Trabalho (Crafting Table).",
                "Fácil", NamedTextColor.GREEN, 15, 25);
        addQuest("picareta_pedra", 12, Material.STONE_PICKAXE, "Idade da Pedra",
                "Crie ou tenha uma Picareta de Pedra.",
                "Fácil", NamedTextColor.GREEN, 20, 30);
        addQuest("fornalha", 13, Material.FURNACE, "Fogo Alto",
                "Crie ou tenha uma Fornalha.",
                "Fácil", NamedTextColor.GREEN, 20, 30);
        addQuest("carvao", 14, Material.COAL, "Combustível Fóssil",
                "Minere ou obtenha Carvão.",
                "Fácil", NamedTextColor.GREEN, 20, 30);
        addQuest("pao", 15, Material.BREAD, "Padeiro da Vila",
                "Obtenha ou coma um Pão.",
                "Fácil", NamedTextColor.GREEN, 25, 40);
        addQuest("pescador", 16, Material.FISHING_ROD, "Dia de Pesca",
                "Pesque um peixe ou obtenha Bacalhau/Salmão.",
                "Fácil", NamedTextColor.GREEN, 25, 40);

        // FILEIRA 2 (Slots 19..25): MÉDIAS (Ferro e Combate) — 35 a 50 pts (1º Lugar: 55 a 75 pts)
        addQuest("ferro", 19, Material.IRON_INGOT, "Idade do Ferro",
                "Obtenha uma Barra de Ferro.",
                "Média", NamedTextColor.YELLOW, 35, 55);
        addQuest("escudo", 20, Material.SHIELD, "Defesa Implacável",
                "Crie ou equipe um Escudo de proteção.",
                "Média", NamedTextColor.YELLOW, 35, 55);
        addQuest("balde_lava", 21, Material.LAVA_BUCKET, "Quente Demais!",
                "Obtenha um Balde com Lava.",
                "Média", NamedTextColor.YELLOW, 40, 60);
        addQuest("cacador", 22, Material.IRON_SWORD, "Caçador da Noite",
                "Elimine 8 monstros hostis (Zumbis, Esqueletos, Aranhas, etc).",
                "Média", NamedTextColor.YELLOW, 45, 70);
        addQuest("ouro", 23, Material.GOLD_INGOT, "Febre do Ouro",
                "Obtenha uma Barra de Ouro.",
                "Média", NamedTextColor.YELLOW, 45, 70);
        addQuest("domador", 24, Material.BONE, "Melhor Amigo",
                "Dome um Lobo/Gato ou reproduza dois animais.",
                "Média", NamedTextColor.YELLOW, 50, 75);
        addQuest("redstone", 25, Material.REDSTONE, "Engenheiro Elétrico",
                "Minere ou obtenha pó de Redstone.",
                "Média", NamedTextColor.YELLOW, 45, 70);

        // FILEIRA 3 (Slots 28..34): DIFÍCEIS (Diamante e Nether) — 65 a 100 pts (1º Lugar: 100 a 150 pts)
        addQuest("diamante", 28, Material.DIAMOND, "Brilho Eterno!",
                "Minere ou obtenha o seu primeiro Diamante.",
                "Difícil", NamedTextColor.GOLD, 65, 100);
        addQuest("picareta_diamante", 29, Material.DIAMOND_PICKAXE, "Minerador Mestre",
                "Crie ou obtenha uma Picareta de Diamante.",
                "Difícil", NamedTextColor.GOLD, 75, 115);
        addQuest("obsidiana", 30, Material.OBSIDIAN, "Rocha Indestrutível",
                "Quebre ou colete um bloco de Obsidiana.",
                "Difícil", NamedTextColor.GOLD, 75, 115);
        addQuest("encantador", 31, Material.ENCHANTING_TABLE, "Mago Aprendiz",
                "Crie uma Mesa de Encantamentos ou encante um item.",
                "Difícil", NamedTextColor.GOLD, 85, 130);
        addQuest("nether", 32, Material.NETHERRACK, "Profundezas Ardentes",
                "Entre na dimensão do Nether.",
                "Difícil", NamedTextColor.GOLD, 90, 135);
        addQuest("blaze", 33, Material.BLAZE_ROD, "Chama Viva",
                "Derrote um Blaze ou obtenha uma Vara de Blaze.",
                "Difícil", NamedTextColor.GOLD, 100, 150);
        addQuest("maca_dourada", 34, Material.GOLDEN_APPLE, "Fruto Dourado",
                "Obtenha ou coma uma Maçã Dourada.",
                "Difícil", NamedTextColor.GOLD, 80, 120);

        // FILEIRA 4 (Slots 37..43): ÉPICAS / LENDÁRIAS (Endgame 45m) — 110 a 250 pts (Balanceado para não decidir sozinho!)
        addQuest("olho_ender", 37, Material.ENDER_EYE, "Visão do Fim",
                "Crie ou obtenha um Olho de Ender (Olho do Fim).",
                "Épica", NamedTextColor.LIGHT_PURPLE, 110, 165);
        addQuest("ancestral", 38, Material.ANCIENT_DEBRIS, "Relíquia Ancestral",
                "Minere ou obtenha um Detrito Ancestral no Nether.",
                "Épica", NamedTextColor.LIGHT_PURPLE, 130, 195);
        addQuest("batalha_pvp", 39, Material.DIAMOND_SWORD, "Caçador Implacável",
                "Abata um jogador rival em combate durante a partida!",
                "Épica", NamedTextColor.LIGHT_PURPLE, 120, 180);
        addQuest("netherite", 40, Material.NETHERITE_INGOT, "Forja Suprema",
                "Obtenha uma Barra de Netherite.",
                "Épica", NamedTextColor.LIGHT_PURPLE, 160, 240);
        addQuest("the_end", 41, Material.END_STONE, "Além do Horizonte",
                "Ative o Altar do End e entre na dimensão do The End.",
                "Lendária", NamedTextColor.RED, 170, 250);
        addQuest("wither_skull", 42, Material.WITHER_SKELETON_SKULL, "Relíquia Sombria",
                "Obtenha um Crânio de Esqueleto Wither ou Estrela do Nether.",
                "Lendária", NamedTextColor.RED, 190, 280);
        addQuest("matador_dragao", 43, Material.DRAGON_HEAD, "Lenda de QuestCodex",
                "Derrote o Ender Dragon!",
                "Lendária", NamedTextColor.RED, 250, 350);
    }

    private void addQuest(String id, int slot, Material icon, String title, String description,
                          String difficulty, NamedTextColor diffColor, int basePoints, int firstPoints) {
        quests.put(id, new QuestDef(id, slot, icon, title, description, difficulty, diffColor, basePoints, firstPoints));
    }

    // Verifica periodicamente o inventário do jogador (garante que itens pegos de baús de vilas/portais contem na hora!)
    private void scanPlayerInventoryForQuests(Player player) {
        if (!getConfig().getBoolean("partida.ativa", false) || celebratingWinner) return;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item == null || item.getType() == Material.AIR) continue;
            if (isKitAbilityItem(item) || isCodexBook(item) || isHunterCompass(item)) continue;
            checkItemAcquisition(player, item.getType());
        }
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

    public void openCodexGui(Player player) {
        Inventory inv = Bukkit.createInventory(
                null,
                54,
                Component.text(GUI_TITLE_CODEX, NamedTextColor.GOLD).decorate(TextDecoration.BOLD)
        );

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
            infoLore.add(Component.text("Tempo Restante: ", NamedTextColor.WHITE)
                    .append(Component.text(formatTime(matchRemainingSeconds), NamedTextColor.GREEN).decorate(TextDecoration.BOLD))
                    .decoration(TextDecoration.ITALIC, false));
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

    public void setPlayerPoints(String playerName, int pts) {
        getConfig().set("pontuacao." + playerName.toLowerCase(), Math.max(0, pts));
        saveConfig();
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
        if (!getConfig().getBoolean("partida.ativa", false) || celebratingWinner) {
            return;
        }

        QuestDef q = quests.get(questId);
        if (q == null) return;

        String pKey = player.getName().toLowerCase();
        String playerQuestsPath = "conquistas-jogador." + pKey;
        List<String> done = new ArrayList<>(getConfig().getStringList(playerQuestsPath));
        if (done.contains(questId)) {
            return;
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

        for (Player p : Bukkit.getOnlinePlayers()) {
            updatePlayerScoreboard(p);
        }
    }

    // =========================================================================
    // EVENTOS DE INTERAÇÃO E PROGRESSO DE CONQUISTAS
    // =========================================================================

    @EventHandler
    public void onInteractSpecialItems(PlayerInteractEvent event) {
        if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            ItemStack item = event.getItem();
            if (isCodexBook(item)) {
                event.setCancelled(true);
                openCodexGui(event.getPlayer());
            } else if (isKitSelectorChest(item)) {
                event.setCancelled(true);
                openKitSelectorGui(event.getPlayer());
            } else if (isHunterCompass(item)) {
                event.setCancelled(true);
                cycleCompassTarget(event.getPlayer());
            } else if (isKitAbilityItem(item)) {
                String kitId = getKitIdFromAbilityItem(item);
                if (!"fisherman".equals(kitId)) { // Fisherman usa o lançamento normal da vara de pesca
                    event.setCancelled(true);
                    activateKitAbility(event.getPlayer(), kitId);
                }
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

        if (plainTitle != null && plainTitle.contains(GUI_TITLE_KITS)) {
            event.setCancelled(true);
            if (event.getWhoClicked() instanceof Player player) {
                int slot = event.getRawSlot();
                for (KitDef kit : kits.values()) {
                    if (kit.slot == slot) {
                        selectedKits.put(player.getUniqueId(), kit.id);
                        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.2f);
                        player.sendMessage(
                                Component.text("✔ Você escolheu o ", NamedTextColor.GREEN)
                                        .append(Component.text("Kit " + kit.name, kit.color).decorate(TextDecoration.BOLD))
                                        .append(Component.text("! Você receberá o item ativador da habilidade quando os portões se abrirem.", NamedTextColor.GRAY))
                        );
                        player.closeInventory();
                        return;
                    }
                }
            }
            return;
        }

        if (event.getWhoClicked() instanceof Player player && event.getCurrentItem() != null) {
            if (!isKitAbilityItem(event.getCurrentItem())) {
                checkItemAcquisition(player, event.getCurrentItem().getType());
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();

        if (isInSafeZone(block.getLocation()) && player.getGameMode() != GameMode.CREATIVE) {
            event.setCancelled(true);
            player.sendActionBar(Component.text("🛡 Você não pode quebrar blocos no Coliseu!", NamedTextColor.RED));
            return;
        }

        Material type = block.getType();
        String name = type.name();
        String playerKit = selectedKits.getOrDefault(player.getUniqueId(), "kangaroo");

        if (name.endsWith("_LOG") || name.endsWith("_STEM")) {
            String key = player.getName().toLowerCase();
            int brokenCount = 1;

            // Passiva do Kit Lumberjack: derruba os troncos acima da árvore automaticamente!
            if (playerKit.equals("lumberjack")) {
                Block up = block.getRelative(BlockFace.UP);
                int limit = 0;
                while ((up.getType().name().endsWith("_LOG") || up.getType().name().endsWith("_STEM")) && limit < 12) {
                    up.breakNaturally();
                    brokenCount++;
                    limit++;
                    up = up.getRelative(BlockFace.UP);
                }
            }

            int count = blockBreakProgress.getOrDefault(key, 0) + brokenCount;
            blockBreakProgress.put(key, count);
            if (count >= 10) {
                completeQuest(player, "madeira");
            }
        } else if (name.endsWith("_ORE") || type == Material.ANCIENT_DEBRIS) {
            // Passiva do Kit Miner: quebra até 4 minérios adjacentes da mesma veia!
            if (playerKit.equals("miner") && type != Material.ANCIENT_DEBRIS) {
                int veinCount = 0;
                for (BlockFace face : Arrays.asList(BlockFace.UP, BlockFace.DOWN, BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST)) {
                    Block adj = block.getRelative(face);
                    if (adj.getType() == type && veinCount < 4) {
                        adj.breakNaturally(player.getInventory().getItemInMainHand());
                        veinCount++;
                    }
                }
            }

            if (type == Material.COAL_ORE || type == Material.DEEPSLATE_COAL_ORE) {
                completeQuest(player, "carvao");
            } else if (type == Material.REDSTONE_ORE || type == Material.DEEPSLATE_REDSTONE_ORE) {
                completeQuest(player, "redstone");
            } else if (type == Material.DIAMOND_ORE || type == Material.DEEPSLATE_DIAMOND_ORE) {
                completeQuest(player, "diamante");
            } else if (type == Material.ANCIENT_DEBRIS) {
                completeQuest(player, "ancestral");
            }
        } else if (type == Material.OBSIDIAN || type == Material.CRYING_OBSIDIAN) {
            completeQuest(player, "obsidiana");
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
            ItemStack stack = event.getItem().getItemStack();
            if (!isKitAbilityItem(stack)) {
                checkItemAcquisition(player, stack.getType());
            }
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
        Player player = event.getPlayer();
        if (event.getState() == PlayerFishEvent.State.CAUGHT_FISH) {
            completeQuest(player, "pescador");
        }

        // Habilidade do Kit Fisherman: puxa o jogador ou entidade fisgada direto para o Fisherman!
        if (event.getState() == PlayerFishEvent.State.CAUGHT_ENTITY && event.getCaught() != null) {
            ItemStack main = player.getInventory().getItemInMainHand();
            ItemStack off = player.getInventory().getItemInOffHand();
            if ("fisherman".equals(getKitIdFromAbilityItem(main)) || "fisherman".equals(getKitIdFromAbilityItem(off))) {
                Entity caught = event.getCaught();
                if (caught instanceof Player victim && (invincibilityRemainingSeconds > 0 || respawnProtectionSeconds.containsKey(victim.getUniqueId()) || isInSafeZone(victim.getLocation()))) {
                    player.sendActionBar(Component.text("🛡 Este jogador está protegido!", NamedTextColor.RED));
                    return;
                }
                KitDef kit = kits.get("fisherman");
                if (checkAndTriggerKitCooldown(player, kit)) {
                    caught.teleport(player.getLocation());
                    player.playSound(player.getLocation(), Sound.ENTITY_FISHING_BOBBER_RETRIEVE, 1.0f, 1.0f);
                    player.sendActionBar(Component.text("🎣 Você puxou " + caught.getName() + " com o Fisherman!", NamedTextColor.GREEN));
                }
            }
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

        if (entity instanceof Player victim && !victim.getUniqueId().equals(killer.getUniqueId())) {
            completeQuest(killer, "batalha_pvp");
            return;
        }

        if (entity.getType() == EntityType.ENDER_DRAGON) {
            completeQuest(killer, "matador_dragao");
        } else if (entity.getType() == EntityType.WITHER || entity.getType() == EntityType.WITHER_SKELETON) {
            if (entity.getType() == EntityType.WITHER) {
                completeQuest(killer, "wither_skull");
            }
        } else if (entity.getType() == EntityType.BLAZE) {
            completeQuest(killer, "blaze");
        }

        if (entity instanceof Monster) {
            String key = killer.getName().toLowerCase();
            int kills = mobKillProgress.getOrDefault(key, 0) + 1;
            mobKillProgress.put(key, kills);
            if (kills >= 8) {
                completeQuest(killer, "cacador");
            }
        }
    }

    private void checkItemAcquisition(Player player, Material type) {
        switch (type) {
            case CRAFTING_TABLE -> completeQuest(player, "bancada");
            case STONE_PICKAXE -> completeQuest(player, "picareta_pedra");
            case FURNACE -> completeQuest(player, "fornalha");
            case COAL, CHARCOAL -> completeQuest(player, "carvao");
            case BREAD -> completeQuest(player, "pao");
            case COD, SALMON, TROPICAL_FISH, PUFFERFISH, COOKED_COD, COOKED_SALMON -> completeQuest(player, "pescador");
            case IRON_INGOT -> completeQuest(player, "ferro");
            case SHIELD -> completeQuest(player, "escudo");
            case LAVA_BUCKET -> completeQuest(player, "balde_lava");
            case GOLD_INGOT -> completeQuest(player, "ouro");
            case REDSTONE -> completeQuest(player, "redstone");
            case DIAMOND -> completeQuest(player, "diamante");
            case DIAMOND_PICKAXE -> completeQuest(player, "picareta_diamante");
            case OBSIDIAN, CRYING_OBSIDIAN -> completeQuest(player, "obsidiana");
            case ENCHANTING_TABLE -> completeQuest(player, "encantador");
            case BLAZE_ROD -> completeQuest(player, "blaze");
            case GOLDEN_APPLE, ENCHANTED_GOLDEN_APPLE -> completeQuest(player, "maca_dourada");
            case ENDER_EYE -> completeQuest(player, "olho_ender");
            case ANCIENT_DEBRIS, NETHERITE_SCRAP -> completeQuest(player, "ancestral");
            case NETHERITE_INGOT -> completeQuest(player, "netherite");
            case WITHER_SKELETON_SKULL, NETHER_STAR -> completeQuest(player, "wither_skull");
            default -> {}
        }
    }

    // =========================================================================
    // SISTEMA DE TAGS, TAB E SCOREBOARD (INTEGRADO COM PONTOS, KITS E TEMPO)
    // =========================================================================

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        applyPlayerTag(player);
        giveEssentialItems(player);

        Location skySpawn = getColiseumSpawnLocation();
        if (skySpawn != null && (!gatesOpen || !player.hasPlayedBefore() || player.getLocation().getY() < coliseumFloorY - 2)) {
            if (!gatesOpen) {
                player.teleport(skySpawn);
            }
        }

        if (isInSafeZone(player.getLocation())) {
            insideSafeZone.add(player.getUniqueId());
        }
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

        Component prefixComp = Component.text(info.prefix, info.color);
        if (info.bold) {
            prefixComp = prefixComp.decorate(TextDecoration.BOLD);
        }

        Component tabName = Component.text(" ", NamedTextColor.DARK_GRAY)
                .append(prefixComp)
                .append(Component.text(player.getName(), info.color).decoration(TextDecoration.BOLD, false))
                .append(Component.text(" ✦ ", NamedTextColor.DARK_GRAY).decoration(TextDecoration.BOLD, false))
                .append(Component.text(pts + " pts ", NamedTextColor.AQUA).decorate(TextDecoration.BOLD));

        player.playerListName(tabName);
        player.displayName(
                prefixComp.append(Component.text(player.getName(), info.color).decoration(TextDecoration.BOLD, false))
        );

        List<Map.Entry<String, Integer>> top1 = getTopPlayers(1);
        String leaderText = top1.isEmpty() ? "Ninguém (0 pts)" : top1.get(0).getKey() + " (" + top1.get(0).getValue() + " pts)";

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
        String kitId = selectedKits.getOrDefault(viewer.getUniqueId(), "kangaroo");
        KitDef viewerKit = kits.getOrDefault(kitId, kits.get("kangaroo"));
        int ping = viewer.getPing();
        int onlineCount = Bukkit.getOnlinePlayers().size();
        int pts = getPlayerPoints(viewer.getName());
        int completedCount = getConfig().getStringList("conquistas-jogador." + viewer.getName().toLowerCase()).size();

        NamedTextColor pingColor;
        if (ping < 60) pingColor = NamedTextColor.GREEN;
        else if (ping < 150) pingColor = NamedTextColor.YELLOW;
        else if (ping < 250) pingColor = NamedTextColor.GOLD;
        else pingColor = NamedTextColor.RED;

        NamedTextColor timerColor = timerRunning
                ? (matchRemainingSeconds <= 300 ? NamedTextColor.RED : NamedTextColor.GREEN)
                : NamedTextColor.YELLOW;

        List<Map.Entry<String, Integer>> top1 = getTopPlayers(1);
        String leaderText = top1.isEmpty() ? "Ninguém" : top1.get(0).getKey() + " (" + top1.get(0).getValue() + ")";

        setSidebarLine(obj, "line11", 11, Component.empty());

        if (celebratingWinner) {
            setSidebarLine(obj, "line10", 10,
                    Component.text("Status: ", NamedTextColor.WHITE)
                            .append(Component.text("Pódio Final!", NamedTextColor.GOLD).decorate(TextDecoration.BOLD))
            );
        } else if (!gatesOpen) {
            setSidebarLine(obj, "line10", 10,
                    Component.text("Status: ", NamedTextColor.WHITE)
                            .append(Component.text("No Coliseu", NamedTextColor.YELLOW).decorate(TextDecoration.BOLD))
            );
        } else if (invincibilityRemainingSeconds > 0) {
            setSidebarLine(obj, "line10", 10,
                    Component.text("Imune: ", NamedTextColor.WHITE)
                            .append(Component.text(formatTime(invincibilityRemainingSeconds), NamedTextColor.GOLD).decorate(TextDecoration.BOLD))
            );
        } else if (respawnProtectionSeconds.containsKey(viewer.getUniqueId())) {
            int rSec = respawnProtectionSeconds.get(viewer.getUniqueId());
            setSidebarLine(obj, "line10", 10,
                    Component.text("Respawn: ", NamedTextColor.WHITE)
                            .append(Component.text(rSec + "s (Imune)", NamedTextColor.AQUA).decorate(TextDecoration.BOLD))
            );
        } else {
            setSidebarLine(obj, "line10", 10,
                    Component.text("Tempo: ", NamedTextColor.WHITE)
                            .append(Component.text(formatTime(matchRemainingSeconds), timerColor).decorate(TextDecoration.BOLD))
            );
        }

        setSidebarLine(obj, "line9", 9,
                Component.text("Cargo: ", NamedTextColor.WHITE)
                        .append(Component.text(tagDisplay, viewerTag.color).decorate(TextDecoration.BOLD))
        );

        setSidebarLine(obj, "line8", 8,
                Component.text("Kit: ", NamedTextColor.WHITE)
                        .append(Component.text(viewerKit != null ? viewerKit.name : "Kangaroo", viewerKit != null ? viewerKit.color : NamedTextColor.AQUA).decorate(TextDecoration.BOLD))
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
    // COMANDOS (/codex, /kit, /partida, /pontos, /tag, /sb)
    // =========================================================================

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String cmdName = command.getName().toLowerCase();

        if (cmdName.equals("kit")) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(Component.text("Apenas jogadores podem escolher kits.", NamedTextColor.RED));
                return true;
            }
            openKitSelectorGui(player);
            return true;
        }

        if (cmdName.equals("codex")) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(Component.text("Apenas jogadores podem abrir o QuestCodex.", NamedTextColor.RED));
                return true;
            }
            if (args.length > 0 && args[0].equalsIgnoreCase("livro")) {
                giveEssentialItems(player);
                player.sendMessage(Component.text("Itens do QuestCodex verificados no seu inventário!", NamedTextColor.GREEN));
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
                sender.sendMessage(Component.text("/partida iniciar [minutos]", NamedTextColor.YELLOW).append(Component.text(" - Deixa de dia, modo Hard, entrega a Habilidade do Kit, abre o Coliseu e dá 2m de Invencibilidade + Speed I", NamedTextColor.GRAY)));
                sender.sendMessage(Component.text("/partida finalizar", NamedTextColor.YELLOW).append(Component.text(" - Inicia a Cerimônia do Pódio com fogos, fecha o server e reseta o mapa", NamedTextColor.GRAY)));
                sender.sendMessage(Component.text("/partida parar", NamedTextColor.YELLOW).append(Component.text(" - Pausa a partida sem fechar o servidor", NamedTextColor.GRAY)));
                sender.sendMessage(Component.text("/partida resetar", NamedTextColor.YELLOW).append(Component.text(" - Fecha o Coliseu, puxa todos para cima e zera os pontos", NamedTextColor.GRAY)));
                sender.sendMessage(Component.text("/partida coliseu", NamedTextColor.YELLOW).append(Component.text(" - Teleporta para o Coliseu nas alturas", NamedTextColor.GRAY)));
                sender.sendMessage(Component.text("/partida top", NamedTextColor.YELLOW).append(Component.text(" - Mostra o ranking de pontos no chat", NamedTextColor.GRAY)));
                return true;
            }

            World overworld = Bukkit.getWorld("world");

            switch (args[0].toLowerCase()) {
                case "iniciar", "start" -> {
                    int mins = getConfig().getInt("partida.duracao-minutos", 45);
                    if (args.length >= 2) {
                        try {
                            mins = Integer.parseInt(args[1]);
                        } catch (NumberFormatException ignored) {}
                    }
                    celebratingWinner = false;
                    matchRemainingSeconds = mins * 60;
                    invincibilityRemainingSeconds = getConfig().getInt("partida.invencibilidade-segundos", 120);
                    respawnProtectionSeconds.clear();
                    kitCooldownUntil.clear();
                    timerRunning = true;
                    getConfig().set("partida.ativa", true);
                    saveConfig();

                    for (World w : Bukkit.getWorlds()) {
                        w.setDifficulty(Difficulty.HARD);
                    }
                    if (overworld != null) {
                        overworld.setTime(1000L);
                        overworld.setStorm(false);
                        overworld.setThundering(false);
                        setColiseumGates(overworld, true);
                    }

                    Title startTitle = Title.title(
                            Component.text("🔓 PORTÕES ABERTOS! 🔓", NamedTextColor.GOLD).decorate(TextDecoration.BOLD),
                            Component.text("Salte do Coliseu! 2 min de Invencibilidade + Speed I!", NamedTextColor.GREEN),
                            Title.Times.times(Duration.ofMillis(300), Duration.ofSeconds(4), Duration.ofSeconds(1))
                    );

                    int speedTicks = invincibilityRemainingSeconds * 20;

                    for (Player p : Bukkit.getOnlinePlayers()) {
                        deliverSelectedKit(p);
                        giveEssentialItems(p);
                        p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, speedTicks, 0, false, true, true));
                        p.setHealth(20.0);
                        p.setFoodLevel(20);
                        p.showTitle(startTitle);
                        p.playSound(p.getLocation(), Sound.EVENT_RAID_HORN, 1.0f, 1.0f);
                        p.playSound(p.getLocation(), Sound.BLOCK_IRON_DOOR_OPEN, 1.0f, 0.8f);
                        updatePlayerScoreboard(p);
                    }

                    Bukkit.broadcast(Component.empty());
                    Bukkit.broadcast(
                            Component.text("⚔ [QUEST CODEX] ", NamedTextColor.GOLD).decorate(TextDecoration.BOLD)
                                    .append(Component.text("OS PORTÕES DO COLISEU FORAM ABERTOS! (Modo: HARD | Dia)", NamedTextColor.GREEN).decorate(TextDecoration.BOLD))
                    );
                    Bukkit.broadcast(
                            Component.text("🛡 Todos receberam sua ", NamedTextColor.YELLOW)
                                    .append(Component.text("Habilidade de Kit HG + Bússola Caça-Pontos + 2m de Invencibilidade & Speed I", NamedTextColor.AQUA).decorate(TextDecoration.BOLD))
                                    .append(Component.text("!", NamedTextColor.YELLOW))
                    );
                    Bukkit.broadcast(Component.empty());
                    return true;
                }
                case "finalizar", "coroar", "vencedor" -> {
                    startEpicWinnerCeremony();
                    return true;
                }
                case "parar", "stop", "encerrar" -> {
                    timerRunning = false;
                    invincibilityRemainingSeconds = 0;
                    getConfig().set("partida.ativa", false);
                    saveConfig();
                    if (overworld != null) {
                        setColiseumGates(overworld, false);
                    }
                    Bukkit.broadcast(
                            Component.text("🏁 [QUEST CODEX] ", NamedTextColor.GOLD).decorate(TextDecoration.BOLD)
                                    .append(Component.text("A partida foi pausada! (Use /partida finalizar para o Pódio + Reset de Mapa)", NamedTextColor.RED).decoration(TextDecoration.BOLD, false))
                    );
                    showRankingChat(Bukkit.getConsoleSender());
                    for (Player p : Bukkit.getOnlinePlayers()) {
                        showRankingChat(p);
                        p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
                        updatePlayerScoreboard(p);
                    }
                    return true;
                }
                case "resetar", "reset" -> {
                    timerRunning = false;
                    celebratingWinner = false;
                    invincibilityRemainingSeconds = 0;
                    respawnProtectionSeconds.clear();
                    kitCooldownUntil.clear();
                    int mins = getConfig().getInt("partida.duracao-minutos", 45);
                    matchRemainingSeconds = mins * 60;
                    getConfig().set("pontuacao", null);
                    getConfig().set("conquistas-jogador", null);
                    getConfig().set("primeiro-conquista", null);
                    getConfig().set("partida.ativa", false);
                    blockBreakProgress.clear();
                    mobKillProgress.clear();
                    saveConfig();

                    if (overworld != null) {
                        setColiseumGates(overworld, false);
                    }

                    Location skySpawn = getColiseumSpawnLocation();
                    for (Player p : Bukkit.getOnlinePlayers()) {
                        p.removePotionEffect(PotionEffectType.SPEED);
                        removeKitAbilityItems(p);
                        if (skySpawn != null) {
                            p.teleport(skySpawn);
                        }
                        giveEssentialItems(p);
                        updatePlayerScoreboard(p);
                    }
                    Bukkit.broadcast(
                            Component.text("♻ [QUEST CODEX] ", NamedTextColor.GOLD).decorate(TextDecoration.BOLD)
                                    .append(Component.text("Todos voltaram para o Coliseu com o Baú de Kits HG! Use /partida iniciar para começar!", NamedTextColor.YELLOW).decoration(TextDecoration.BOLD, false))
                    );
                    return true;
                }
                case "coliseu", "spawn" -> {
                    if (sender instanceof Player player) {
                        Location skySpawn = getColiseumSpawnLocation();
                        if (skySpawn != null) {
                            player.teleport(skySpawn);
                            player.sendMessage(Component.text("Teleportado para o Coliseu nas alturas!", NamedTextColor.GREEN));
                        }
                    }
                    return true;
                }
                case "darlivro" -> {
                    for (Player p : Bukkit.getOnlinePlayers()) {
                        giveEssentialItems(p);
                    }
                    sender.sendMessage(Component.text("Itens essenciais do QuestCodex entregues para todos!", NamedTextColor.GREEN));
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
                setupWorldAndColiseum();
                for (Player p : Bukkit.getOnlinePlayers()) {
                    applyPlayerTag(p);
                }
                sender.sendMessage(Component.text("Configuração recarregada com sucesso!", NamedTextColor.GREEN));
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
        sender.sendMessage(Component.text("/kit", NamedTextColor.YELLOW).append(Component.text(" - Abre o seletor de 21 Kits HG antes da partida", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/pontos", NamedTextColor.YELLOW).append(Component.text(" - Mostra o ranking de pontos da partida", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/tag", NamedTextColor.YELLOW).append(Component.text(" - Abre suas tags no chat para escolher clicando", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/sb", NamedTextColor.YELLOW).append(Component.text(" - Mostra ou oculta a Scoreboard lateral", NamedTextColor.GRAY)));
        if (sender.isOp() || sender.hasPermission("tags.admin")) {
            sender.sendMessage(Component.text("--- Comandos de Admin (OP) ---", NamedTextColor.RED));
            sender.sendMessage(Component.text("/partida <iniciar|finalizar|parar|resetar|coliseu>", NamedTextColor.AQUA).append(Component.text(" - Controla o Coliseu e a partida", NamedTextColor.GRAY)));
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
                List<String> opts = new ArrayList<>(Arrays.asList("iniciar", "finalizar", "parar", "resetar", "coliseu", "darlivro", "top"));
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

    private record KitDef(
            String id,
            int slot,
            Material icon,
            String name,
            NamedTextColor color,
            int cooldownSeconds,
            String abilityItemName,
            List<String> description
    ) {}
}
