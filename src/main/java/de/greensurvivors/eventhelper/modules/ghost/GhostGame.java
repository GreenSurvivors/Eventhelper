package de.greensurvivors.eventhelper.modules.ghost;

import de.greensurvivors.eventhelper.EventHelper;
import de.greensurvivors.eventhelper.messages.LangPath;
import de.greensurvivors.eventhelper.messages.SharedPlaceHolder;
import de.greensurvivors.eventhelper.modules.ghost.ghostEntity.IGhost;
import de.greensurvivors.eventhelper.modules.ghost.payer.*;
import de.greensurvivors.simplequests.events.QuestCompleatedEvent;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;

public class GhostGame implements Listener { // todo spectating command
    private final @NotNull EventHelper plugin;
    private final @NotNull GhostModul ghostModul;
    private final @NotNull GhostGameConfig config;
    private final @NotNull String name_id;

    private final @NotNull Map<@NotNull UUID, @NotNull AGhostGamePlayer> players = new HashMap<>();
    private final @NotNull Map<@NotNull UUID, @NotNull SpectatingPlayer> spectators = new HashMap<>();
    private final @NotNull Set<@NotNull MouseTrap> mouseTraps = new HashSet<>();
    private final @NotNull Set<@NotNull IGhost> ghosts = new HashSet<>();
    private final @NotNull Scoreboard scoreboard;
    private final @NotNull Team perishedTeam;
    private @NotNull GameState gameState;
    private long amountOfTicksRun;
    private @Nullable BukkitTask timeTask = null; // this has to be sync!
    private int gainedPoints = 0;

    public GhostGame(final @NotNull EventHelper plugin, final @NotNull GhostModul modul, final @NotNull String name_id) {
        this.plugin = plugin;
        this.ghostModul = modul;
        this.name_id = name_id;

        this.gameState = GameState.IDLE;
        this.config = new GhostGameConfig(plugin, name_id, modul);

        scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();

        perishedTeam = scoreboard.registerNewTeam("perished");
        perishedTeam.setOption(Team.Option.COLLISION_RULE, Team.OptionStatus.NEVER);
        perishedTeam.setCanSeeFriendlyInvisibles(true);
    }

