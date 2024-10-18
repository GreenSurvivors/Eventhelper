package de.greensurvivors.eventhelper.modules.ghost;

import de.greensurvivors.eventhelper.EventHelper;
import de.greensurvivors.eventhelper.messages.LangPath;
import de.greensurvivors.eventhelper.messages.MessageManager;
import de.greensurvivors.eventhelper.messages.SharedPlaceHolder;
import de.greensurvivors.eventhelper.modules.ghost.ghostEntity.IGhost;
import de.greensurvivors.eventhelper.modules.ghost.payer.*;
import de.greensurvivors.simplequests.SimpleQuests;
import de.greensurvivors.simplequests.events.QuestCompleatedEvent;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serial;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Pattern;

// todo automatic set amount of ghasts per amount of players
public class GhostGame implements Listener { // todo spectating command
    private final static @NotNull Pattern gameNamePattern = Pattern.compile("\\{gameName}");

    private final @NotNull EventHelper plugin;
    private final @NotNull GhostModul ghostModul;
    private final @NotNull GhostGameConfig config;
    private final @NotNull String name_id;

    private final @NotNull Map<@NotNull UUID, @NotNull AGhostGamePlayer> offlinePlayers = new HashMap<>();
    private final @NotNull Map<@NotNull UUID, @NotNull AGhostGamePlayer> players = new HashMap<>();
    private final @NotNull Map<@NotNull UUID, @NotNull SpectatingPlayer> spectators = new HashMap<>();
    private final @NotNull Set<@NotNull IGhost> ghosts = new HashSet<>();
    private final @NotNull Scoreboard scoreboard;
    private final @NotNull Team perishedTeam;
    private @NotNull GameState gameState;
    private long amountOfTicksRun;
    private @Nullable BukkitTask timeTask = null; // this has to be sync!
    private double gainedPoints = 0;

    public GhostGame(final @NotNull EventHelper plugin, final @NotNull GhostModul modul, final @NotNull String name_id) {
        this.plugin = plugin;
        this.ghostModul = modul;
        this.name_id = name_id;

        this.gameState = GameState.IDLE;
        this.config = new GhostGameConfig(plugin, name_id, modul);

        scoreboard = plugin.getServer().getScoreboardManager().getNewScoreboard();

        perishedTeam = scoreboard.registerNewTeam("perished");
        perishedTeam.setOption(Team.Option.COLLISION_RULE, Team.OptionStatus.NEVER);
        perishedTeam.setCanSeeFriendlyInvisibles(true);
    }

