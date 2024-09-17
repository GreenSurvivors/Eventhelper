package de.greensurvivors.eventhelper.modules.ghost.command;

import de.greensurvivors.eventhelper.EventHelper;
import de.greensurvivors.eventhelper.messages.LangPath;
import de.greensurvivors.eventhelper.modules.ghost.GhostGameConfig;
import de.greensurvivors.eventhelper.modules.ghost.GhostLangPath;
import de.greensurvivors.eventhelper.modules.ghost.GhostModul;
import de.greensurvivors.eventhelper.modules.ghost.ghostEntity.IGhost;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;

public class GhostGame implements Listener {
    private final @NotNull GhostGameConfig config;
    private final @NotNull EventHelper plugin;
    private final @NotNull String name;

    private final @NotNull Set<UUID> players = new HashSet<>();
    private final @NotNull Set<IGhost> ghosts = new HashSet<>();
    private @NotNull GameState gameState;
    private long amountOfTicksRun;
    private @Nullable BukkitTask timeTask = null; // this has to be sync!

    public GhostGame(final @NotNull EventHelper plugin, final @NotNull GhostModul modul, final @NotNull String name) {
        this.plugin = plugin;
        this.name = name;

        this.gameState = GameState.IDLE;
        this.config = new GhostGameConfig(plugin, name);
        this.config.setModul(modul);
    }

    @EventHandler(ignoreCancelled = true)
    private void onPlayerQuit(final @NotNull PlayerQuitEvent event) {
        if (players.contains(event.getPlayer().getUniqueId())) {
            playerQuit(event.getPlayer());
        }
    }

    @EventHandler(ignoreCancelled = true)
    private void onPlayerChangeWorld(final @NotNull PlayerChangedWorldEvent event) {
        if (players.contains(event.getPlayer().getUniqueId())) {
            playerQuit(event.getPlayer());
        }
    }

    @EventHandler(ignoreCancelled = true)
    private void onPlayerKick(final @NotNull PlayerKickEvent event) {
        if (players.contains(event.getPlayer().getUniqueId())) {
            playerQuit(event.getPlayer());
        }
    }

