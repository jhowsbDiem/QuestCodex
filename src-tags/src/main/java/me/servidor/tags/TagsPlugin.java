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
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.WorldBorder;
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
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.FurnaceExtractEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
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
import java.util.Set;
import java.util.UUID;

public final class TagsPlugin extends JavaPlugin implements Listener, CommandExecutor, TabCompleter {

    private final Set<UUID> hiddenScoreboard = new HashSet<>();
    private final Set<UUID> insideSafeZone = new HashSet<>();
    private final Map<String, QuestDef> quests = new LinkedHashMap<>();
    private final Map<String, Integer> blockBreakProgress = new HashMap<>();
    private final Map<String, Integer> mobKillProgress = new HashMap<>();
    private NamespacedKey codexBookKey;

    // Cronômetro da partida (em segundos). Padrão: 45 * 60 = 2700s
    private int matchRemainingSeconds = 45 * 60;
    private boolean timerRunning = false;

    // Controle do Coliseu e Invencibilidade Inicial (2 minutos = 120s)
    private boolean gatesOpen = false;
    private int invincibilityRemainingSeconds = 0;

    // Coordenadas base do Coliseu nas alturas
    private int coliseumCenterX = 0;
    private int coliseumCenterZ = 0;
    private int coliseumFloorY = 165;
    private int coliseumRadius = 16;

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
        if (!getConfig().contains("partida.invencibilidade-segundos")) {
            getConfig().set("partida.ativa", false);
            getConfig().set("partida.duracao-minutos", 45);
            getConfig().set("partida.invencibilidade-segundos", 120);
            getConfig().set("partida.bonus-primeiro-porcentagem", 50);
            getConfig().set("partida.borda-mapa-blocos", 350);
        } else if (getConfig().getInt("partida.borda-mapa-blocos", 350) >= 1000) {
            getConfig().set("partida.borda-mapa-blocos", 350);
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
        setupWorldAndColiseum();

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
            if (isInSafeZone(player.getLocation())) {
                insideSafeZone.add(player.getUniqueId());
            }
        }

        // Loop principal de 1 segundo (Cronômetro de 45 min + Invencibilidade 2 min + Scoreboard + TAB)
        Bukkit.getScheduler().runTaskTimer(this, () -> {
            tickMatchTimer();
            for (Player player : Bukkit.getOnlinePlayers()) {
                updatePlayerScoreboard(player);
            }
        }, 20L, 20L);