    @EventHandler(ignoreCancelled = true)
    private void onPlayerQuit(final @NotNull PlayerQuitEvent event) {
        if (players.containsKey(event.getPlayer().getUniqueId()) ||
            spectators.containsKey(event.getPlayer().getUniqueId())) {
            playerQuit(event.getPlayer(), true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    private void onPlayerChangeWorld(final @NotNull PlayerChangedWorldEvent event) {
        if (players.containsKey(event.getPlayer().getUniqueId()) ||
            spectators.containsKey(event.getPlayer().getUniqueId())) {
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
        AGhostGamePlayer ghostGamePlayer = players.get(event.getPlayer().getUniqueId());

        if (ghostGamePlayer != null) {
            final QuestModifier oldQuestModifier = ghostGamePlayer.getQuestModifier();
            if (oldQuestModifier != null && oldQuestModifier.getQuestIdentifier().equalsIgnoreCase(event.getQuest().getQuestIdentifier())) {
                // disable old quest
                SimpleQuests.getInstance().getDatabaseManager().setTimesQuestFinished(event.getPlayer(), oldQuestModifier.getRequiredQuestIdentifier(), 0);

                Double gainedPoints = getConfig().getPointsOfTask(event.getQuest().getQuestIdentifier());

                if (gainedPoints != null) {
                    gainPoints(ghostGamePlayer, gainedPoints);
                }

                final @Nullable QuestModifier newQuestModifier = ghostGamePlayer.finishCurrentQuest();

                if (newQuestModifier != null) { // enable new quest
                    SimpleQuests.getInstance().getDatabaseManager().setTimesQuestFinished(event.getPlayer(), newQuestModifier.getRequiredQuestIdentifier(), 1);
                }
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    private void onPlayerDeath(final @NotNull PlayerDeathEvent event) {
        final AGhostGamePlayer ghostGamePlayer = players.get(event.getPlayer().getUniqueId());
        if (ghostGamePlayer instanceof AlivePlayer) {
            makePerishedPlayer(event.getPlayer().getUniqueId());
            event.getPlayer().teleportAsync(getConfig().getPlayerStartLocation());
            event.setCancelled(true);
        } else if (ghostGamePlayer instanceof DeadPlayer) {
            event.getPlayer().teleportAsync(getConfig().getPlayerStartLocation());
            event.setCancelled(true);
        } else if (spectators.containsKey(event.getPlayer().getUniqueId())) { // how?
            event.getPlayer().teleportAsync(getConfig().getPlayerStartLocation());
            event.setCancelled(true);
        }
    }

    public void startStartingCountdown() {
        gameState = GameState.COUNTDOWN;
        tickCountdown(10);

        for (String cmd : getConfig().getGameInitCommands()) {
            plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), gameNamePattern.matcher(cmd).replaceAll(getName_id()));
        }
    }

    protected void tickCountdown(int secondsRemain) {
        if (secondsRemain > 0) {
            for (Iterator<Map.Entry<UUID, AGhostGamePlayer>> iterator = players.entrySet().iterator(); iterator.hasNext(); ) {
                Map.Entry<UUID, AGhostGamePlayer> entry = iterator.next();
                Player player = plugin.getServer().getPlayer(entry.getKey());

                if (player != null) {
                    plugin.getMessageManager().sendLang(player, GhostLangPath.GAME_COUNTDOWN,
                        Placeholder.unparsed(SharedPlaceHolder.NUMBER.getKey(), String.valueOf(secondsRemain)));
                    player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.MASTER, 1.0f, 1.0f);
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
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> tickCountdown(secondsRemain - 1), 20);
        } else {
            startGame();
        }
    }

    protected void startGame() {
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
            Player player = plugin.getServer().getPlayer(entry.getKey());

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

        for (String cmd : getConfig().getGameStartCommands()) {
            plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), gameNamePattern.matcher(cmd).replaceAll(getName_id()));
        }

        timeTask = plugin.getServer().getScheduler().runTaskTimer(plugin, this::tick, 0, 1);
        gameState = GameState.RUNNING;
    }

    protected void tick() {
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
                        Player player = plugin.getServer().getPlayer(entry.getKey());

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

                    for (MouseTrap mouseTrap : getConfig().getMouseTraps()) {
                        for (Map.Entry<AlivePlayer, Long> entry : mouseTrap.getTrappedPlayers().entrySet()) {
                            Duration durationToStayAlive = getConfig().getDurationTrappedUntilDeath().minusMillis(entry.getValue());
                            if (durationToStayAlive.toMillis() < 0) {
                                makePerishedPlayer(entry.getKey().getUuid());

                                if (areAllPlayersDead()) {
                                    endGame(EndReason.ALL_DEAD);
                                }
                            } else {
                                switch ((int) durationToStayAlive.toSeconds()) {
                                    // don't do full countdown from 10 to 1, if many players got trapped around the same time the game will spam otherwise
                                    case 1, 2, 3, 4, 5, 10, 30, 60, 120 ->
                                        broadcastAll(GhostLangPath.PLAYER_TRAP_TIME_REMAINING,
                                            Placeholder.component(SharedPlaceHolder.TIME.getKey(), MessageManager.formatTime(durationToStayAlive)));
                                    default -> {
                                        // check if the amount of seconds left is in function f(0) = 300, f(x+1) = 2 * f(x)
                                        // so 5m, 10m, 20m, 40m... But please, for the love of cod, don't make use of this case.
                                        // Players shouldn't stay 5 minutes or longer trapped. That's boring!
                                        // I will come and slap a fish in your face if you do!
                                        long temp = durationToStayAlive.toSeconds() / 300;
                                        if (temp > 0 && (temp & (temp - 1)) == 0) {
                                            broadcastAll(GhostLangPath.PLAYER_TRAP_TIME_REMAINING,
                                                Placeholder.component(SharedPlaceHolder.TIME.getKey(), MessageManager.formatTime(durationToStayAlive)));
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // just set to start time for players
                    for (Iterator<Map.Entry<UUID, AGhostGamePlayer>> iterator = players.entrySet().iterator(); iterator.hasNext(); ) {
                        Map.Entry<UUID, AGhostGamePlayer> entry = iterator.next();
                        Player player = plugin.getServer().getPlayer(entry.getKey());

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

        for (String cmd : getConfig().getGameEndCommands()) {
            plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), gameNamePattern.matcher(cmd).replaceAll(getName_id()));
        }

        resetGame();
    }

    public void resetGame() {
        gameState = GameState.RESETTING;

        for (MouseTrap mouseTrap : getConfig().getMouseTraps()) {
            mouseTrap.releaseAllPlayers();
        }

        for (Iterator<Map.Entry<UUID, AGhostGamePlayer>> iterator = players.entrySet().iterator(); iterator.hasNext(); ) {
            Map.Entry<UUID, AGhostGamePlayer> entry = iterator.next();
            @Nullable Player player = plugin.getServer().getPlayer(entry.getKey());
            if (player != null) {
                player.teleportAsync(config.getEndLocation());
            }

            entry.getValue().restorePlayer();
            iterator.remove();
        }
        offlinePlayers.clear();

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

    public void playerSpectate(final @NotNull Player player) {
        if (ghostModul.getGameParticipatingIn(player) != null) {
            makePlayerSpectator(player);
        } else {
            plugin.getMessageManager().sendLang(player, GhostLangPath.ERROR_ALREADY_PARTICIPATING);
        }
    }

    public void playerJoin(final @NotNull Player player) { // todo permission
        if (ghostModul.getGameParticipatingIn(player) != null) {
            switch (gameState) {
                case IDLE -> {
                    if (!isGameFull()) {
                        makeAlivePlayer(player);
                    } else {
                        plugin.getMessageManager().sendLang(player, GhostLangPath.ERROR_GAME_FULL);
                    }
                }
                case RUNNING -> {
                    if (config.isLateJoinAllowed()) {
                        if (!isGameFull()) {
                            AGhostGamePlayer offlinePlayer = offlinePlayers.get(player.getUniqueId());

                            if (offlinePlayer != null) {
                                if (config.isRejoinAllowed()) {
                                    // now sort them to what and where they were before quitting
                                    if (offlinePlayer instanceof AlivePlayer alivePlayer) {
                                        makeAlivePlayerAgain(alivePlayer);
                                    } else if (offlinePlayer instanceof DeadPlayer deadPlayer) {
                                        rePerishPlayer(deadPlayer);
                                    }
                                } else {
                                    plugin.getMessageManager().sendLang(player, GhostLangPath.ERROR_NO_REJOIN);
                                }
                            } else {
                                makeAlivePlayer(player);
                            }
                        } else {
                            plugin.getMessageManager().sendLang(player, GhostLangPath.ERROR_GAME_FULL);
                        }
                    } else {
                        plugin.getMessageManager().sendLang(player, GhostLangPath.ERROR_NO_LATE_JOIN);
                    }
                }
                case COUNTDOWN, STARTING, RESETTING ->
                    plugin.getMessageManager().sendLang(player, GhostLangPath.ERROR_JOIN_GAME_STATE);
            }
        } else {
            plugin.getMessageManager().sendLang(player, GhostLangPath.ERROR_ALREADY_PARTICIPATING);
        }
    }

    protected void makeAlivePlayer(final @NotNull Player player) {
        final AlivePlayer alivePlayer = new AlivePlayer(plugin, this, player.getUniqueId());
        players.put(player.getUniqueId(), alivePlayer);

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

        for (QuestModifier questModifier : getConfig().getTasks().values()) {
            if (questModifier.getQuestIdentifier().equals(alivePlayer.getQuestModifier().getQuestIdentifier())) {
                SimpleQuests.getInstance().getDatabaseManager().setTimesQuestFinished(player, questModifier.getRequiredQuestIdentifier(), 1);
            } else {
                SimpleQuests.getInstance().getDatabaseManager().setTimesQuestFinished(player, questModifier.getRequiredQuestIdentifier(), 0);
            }
        }

        player.setGameMode(GameMode.SURVIVAL);

        plugin.getMessageManager().sendLang(player, GhostLangPath.PLAYER_GAME_JOIN,
            Placeholder.component(SharedPlaceHolder.TEXT.getKey(), getConfig().getDisplayName()));
        broadcastExcept(GhostLangPath.PLAYER_GAME_JOIN_BROADCAST, player.getUniqueId(),
            Placeholder.component(SharedPlaceHolder.PLAYER.getKey(), player.displayName()));
    }

    // used for rejoining a game
    protected void makeAlivePlayerAgain(final @NotNull AlivePlayer oldAlivePlayer) {
        final AlivePlayer newAlivePlayer = new AlivePlayer(oldAlivePlayer);
        players.put(oldAlivePlayer.getUuid(), newAlivePlayer);
        Player player = oldAlivePlayer.getBukkitPlayer();

        if (oldAlivePlayer.getMouseTrapTrappedIn() == null) {
            player.teleportAsync(config.getLobbyLocation());
        } else {
            newAlivePlayer.trapInMouseTrap(oldAlivePlayer.getMouseTrapTrappedIn());
        }

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
        final @Nullable AGhostGamePlayer ghostGamePlayer = players.get(player.getUniqueId());

        if (ghostGamePlayer == null) {// used when joining as spectator
            makePlayerSpectator(player, new PlayerData(plugin, player));
        } else {// used when dying permanently
            makePlayerSpectator(player, ghostGamePlayer.getPlayerData());
            players.remove(player.getUniqueId());

            if (players.isEmpty()) {
                if (ghostGamePlayer instanceof AlivePlayer) { // should never happen
                    endGame(EndReason.ALL_DEAD);
                } else {
                    endGame(EndReason.GAME_EMPTY);
                }
            }
        }
    }

    protected void makePlayerSpectator(final @NotNull Player player, final @NotNull PlayerData playerData) {
        players.remove(player.getUniqueId());
        spectators.put(player.getUniqueId(), new SpectatingPlayer(plugin, this, player.getUniqueId(), playerData));

        for (AGhostGamePlayer ghostGamePlayer : players.values()) {
            final @Nullable Player otherPlayer = ghostGamePlayer.getBukkitPlayer();

            if (otherPlayer != null) {
                otherPlayer.hidePlayer(plugin, player);
            }
        }
        for (Iterator<SpectatingPlayer> iterator = spectators.values().iterator(); iterator.hasNext(); ) {
            SpectatingPlayer otherSpectator = iterator.next();
            final @Nullable Player otherPlayer = otherSpectator.getBukkitPlayer();

            if (otherPlayer != null) {
                otherPlayer.showPlayer(plugin, player);
            } else {
                iterator.remove();
            }
        }

        player.setGameMode(GameMode.ADVENTURE);
        player.setInvulnerable(true);
        player.setAllowFlight(true);

        player.setScoreboard(scoreboard);
        perishedTeam.addPlayer(player);

        player.teleportAsync(getConfig().getSpectatorStartLocation());
    }

    protected void makePerishedPlayer(final @NotNull UUID uuid) throws NoAlivePlayerException {
        final @Nullable AGhostGamePlayer ghostGamePlayer = players.get(uuid);

        if (ghostGamePlayer instanceof AlivePlayer alivePlayer) {
            final @Nullable Player player = ghostGamePlayer.getBukkitPlayer();
            players.put(uuid, new DeadPlayer(plugin, this, uuid, alivePlayer.getPlayerData(), alivePlayer.generateGhostTasks()));

            perishedTeam.addPlayer(player);
            player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, -1, 0, false, false, false));
            player.setGameMode(GameMode.SURVIVAL);

        } else {
            throw new NoAlivePlayerException("Tried to perish player with UUID " + uuid + ", but they where not alive!");
        }
    }

    protected void rePerishPlayer(final @NotNull DeadPlayer deadPlayer) { // damn necromancers!
        final @Nullable Player player = deadPlayer.getBukkitPlayer();
        players.put(deadPlayer.getUuid(), new DeadPlayer(plugin, this, deadPlayer.getUuid(), new PlayerData(plugin, player), deadPlayer.getGhostTasks()));
        player.teleportAsync(config.getLobbyLocation());
        player.setGameMode(GameMode.SURVIVAL);

        perishedTeam.addPlayer(player);
        player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, -1, 0, false, false, false));

        for (SpectatingPlayer spectatingPlayer : spectators.values()) {
            player.hidePlayer(plugin, spectatingPlayer.getBukkitPlayer());
        }

        plugin.getMessageManager().sendLang(player, GhostLangPath.PLAYER_GAME_JOIN,
            Placeholder.component(SharedPlaceHolder.TEXT.getKey(), getConfig().getDisplayName()));
        broadcastExcept(GhostLangPath.PLAYER_GAME_JOIN_BROADCAST, player.getUniqueId(),
            Placeholder.component(SharedPlaceHolder.PLAYER.getKey(), player.displayName()));
    }

    public void playerQuit(final @NotNull Player player, boolean teleport) {
        final @Nullable AGhostGamePlayer ghostGamePlayer = players.get(player.getUniqueId());
        if (ghostGamePlayer != null) {
            if (teleport) {
                player.teleport(config.getEndLocation());
                player.resetPlayerTime();
            }

            // reset Scoreboard
            perishedTeam.removePlayer(player);
            // player will get potion effects removed and dead players will be visible again, after the player data was restored
            ghostGamePlayer.restorePlayer();

            if (ghostGamePlayer instanceof AlivePlayer alivePlayer) {
                MouseTrap trap = alivePlayer.getMouseTrapTrappedIn();

                if (trap != null) {
                    trap.removePlayer(alivePlayer);
                }
            } else if (ghostGamePlayer instanceof DeadPlayer) {

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

            final QuestModifier oldQuestModifier = ghostGamePlayer.getQuestModifier();
            if (oldQuestModifier != null) {
                // disable old quest
                SimpleQuests.getInstance().getDatabaseManager().setTimesQuestFinished(ghostGamePlayer.getBukkitPlayer(), oldQuestModifier.getRequiredQuestIdentifier(), 0);
            }

            offlinePlayers.put(player.getUniqueId(), players.remove(player.getUniqueId()));

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
                Player player = plugin.getServer().getPlayer(entry.getKey());

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
                Player player = plugin.getServer().getPlayer(entry.getKey());

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

    public @Nullable AGhostGamePlayer getGhostGamePlayer(final @NotNull Player player) {
        return this.players.get(player.getUniqueId());
    }

    public @Nullable AGhostGameParticipant getGhostGhostGameParticipant(final @NotNull Player player) {
        AGhostGamePlayer ghostGamePlayer = this.players.get(player.getUniqueId());
        return ghostGamePlayer == null ? spectators.get(player.getUniqueId()) : ghostGamePlayer;
    }

    public void gainPoints(final @NotNull AGhostGamePlayer pointGainingPlayer, final double newGainedPoints) { // todo message to player
        this.gainedPoints += newGainedPoints;

        if (gainedPoints >= getConfig().getPointGoal()) {
            endGame(EndReason.WIN);
        } else {
            float percent = (float) (getConfig().getPointGoal() / gainedPoints);
            float stepSize = 1.0f / config.getPointGoal();

            for (Iterator<Map.Entry<UUID, AGhostGamePlayer>> iterator = players.entrySet().iterator(); iterator.hasNext(); ) {
                Map.Entry<UUID, AGhostGamePlayer> entry = iterator.next();

                Player playerInGame = plugin.getServer().getPlayer(entry.getKey());

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
                broadcastAll(GhostLangPath.GAME_POINTS_MILESTONE_25,
                    Placeholder.component(SharedPlaceHolder.PLAYER.getKey(), pointGainingPlayer.getBukkitPlayer().displayName()),
                    Placeholder.unparsed(SharedPlaceHolder.NUMBER.getKey(), String.valueOf(newGainedPoints)));
            } else if (percent - 0.5f < stepSize) {
                broadcastAll(GhostLangPath.GAME_POINTS_MILESTONE_50,
                    Placeholder.component(SharedPlaceHolder.PLAYER.getKey(), pointGainingPlayer.getBukkitPlayer().displayName()),
                    Placeholder.unparsed(SharedPlaceHolder.NUMBER.getKey(), String.valueOf(newGainedPoints)));
            } else if (percent - 0.75f < stepSize) {
                broadcastAll(GhostLangPath.GAME_POINTS_MILESTONE_75,
                    Placeholder.component(SharedPlaceHolder.PLAYER.getKey(), pointGainingPlayer.getBukkitPlayer().displayName()),
                    Placeholder.unparsed(SharedPlaceHolder.NUMBER.getKey(), String.valueOf(newGainedPoints)));
            } else if (percent - 0.9f < stepSize) {
                broadcastAll(GhostLangPath.GAME_POINTS_MILESTONE_90,
                    Placeholder.component(SharedPlaceHolder.PLAYER.getKey(), pointGainingPlayer.getBukkitPlayer().displayName()),
                    Placeholder.unparsed(SharedPlaceHolder.NUMBER.getKey(), String.valueOf(newGainedPoints)));
            }
        }
    }

    public @NotNull Set<@NotNull MouseTrap> getMouseTraps() {
        return getConfig().getMouseTraps();
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
        COUNTDOWN,
        STARTING,
        RUNNING,
        RESETTING
    }

    protected static class NoAlivePlayerException extends IllegalArgumentException {
        @Serial
        private static final long serialVersionUID = 6702629890736910312L;

        /**
         * Constructs an {@code NoAlivePlayerException} with no
         * detail message.
         */
        public NoAlivePlayerException() {
            super();
        }

        /**
         * Constructs an {@code NoAlivePlayerException} with the
         * specified detail message.
         *
         * @param s the detail message.
         */
        public NoAlivePlayerException(String s) {
            super(s);
        }

        /**
         * Constructs a new exception with the specified detail message and
         * cause.
         *
         * <p>Note that the detail message associated with {@code cause} is
         * <i>not</i> automatically incorporated in this exception's detail
         * message.
         *
         * @param message the detail message (which is saved for later retrieval
         *                by the {@link Throwable#getMessage()} method).
         * @param cause   the cause (which is saved for later retrieval by the
         *                {@link Throwable#getCause()} method).  (A {@code null} value
         *                is permitted, and indicates that the cause is nonexistent or
         *                unknown.)
         */
        public NoAlivePlayerException(String message, Throwable cause) {
            super(message, cause);
        }

        /**
         * Constructs a new exception with the specified cause and a detail
         * message of {@code (cause==null ? null : cause.toString())} (which
         * typically contains the class and detail message of {@code cause}).
         * This constructor is useful for exceptions that are little more than
         * wrappers for other throwables (for example, {@link
         * java.security.PrivilegedActionException}).
         *
         * @param cause the cause (which is saved for later retrieval by the
         *              {@link Throwable#getCause()} method).  (A {@code null} value is
         *              permitted, and indicates that the cause is nonexistent or
         *              unknown.)
         */
        public NoAlivePlayerException(Throwable cause) {
            super(cause);
        }
    }
}