    @EventHandler(ignoreCancelled = true)
    private void onPlayerQuit(final @NotNull PlayerQuitEvent event) {
        if (players.containsKey(event.getPlayer().getUniqueId())) {
            playerQuit(event.getPlayer(), true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    private void onPlayerChangeWorld(final @NotNull PlayerChangedWorldEvent event) {
        if (players.containsKey(event.getPlayer().getUniqueId())) {
            playerQuit(event.getPlayer(), false);
        }
    }

    /* // probably not needed, since PlayerQuitEvent should get called anyways
    @EventHandler(ignoreCancelled = true)
    private void onPlayerKick(final @NotNull PlayerKickEvent event) {
        if (players.containsKey(event.getPlayer().getUniqueId())) {
            playerQuit(event.getPlayer(), true);
        }
    }*/

    @EventHandler(ignoreCancelled = true)
    private void onQuestComplete(final @NotNull QuestCompleatedEvent event) {
    }

    public void startGame() { // todo start count down
        gameState = GameState.STARTING;
        Random random = ThreadLocalRandom.current();

        for (int i = 0; i < config.getAmountOfGhosts(); i++) {
            Location spawnLocation = config.getGhostSpawnLocations().get(random.nextInt(config.getGhostSpawnLocations().size()));

            IGhost newGhost = IGhost.spawnNew(spawnLocation, CreatureSpawnEvent.SpawnReason.CUSTOM, this, ghost -> {
                ghost.setPersistent(true);  // don't despawn
                ghost.setCollidable(false); // don't be a push around
                //ghost.setInvulnerable(true);
            });

            ghosts.add(newGhost);
        }

        if (timeTask != null) {
            timeTask.cancel();
        }

        for (Iterator<Map.Entry<UUID, AGhostGamePlayer>> iterator = players.entrySet().iterator(); iterator.hasNext(); ) {
            Map.Entry<UUID, AGhostGamePlayer> entry = iterator.next();
            Player player = Bukkit.getPlayer(entry.getKey());

            if (player == null) {
                if (entry.getValue() instanceof AlivePlayer alivePlayer) {
                    MouseTrap trap = alivePlayer.getMouseTrapTrappedIn();

                    if (trap != null) {
                        trap.removePlayer(alivePlayer);
                    }
                }

                plugin.getComponentLogger().error("Removed player \"{}\" from game since they got missing. They couldn't get reset correctly!", entry.getKey());
                iterator.remove();
            } else {
                player.teleportAsync(config.getPlayerStartLocation());
                player.setPlayerTime(config.getStartPlayerTime(), false);
            }
        }

        if (players.isEmpty()) { // all players magically vanished.
            endGame(EndReason.ALL_DEAD);

            return;
        }

        timeTask = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 0, 1);
        gameState = GameState.RUNNING;
    }

    private void tick() {
        amountOfTicksRun++;

        long gameDurationInTicks = config.getGameDuration().toSeconds() * 20;
        if (gameDurationInTicks < amountOfTicksRun) {
            if (config.getStartPlayerTime() >= 0 && config.getEndPlayerTime() >= 0) {
                long timeDiff = config.getEndPlayerTime() - config.getStartPlayerTime();

                if (timeDiff > 0) {
                    long playerTimeNow = config.getStartPlayerTime() + amountOfTicksRun * gameDurationInTicks / timeDiff;

                    // set playerTime for alive players
                    for (Iterator<Map.Entry<UUID, AGhostGamePlayer>> iterator = players.entrySet().iterator(); iterator.hasNext(); ) {
                        Map.Entry<UUID, AGhostGamePlayer> entry = iterator.next();
                        Player player = Bukkit.getPlayer(entry.getKey());

                        if (player == null) {
                            if (entry.getValue() instanceof AlivePlayer alivePlayer) {
                                MouseTrap trap = alivePlayer.getMouseTrapTrappedIn();

                                if (trap != null) {
                                    trap.removePlayer(alivePlayer);
                                }
                            }

                            plugin.getComponentLogger().error("Removed player \"{}\" from game since they got missing. They couldn't get reset correctly!", entry.getKey());
                            iterator.remove();
                        } else {
                            player.setPlayerTime(playerTimeNow, false);
                        }
                    }
                } else {
                    // just set to start time for players
                    for (Iterator<Map.Entry<UUID, AGhostGamePlayer>> iterator = players.entrySet().iterator(); iterator.hasNext(); ) {
                        Map.Entry<UUID, AGhostGamePlayer> entry = iterator.next();
                        Player player = Bukkit.getPlayer(entry.getKey());

                        if (player == null) {
                            if (entry.getValue() instanceof AlivePlayer alivePlayer) {
                                MouseTrap trap = alivePlayer.getMouseTrapTrappedIn();

                                if (trap != null) {
                                    trap.removePlayer(alivePlayer);
                                }
                            }

                            plugin.getComponentLogger().error("Removed player \"{}\" from game since they got missing. They couldn't get reset correctly!", entry.getKey());
                            iterator.remove();
                        } else {
                            player.setPlayerTime(config.getStartPlayerTime(), false);
                        }
                    }
                }

                // check if players just vanished
                if (players.isEmpty()) {
                    endGame(EndReason.GAME_EMPTY);
                }

                // todo tick mouse traps here
            }
        } else {
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
            case GAME_EMPTY -> {
            } // nothing to do
        }

        resetGame();
    }

    public void resetGame() {
        gameState = GameState.RESETTING;

        for (MouseTrap mouseTrap : mouseTraps) {
            mouseTrap.releaseAllPlayers();
        }

        for (Iterator<Map.Entry<UUID, AGhostGamePlayer>> iterator = players.entrySet().iterator(); iterator.hasNext(); ) {
            Map.Entry<UUID, AGhostGamePlayer> entry = iterator.next();
            @Nullable Player player = Bukkit.getPlayer(entry.getKey());
            if (player != null) {
                player.teleportAsync(config.getEndLocation());
            }

            entry.getValue().restorePlayer();
            iterator.remove();
        }

        // kill entities
        for (IGhost ghost : ghosts) {
            ghost.remove();
        }
        ghosts.clear();

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
        if (ghostModul.getGameOfPlayer(player) != null) {
            switch (gameState) {
                case IDLE -> {
                    if (!isGameFull()) {
                        setUpJoinedPlayer(player);
                    } else {
                        plugin.getMessageManager().sendLang(player, GhostLangPath.ERROR_GAME_FULL);
                    }
                }
                case RUNNING -> {
                    if (config.isLateJoinAllowed()) {
                        if (!isGameFull()) {
                            setUpJoinedPlayer(player);
                        } else {
                            plugin.getMessageManager().sendLang(player, GhostLangPath.ERROR_GAME_FULL);
                        }
                    } else {
                        plugin.getMessageManager().sendLang(player, GhostLangPath.ERROR_NO_LATE_JOIN);
                    }
                }
                case STARTING, RESETTING ->
                    plugin.getMessageManager().sendLang(player, GhostLangPath.ERROR_JOIN_GAME_STATE);
            }
        } else {
            plugin.getMessageManager().sendLang(player, GhostLangPath.ERROR_ALREADY_PLAYING);
        }
    }

    private void setUpJoinedPlayer(final @NotNull Player player) {
        players.put(player.getUniqueId(), new AlivePlayer(plugin, this, player.getUniqueId()));

        player.teleportAsync(config.getLobbyLocation());

        // hide players
        for (AGhostGamePlayer otherPlayer : players.values()) {
            if (otherPlayer instanceof DeadPlayer) {
                player.hidePlayer(plugin, otherPlayer.getBukkitPlayer());
            }
        }
        for (SpectatingPlayer spectatingPlayer : spectators.values()) {
            player.hidePlayer(plugin, spectatingPlayer.getBukkitPlayer());
        }

        plugin.getMessageManager().sendLang(player, GhostLangPath.PLAYER_GAME_JOIN,
            Placeholder.component(SharedPlaceHolder.TEXT.getKey(), getConfig().getDisplayName()));
        broadcastExcept(GhostLangPath.PLAYER_GAME_JOIN_BROADCAST, player.getUniqueId(),
            Placeholder.component(SharedPlaceHolder.PLAYER.getKey(), player.displayName()));
    }

    protected void makePlayerSpectator(final @NotNull Player player) {
        makePlayerSpectator(player, new PlayerData(plugin, player));
    }

    protected void makePlayerSpectator(final @NotNull Player player, final @NotNull PlayerData playerData) {
        players.remove(player.getUniqueId());
        spectators.put(player.getUniqueId(), new SpectatingPlayer(plugin, this, player.getUniqueId(), playerData));

        for (AGhostGamePlayer ghostGamePlayer : players.values()) {
            Player otherPlayer = ghostGamePlayer.getBukkitPlayer();

            if (otherPlayer != null) {
                otherPlayer.hidePlayer(plugin, player);
            }
        }

        player.setScoreboard(scoreboard);
        perishedTeam.addPlayer(player);
    }

    public void playerQuit(final @NotNull Player player, boolean teleport) {
        AGhostGamePlayer ghostGamePlayer = players.get(player.getUniqueId());
        if (ghostGamePlayer != null) {
            if (teleport) {
                player.teleport(config.getEndLocation());
                player.resetPlayerTime();
            }

            // reset Scoreboard
            perishedTeam.removePlayer(player);
            ghostGamePlayer.restorePlayer();

            if (ghostGamePlayer instanceof AlivePlayer alivePlayer) {
                MouseTrap trap = alivePlayer.getMouseTrapTrappedIn();

                if (trap != null) {
                    trap.removePlayer(alivePlayer);
                }
            } else if (ghostGamePlayer instanceof DeadPlayer deadPlayer) { // todo make visible again

                for (Iterator<AGhostGamePlayer> iterator = players.values().iterator(); iterator.hasNext(); ) {
                    AGhostGamePlayer otherGhostGamePlayer = iterator.next();
                    Player otherPlayer = otherGhostGamePlayer.getBukkitPlayer();

                    if (otherPlayer != null) {
                        otherPlayer.showPlayer(plugin, player);
                    } else {
                        iterator.remove();
                    }
                }
            }

            players.remove(player.getUniqueId());

            if (gameState != GameState.RESETTING) {
                if (players.isEmpty()) {
                    endGame(EndReason.GAME_EMPTY);
                } else if (areAllPlayersDead()) {
                    endGame(EndReason.ALL_DEAD);
                } else {
                    plugin.getMessageManager().sendLang(player, GhostLangPath.PLAYER_GAME_QUIT);
                    broadcastExcept(GhostLangPath.PLAYER_GAME_QUIT_BROADCAST, player.getUniqueId(),
                        Placeholder.component(SharedPlaceHolder.PLAYER.getKey(), player.displayName()));
                }
            }
        } else {
            SpectatingPlayer spectatingPlayer = spectators.get(player.getUniqueId());

            if (spectatingPlayer != null) {
                // reset Scoreboard
                perishedTeam.removePlayer(player);
                spectatingPlayer.restorePlayer();

                for (Iterator<AGhostGamePlayer> iterator = players.values().iterator(); iterator.hasNext(); ) {
                    AGhostGamePlayer otherGhostGamePlayer = iterator.next();
                    Player otherPlayer = otherGhostGamePlayer.getBukkitPlayer();

                    if (otherPlayer != null) {
                        otherPlayer.showPlayer(plugin, player);
                    } else {
                        iterator.remove();
                    }
                }
            } else {
                if (gameState != GameState.RESETTING) {
                    plugin.getMessageManager().sendLang(player, GhostLangPath.ERROR_NOT_PLAYING_SELF);
                }
            }
        }
    }

    public void broadcastExcept(final @NotNull LangPath langPath,
                                final @Nullable UUID exception,
                                final @NotNull TagResolver... resolvers) {
        for (Iterator<Map.Entry<UUID, AGhostGamePlayer>> iterator = players.entrySet().iterator(); iterator.hasNext(); ) {
            Map.Entry<UUID, AGhostGamePlayer> entry = iterator.next();
            if (!entry.getKey().equals(exception)) {
                Player player = Bukkit.getPlayer(entry.getKey());

                if (player != null) {
                    plugin.getMessageManager().sendLang(player, langPath, resolvers);
                } else {
                    if (entry.getValue() instanceof AlivePlayer alivePlayer) {
                        MouseTrap trap = alivePlayer.getMouseTrapTrappedIn();

                        if (trap != null) {
                            trap.removePlayer(alivePlayer);
                        }
                    }

                    iterator.remove();
                }
            }
        }

        if (players.isEmpty()) {
            endGame(EndReason.GAME_EMPTY);
        }
    }

    public void broadcastAll(final @NotNull LangPath langPath, final @NotNull TagResolver... resolvers) {
        broadcastExcept(langPath, null, resolvers);
    }

    public void broadcastExcept(@NotNull LangPath langPath, final @Nullable UUID exception) {
        for (Iterator<Map.Entry<UUID, AGhostGamePlayer>> iterator = players.entrySet().iterator(); iterator.hasNext(); ) {
            Map.Entry<UUID, AGhostGamePlayer> entry = iterator.next();
            if (!entry.getKey().equals(exception)) {
                Player player = Bukkit.getPlayer(entry.getKey());

                if (player != null) {
                    plugin.getMessageManager().sendLang(player, langPath);
                } else {
                    if (entry.getValue() instanceof AlivePlayer alivePlayer) {
                        MouseTrap trap = alivePlayer.getMouseTrapTrappedIn();

                        if (trap != null) {
                            trap.removePlayer(alivePlayer);
                        }
                    }

                    iterator.remove();
                }
            }
        }

        if (players.isEmpty()) {
            endGame(EndReason.GAME_EMPTY);
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

    public @NotNull String getName_id() {
        return name_id;
    }

    public @NotNull GameState getGameState() {
        return gameState;
    }

    public @NotNull GhostGameConfig getConfig() {
        return config;
    }

    protected boolean areAllPlayersDead() {
        return players.values().stream().noneMatch(p -> p instanceof AlivePlayer);
    }

    public boolean isPlaying(final @NotNull Player player) {
        return this.players.containsKey(player.getUniqueId());
    }

    public void gainPoints(final @NotNull Player pointGainingPlayer, final int newGainedPoints) {
        if (players.containsKey(pointGainingPlayer.getUniqueId())) { // todo make better use of player - include in message something like <player got x points>?
            this.gainedPoints += newGainedPoints;

            if (gainedPoints >= getConfig().getPointGoal()) {
                endGame(EndReason.WIN);
            } else {
                float percent = (float) getConfig().getPointGoal() / gainedPoints;
                float stepSize = 1.0f / config.getPointGoal();

                for (Iterator<Map.Entry<UUID, AGhostGamePlayer>> iterator = players.entrySet().iterator(); iterator.hasNext(); ) {
                    Map.Entry<UUID, AGhostGamePlayer> entry = iterator.next();

                    Player playerInGame = Bukkit.getPlayer(entry.getKey());

                    if (playerInGame == null) {
                        if (entry.getValue() instanceof AlivePlayer alivePlayer) {
                            MouseTrap trap = alivePlayer.getMouseTrapTrappedIn();

                            if (trap != null) {
                                trap.removePlayer(alivePlayer);
                            }
                        }

                        iterator.remove();
                    } else {
                        playerInGame.setExp(percent);
                    }
                }

                if (players.isEmpty()) {
                    endGame(EndReason.ALL_DEAD);

                    return;
                }

                if (percent - 0.25f < stepSize) {
                    broadcastAll(GhostLangPath.GAME_POINTS_MILESTONE_25);
                } else if (percent - 0.5f < stepSize) {
                    broadcastAll(GhostLangPath.GAME_POINTS_MILESTONE_50);
                } else if (percent - 0.75f < stepSize) {
                    broadcastAll(GhostLangPath.GAME_POINTS_MILESTONE_75);
                } else if (percent - 0.9f < stepSize) {
                    broadcastAll(GhostLangPath.GAME_POINTS_MILESTONE_75);
                }
            }
        }
    }

    public @NotNull Set<MouseTrap> getMouseTraps() {
        return mouseTraps;
    }

    public enum EndReason {
        EXTERN,
        TIME,
        ALL_DEAD,
        GAME_EMPTY,
        WIN,
    }

    public enum GameState {
        IDLE,
        STARTING,
        RUNNING,
        RESETTING
    }
}