        getLogger().info("QuestCodex v3.0 (Coliseu nas Alturas + Portoes + Invencibilidade 2m + Speed I) ativado!");
    }

    // =========================================================================
    // CONSTRUÇÃO DO COLISEU NAS ALTURAS E CONTROLE DOS PORTÕES
    // =========================================================================

    private void setupWorldAndColiseum() {
        World overworld = Bukkit.getWorld("world");
        if (overworld == null) return;

        Location currentSpawn = overworld.getSpawnLocation();
        coliseumCenterX = currentSpawn.getBlockX();
        coliseumCenterZ = currentSpawn.getBlockZ();
        coliseumFloorY = getConfig().getInt("coliseu.altura-y", 165);
        coliseumRadius = getConfig().getInt("coliseu.raio", 16);

        int borderSize = getConfig().getInt("partida.borda-mapa-blocos", 350);
        WorldBorder border = overworld.getWorldBorder();
        border.setCenter(coliseumCenterX, coliseumCenterZ);
        border.setSize(borderSize);

        // Constrói o Coliseu nas alturas (Y = 165) e define o Spawnpoint dentro dele
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
        int R = coliseumRadius; // 16

        // Verifica se o piso central do Coliseu já foi construído
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

                // Limpa o ar acima do piso
                for (int dy = 1; dy <= 12; dy++) {
                    world.getBlockAt(x, cy + dy, z).setType(Material.AIR);
                }

                // 1. Piso do Coliseu (com centro de vidro para ver o mundo lá embaixo!)
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

                // Base inferior reforçada
                if (dist > 3.5) {
                    world.getBlockAt(x, cy - 1, z).setType(Material.STONE_BRICKS);
                }

                // 2. Muralhas e Arquibancadas do Coliseu (Borda circular)
                if (dist >= R - 1.2 && dist <= R + 0.6) {
                    boolean isGateOpening = (Math.abs(dx) <= 2 && Math.abs(dz) >= R - 2)
                            || (Math.abs(dz) <= 2 && Math.abs(dx) >= R - 2);

                    // Altura da parede do Coliseu: 8 blocos
                    for (int dy = 1; dy <= 8; dy++) {
                        Block wallBlock = world.getBlockAt(x, cy + dy, z);
                        if (isGateOpening && dy <= 4) {
                            // Espaço dos 4 Portões (Norte, Sul, Leste, Oeste) — será preenchido por setColiseumGates
                            wallBlock.setType(Material.IRON_BARS);
                        } else if (dy == 8) {
                            // Topo da muralha com iluminação e detalhes dourados
                            wallBlock.setType(((dx + dz) % 2 == 0) ? Material.STONE_BRICK_SLAB : Material.GOLD_BLOCK);
                        } else if (dy == 5) {
                            wallBlock.setType(Material.CHISELED_STONE_BRICKS);
                        } else if ((dx % 4 == 0 || dz % 4 == 0)) {
                            // Pilares de Quartzo
                            wallBlock.setType(Material.QUARTZ_PILLAR);
                        } else if (dy >= 3 && dy <= 6 && !isGateOpening) {
                            // Janelas panorâmicas com grades
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
                        // Piso da plataforma de salto nos portões
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
        int cz = spawnZ - 30; // 30 blocos ao Norte do Spawn no chão
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

        placePortalFrame(world, cx - 1, cy + 1, cz - 2, org.bukkit.block.BlockFace.SOUTH);
        placePortalFrame(world, cx,     cy + 1, cz - 2, org.bukkit.block.BlockFace.SOUTH);
        placePortalFrame(world, cx + 1, cy + 1, cz - 2, org.bukkit.block.BlockFace.SOUTH);

        placePortalFrame(world, cx - 1, cy + 1, cz + 2, org.bukkit.block.BlockFace.NORTH);
        placePortalFrame(world, cx,     cy + 1, cz + 2, org.bukkit.block.BlockFace.NORTH);
        placePortalFrame(world, cx + 1, cy + 1, cz + 2, org.bukkit.block.BlockFace.NORTH);

        placePortalFrame(world, cx - 2, cy + 1, cz - 1, org.bukkit.block.BlockFace.EAST);
        placePortalFrame(world, cx - 2, cy + 1, cz,     org.bukkit.block.BlockFace.EAST);
        placePortalFrame(world, cx - 2, cy + 1, cz + 1, org.bukkit.block.BlockFace.EAST);

        placePortalFrame(world, cx + 2, cy + 1, cz - 1, org.bukkit.block.BlockFace.WEST);
        placePortalFrame(world, cx + 2, cy + 1, cz,     org.bukkit.block.BlockFace.WEST);
        placePortalFrame(world, cx + 2, cy + 1, cz + 1, org.bukkit.block.BlockFace.WEST);
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

    private void placePortalFrame(World world, int x, int y, int z, org.bukkit.block.BlockFace facing) {
        Block b = world.getBlockAt(x, y, z);
        b.setType(Material.END_PORTAL_FRAME);
        if (b.getBlockData() instanceof org.bukkit.block.data.type.EndPortalFrame frame) {
            frame.setFacing(facing);
            frame.setEye(false);
            b.setBlockData(frame);
        }
    }

    // =========================================================================
    // CRONÔMETRO DE 45 MINUTOS E INVENCIBILIDADE DE 2 MINUTOS
    // =========================================================================

    private void tickMatchTimer() {
        if (!timerRunning || !getConfig().getBoolean("partida.ativa", false)) {
            return;
        }

        // 1. Contagem regressiva da Invencibilidade Inicial (2 minutos = 120s)
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
                                .append(Component.text("A invencibilidade de 2 minutos ACABOU! O PvP e o dano estão valendo!", NamedTextColor.YELLOW).decoration(TextDecoration.BOLD, false))
                );
                for (Player p : Bukkit.getOnlinePlayers()) {
                    p.playSound(p.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 0.7f, 1.2f);
                }
            }
        }

        // 2. Contagem regressiva da Partida (45 minutos)
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
                finishMatchAutomatically();
            }
        }
    }

    private void finishMatchAutomatically() {
        timerRunning = false;
        invincibilityRemainingSeconds = 0;
        getConfig().set("partida.ativa", false);
        saveConfig();

        List<Map.Entry<String, Integer>> top = getTopPlayers(1);
        String winnerName = top.isEmpty() ? "Ninguém" : top.get(0).getKey();
        int winnerPts = top.isEmpty() ? 0 : top.get(0).getValue();

        Bukkit.broadcast(Component.empty());
        Bukkit.broadcast(Component.text("=============================================", NamedTextColor.GOLD).decorate(TextDecoration.BOLD));
        Bukkit.broadcast(Component.text("🏁 TEMPO ESGOTADO! FIM DA PARTIDA! 🏁", NamedTextColor.RED).decorate(TextDecoration.BOLD));
        Bukkit.broadcast(
                Component.text("🏆 GRANDE VENCEDOR: ", NamedTextColor.YELLOW).decorate(TextDecoration.BOLD)
                        .append(Component.text(winnerName + " (" + winnerPts + " pts)", NamedTextColor.GREEN).decorate(TextDecoration.BOLD))
        );
        Bukkit.broadcast(Component.text("=============================================", NamedTextColor.GOLD).decorate(TextDecoration.BOLD));

        showRankingChat(Bukkit.getConsoleSender());
        Title winTitle = Title.title(
                Component.text("🏆 FIM DE JOGO! 🏆", NamedTextColor.GOLD).decorate(TextDecoration.BOLD),
                Component.text("Vencedor: " + winnerName + " (" + winnerPts + " pts)", NamedTextColor.YELLOW),
                Title.Times.times(Duration.ofMillis(500), Duration.ofSeconds(5), Duration.ofSeconds(1))
        );

        for (Player p : Bukkit.getOnlinePlayers()) {
            showRankingChat(p);
            p.showTitle(winTitle);
            p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
        }
    }

    private String formatTime(int totalSeconds) {
        int mins = totalSeconds / 60;
        int secs = totalSeconds % 60;
        return String.format("%02d:%02d", mins, secs);
    }

    // =========================================================================
    // SAFE ZONE DO COLISEU, RESTRIÇÃO DE SAÍDA E INVENCIBILIDADE
    // =========================================================================

    public boolean isInSafeZone(Location loc) {
        if (loc == null || loc.getWorld() == null) return false;
        if (!getConfig().getBoolean("safezone.ativada", true)) return false;
        if (loc.getWorld().getEnvironment() != World.Environment.NORMAL) return false;

        // O Coliseu nas alturas (Y >= coliseumFloorY - 3 dentro do raio de 25 blocos) é sempre protegido!
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

        // Se a partida ainda não começou (portões trancados), somente DONO, MOD e ADMIN podem sair do Coliseu!
        if (!gatesOpen && !canBypassColiseumLock(player)) {
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
            // Invencível enquanto os portões estão fechados, durante os 2 min iniciais (para queda do Coliseu e tudo mais) ou dentro do Coliseu
            if (!gatesOpen || invincibilityRemainingSeconds > 0 || isInSafeZone(victim.getLocation())) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPvpProtected(EntityDamageByEntityEvent event) {
        Player attacker = null;
        if (event.getDamager() instanceof Player p) {
            attacker = p;
        } else if (event.getDamager() instanceof Projectile proj && proj.getShooter() instanceof Player p) {
            attacker = p;
        }

        if (event.getEntity() instanceof Player victim) {
            if (!gatesOpen || invincibilityRemainingSeconds > 0
                    || isInSafeZone(victim.getLocation())
                    || (attacker != null && isInSafeZone(attacker.getLocation()))) {
                event.setCancelled(true);
                if (attacker != null) {
                    if (invincibilityRemainingSeconds > 0) {
                        attacker.sendActionBar(Component.text("🛡 Invencibilidade inicial ativa (" + formatTime(invincibilityRemainingSeconds) + ")!", NamedTextColor.RED));
                    } else {
                        attacker.sendActionBar(Component.text("🛡 PvP desativado no Coliseu!", NamedTextColor.RED));
                    }
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onHungerProtected(FoodLevelChangeEvent event) {
        if (event.getEntity() instanceof Player player) {
            if ((!gatesOpen || invincibilityRemainingSeconds > 0 || isInSafeZone(player.getLocation()))
                    && event.getFoodLevel() < player.getFoodLevel()) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockPlaceInSafeZone(BlockPlaceEvent event) {
        Player player = event.getPlayer();
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
    // REGISTRO DE CONQUISTAS
    // =========================================================================

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
        if (!getConfig().getBoolean("partida.ativa", false)) {
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

        if (event.getWhoClicked() instanceof Player player && event.getCurrentItem() != null) {
            checkItemAcquisition(player, event.getCurrentItem().getType());
        }
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        Location skySpawn = getColiseumSpawnLocation();
        if (skySpawn != null) {
            event.setRespawnLocation(skySpawn);
        }
        Bukkit.getScheduler().runTaskLater(this, () -> ensurePlayerHasCodexBook(event.getPlayer()), 10L);
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
    // SISTEMA DE TAGS, TAB E SCOREBOARD (INTEGRADO COM PONTOS E TEMPO)
    // =========================================================================

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        applyPlayerTag(player);
        ensurePlayerHasCodexBook(player);

        // Se a partida ainda não começou ou o jogador acabou de entrar, garante que ele nasça no Coliseu
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

        // Linhas da Scoreboard (10 até 1)
        setSidebarLine(obj, "line10", 10, Component.empty());

        if (!gatesOpen) {
            setSidebarLine(obj, "line9", 9,
                    Component.text("Status: ", NamedTextColor.WHITE)
                            .append(Component.text("No Coliseu", NamedTextColor.YELLOW).decorate(TextDecoration.BOLD))
            );
        } else if (invincibilityRemainingSeconds > 0) {
            setSidebarLine(obj, "line9", 9,
                    Component.text("Imune: ", NamedTextColor.WHITE)
                            .append(Component.text(formatTime(invincibilityRemainingSeconds), NamedTextColor.GOLD).decorate(TextDecoration.BOLD))
            );
        } else {
            setSidebarLine(obj, "line9", 9,
                    Component.text("Tempo: ", NamedTextColor.WHITE)
                            .append(Component.text(formatTime(matchRemainingSeconds), timerColor).decorate(TextDecoration.BOLD))
            );
        }

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
                sender.sendMessage(Component.text("/partida iniciar [minutos]", NamedTextColor.YELLOW).append(Component.text(" - Abre os portões do Coliseu, dá 2m de Invencibilidade + Speed I e inicia os 45m", NamedTextColor.GRAY)));
                sender.sendMessage(Component.text("/partida parar", NamedTextColor.YELLOW).append(Component.text(" - Pausa a partida e anuncia o vencedor atual", NamedTextColor.GRAY)));
                sender.sendMessage(Component.text("/partida resetar", NamedTextColor.YELLOW).append(Component.text(" - Fecha o Coliseu, puxa todos para cima e zera os pontos", NamedTextColor.GRAY)));
                sender.sendMessage(Component.text("/partida coliseu", NamedTextColor.YELLOW).append(Component.text(" - Teleporta para o Coliseu nas alturas", NamedTextColor.GRAY)));
                sender.sendMessage(Component.text("/partida darlivro", NamedTextColor.YELLOW).append(Component.text(" - Entrega o livro QuestCodex para todos", NamedTextColor.GRAY)));
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
                    matchRemainingSeconds = mins * 60;
                    invincibilityRemainingSeconds = getConfig().getInt("partida.invencibilidade-segundos", 120);
                    timerRunning = true;
                    getConfig().set("partida.ativa", true);
                    saveConfig();

                    if (overworld != null) {
                        setColiseumGates(overworld, true);
                    }

                    Title startTitle = Title.title(
                            Component.text("🔓 PORTÕES ABERTOS! 🔓", NamedTextColor.GOLD).decorate(TextDecoration.BOLD),
                            Component.text("Salte do Coliseu! 2 min de Invencibilidade + Speed I!", NamedTextColor.GREEN),
                            Title.Times.times(Duration.ofMillis(300), Duration.ofSeconds(4), Duration.ofSeconds(1))
                    );

                    int speedTicks = invincibilityRemainingSeconds * 20;
                    PotionEffect speedEffect = new PotionEffect(PotionEffectType.SPEED, speedTicks, 0, false, true, true);

                    for (Player p : Bukkit.getOnlinePlayers()) {
                        ensurePlayerHasCodexBook(p);
                        p.addPotionEffect(speedEffect);
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
                                    .append(Component.text("OS PORTÕES DO COLISEU FORAM ABERTOS!", NamedTextColor.GREEN).decorate(TextDecoration.BOLD))
                    );
                    Bukkit.broadcast(
                            Component.text("🛡 Todos receberam ", NamedTextColor.YELLOW)
                                    .append(Component.text("2 minutos de Invencibilidade + Speed I", NamedTextColor.AQUA).decorate(TextDecoration.BOLD))
                                    .append(Component.text(" para saltar do Coliseu e começar!", NamedTextColor.YELLOW))
                    );
                    Bukkit.broadcast(Component.empty());
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
                                    .append(Component.text("A partida foi encerrada e os portões do Coliseu foram fechados!", NamedTextColor.RED).decoration(TextDecoration.BOLD, false))
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
                    invincibilityRemainingSeconds = 0;
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
                        if (skySpawn != null) {
                            p.teleport(skySpawn);
                        }
                        ensurePlayerHasCodexBook(p);
                        updatePlayerScoreboard(p);
                    }
                    Bukkit.broadcast(
                            Component.text("♻ [QUEST CODEX] ", NamedTextColor.GOLD).decorate(TextDecoration.BOLD)
                                    .append(Component.text("Todos foram puxados para o Coliseu trancado e os pontos foram zerados! Use /partida iniciar para abrir os portões!", NamedTextColor.YELLOW).decoration(TextDecoration.BOLD, false))
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
        sender.sendMessage(Component.text("/codex livro", NamedTextColor.YELLOW).append(Component.text(" - Recebe o livro QuestCodex novamente", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/pontos", NamedTextColor.YELLOW).append(Component.text(" - Mostra o ranking de pontos da partida", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/tag", NamedTextColor.YELLOW).append(Component.text(" - Abre suas tags no chat para escolher clicando", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/tag <nome>", NamedTextColor.YELLOW).append(Component.text(" - Equipa uma tag direto (ex: /tag dono)", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/sb", NamedTextColor.YELLOW).append(Component.text(" - Mostra ou oculta a Scoreboard lateral", NamedTextColor.GRAY)));
        if (sender.isOp() || sender.hasPermission("tags.admin")) {
            sender.sendMessage(Component.text("--- Comandos de Admin (OP) ---", NamedTextColor.RED));
            sender.sendMessage(Component.text("/partida <iniciar|parar|resetar|coliseu>", NamedTextColor.AQUA).append(Component.text(" - Controla o Coliseu e a partida", NamedTextColor.GRAY)));
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
                List<String> opts = new ArrayList<>(Arrays.asList("iniciar", "parar", "resetar", "coliseu", "darlivro", "top"));
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