    public void startGame() {
        gameState = GameState.STARTING;
        Random random = ThreadLocalRandom.current();

        for (int i = 0; i < config.getAmountOfGhosts(); i++) {
            Location spawnLocation = config.getGhostSpawnLocations().get(random.nextInt(config.getGhostSpawnLocations().size()));

            IGhost newGhost = IGhost.spawnNew(spawnLocation, CreatureSpawnEvent.SpawnReason.CUSTOM, ghost -> {
                ghost.setPersistent(true);
                ghost.setCollidable(false);
                ghost.setInvulnerable(true);
            });

            ghosts.add(newGhost);
        }

        if (timeTask != null) {
            timeTask.cancel();
        }

        for (Iterator<UUID> iterator = players.iterator(); iterator.hasNext(); ) {
            UUID uuid = iterator.next();
            Player player = Bukkit.getPlayer(uuid);

            if (player != null) {
                player.teleportAsync(config.getStartLocation());
                player.setPlayerTime(config.getStartPlayerTime(), false);
            } else {
                iterator.remove();
            }
        }

        timeTask = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 0, 1);
        gameState = GameState.RUNNING;
    }

    private void tick() {
        amountOfTicksRun++;

        long gameDurationInTicks = config.getGameDuration().toSeconds() * 20;
        if (gameDurationInTicks < amountOfTicksRun) {
            long diff = config.getEndPlayerTime() - config.getStartPlayerTime();

            long playerTimeNow = config.getStartPlayerTime() + amountOfTicksRun * gameDurationInTicks / diff;

            for (Iterator<UUID> iterator = players.iterator(); iterator.hasNext(); ) {
                UUID uuid = iterator.next();
                Player player = Bukkit.getPlayer(uuid);

                if (player != null) {
                    player.setPlayerTime(playerTimeNow, false);
                } else {
                    iterator.remove();
                }
            }
        } else { // todo game end because of time
            endGame(EndReason.TIME);
        }
    }

    public void endGame(final @NotNull EndReason reason) { // todo do we need anything else here?
        switch (reason) {
            case EXTERN -> { // Command

            }
            case TIME -> broadcastAll(GhostLangPath.GAME_LOOSE_TIME_BROADCAST);
            case ALL_DEAD -> broadcastAll(GhostLangPath.GAME_LOOSE_DEATH_BROADCAST);
            case WIN -> broadcastAll(GhostLangPath.GAME_WIN_BROADCAST);
        }

        resetGame();
    }

    public void resetGame() {
        gameState = GameState.RESETTING;

        for (Iterator<UUID> iterator = players.iterator(); iterator.hasNext(); ) {
            UUID uuid = iterator.next();
            @Nullable Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                player.teleportAsync(config.getEndLocation());
            }

            iterator.remove();
        }

        for (IGhost ghost : ghosts) {
            ghost.remove();
        }

        if (timeTask != null) {
            timeTask.cancel();
        }
        amountOfTicksRun = 0;

        amountOfTicksRun = config.getStartPlayerTime();
        gameState = GameState.IDLE;
    }

    public boolean isGameFull() {
        return config.getMaxAmountPlayers() == -1 || players.size() < config.getMaxAmountPlayers();
    }

    public void playerJoin(final @NotNull Player player) { // permission
        if (!players.contains(player.getUniqueId())) {
            switch (gameState) {
                case IDLE -> {
                    if (!isGameFull()) {
                        players.add(player.getUniqueId());

                        player.teleportAsync(config.getLobbyLocation());

                        plugin.getMessageManager().sendLang(player, GhostLangPath.PLAYER_GAME_JOIN);
                        broadcastExcept(GhostLangPath.PLAYER_GAME_JOIN_BROADCAST, player.getUniqueId());
                    } else {
                        plugin.getMessageManager().sendLang(player, GhostLangPath.ERROR_GAME_FULL);
                    }
                }
                case RUNNING -> {
                    if (config.isLateJoinAllowed()) {
                        if (!isGameFull()) {
                            player.teleportAsync(config.getStartLocation());

                            plugin.getMessageManager().sendLang(player, GhostLangPath.PLAYER_GAME_JOIN);
                            broadcastExcept(GhostLangPath.PLAYER_GAME_JOIN_BROADCAST, player.getUniqueId());
                        } else {
                            plugin.getMessageManager().sendLang(player, GhostLangPath.ERROR_GAME_FULL);
                        }
                    } else {
                        plugin.getMessageManager().sendLang(player, GhostLangPath.ERROR_NO_LATE_JOIN);
                    }
                }
                case STARTING, RESETTING -> plugin.getMessageManager().sendLang(player, GhostLangPath.ERROR_GAME_STATE);
            }
        } else {
            plugin.getMessageManager().sendLang(player, GhostLangPath.ERROR_ALREADY_PLAYING);
        }
    }

    public void playerQuit(final @NotNull Player player) {
        if (players.contains(player.getUniqueId())) {
            player.teleport(config.getEndLocation());

            players.remove(player.getUniqueId());

            if (gameState != GameState.RESETTING) {
                plugin.getMessageManager().sendLang(player, GhostLangPath.PLAYER_GAME_QUIT);
                broadcastExcept(GhostLangPath.PLAYER_GAME_QUIT_BROADCAST, player.getUniqueId());
            }
        } else {
            if (gameState != GameState.RESETTING) {
                plugin.getMessageManager().sendLang(player, GhostLangPath.ERROR_NOT_PLAYING);
            }
        }
    }

    public void broadcastExcept(@NotNull LangPath langPath, final @Nullable UUID exception) {
        for (Iterator<UUID> iterator = players.iterator(); iterator.hasNext(); ) {
            UUID uuid = iterator.next();
            if (!uuid.equals(exception)) {
                Player player = Bukkit.getPlayer(uuid);

                if (player != null) {
                    plugin.getMessageManager().sendLang(player, langPath);
                } else {
                    iterator.remove();
                }
            }
        }
    }

    public void broadcastAll(@NotNull LangPath langPath) {
        broadcastExcept(langPath, null);
    }

    public @NotNull CompletableFuture<@NotNull Boolean> reload() { // todo should this reset the game?
        return config.reload().thenApply(result -> {
            resetGame();
            return result;
        });
    }

    public @NotNull String getName() {
        return name;
    }

    public @NotNull GameState getGameState() {
        return gameState;
    }

    public enum EndReason {
        EXTERN,
        TIME,
        ALL_DEAD,
        WIN,
    }

    public enum GameState {
        IDLE,
        STARTING,
        RUNNING,
        RESETTING
    }
}
