package de.greensurvivors.eventhelper.modules.ghost;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import de.greensurvivors.eventhelper.EventHelper;
import de.greensurvivors.eventhelper.messages.LangPath;
import de.greensurvivors.eventhelper.messages.MessageManager;
import de.greensurvivors.eventhelper.messages.SharedPlaceHolder;
import de.greensurvivors.eventhelper.modules.ghost.ghostentity.IGhost;
import de.greensurvivors.eventhelper.modules.ghost.player.*;
import de.greensurvivors.eventhelper.modules.ghost.vex.IVex;
import de.greensurvivors.simplequests.SimpleQuests;
import de.greensurvivors.simplequests.events.QuestCompleatedEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.block.Block;
import org.bukkit.block.data.Powerable;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Range;

import java.io.Serial;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Pattern;

// todo automatic set amount of ghasts per amount of players
public class GhostGame implements Listener {
    private final static @NotNull Pattern gameNamePattern = Pattern.compile("\\{gameName}");

    private final @NotNull EventHelper plugin;
    private final @NotNull GhostModul ghostModul;
    private final @NotNull GhostGameConfig config;
    private final @NotNull String name_id;

    private final @NotNull Map<@NotNull UUID, @NotNull AGhostGamePlayer> offlinePlayers = new ConcurrentHashMap<>();
    private final @NotNull Map<@NotNull UUID, @NotNull AGhostGamePlayer> players = new ConcurrentHashMap<>();
    private final @NotNull Map<@NotNull UUID, @NotNull SpectatingPlayer> spectators = new ConcurrentHashMap<>();
    private final @NotNull Set<@NotNull IGhost> ghosts = new HashSet<>();
    private final @NotNull Set<@NotNull IVex> vexes = new HashSet<>();
    private final @NotNull Scoreboard scoreboard;
    private final @NotNull Team perishedTeam;
    private @NotNull GameState gameState;
    private long amountOfTicksRun;
    private @Nullable BukkitTask tickTask = null; // this has to be sync!
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

    public @NotNull GhostModul getGhostModul() {
        return ghostModul;
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

    @EventHandler(ignoreCancelled = true)
    private void onQuestComplete(final @NotNull QuestCompleatedEvent event) { // todo reset whole questline not just requirement and end
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
                } else if (ghostGamePlayer instanceof DeadPlayer) {
                    makePlayerSpectator(ghostGamePlayer.getBukkitPlayer());

                    broadcastExcept(GhostLangPath.PLAYER_PERISHED_TASK_DONE_BROADCAST, ghostGamePlayer.getUuid(),
                        Placeholder.component(SharedPlaceHolder.PLAYER.getKey(), ghostGamePlayer.getBukkitPlayer().displayName()));
                    plugin.getMessageManager().sendLang(ghostGamePlayer.getBukkitPlayer(), GhostLangPath.PLAYER_PERISHED_TASK_DONE_SELF);
                }
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    private void onPlayerDeath(final @NotNull PlayerDeathEvent event) {
        Player player = event.getPlayer();
        final AGhostGamePlayer ghostGamePlayer = players.get(player.getUniqueId());
        if (ghostGamePlayer instanceof AlivePlayer alivePlayer) {
            if (wouldAllPlayersBeDead(alivePlayer)) {
                // don't do things you have immediately undo after or maybe even can't easily undo like an async tp
                plugin.getServer().getScheduler().runTask(plugin, () -> endGame(EndReason.ALL_DEAD));
            } else {
                if (alivePlayer.getMouseTrapTrappedIn() == null) { // if a player already was trapped, do nothing
                    final List<@NotNull MouseTrap> mouseTraps = getMouseTraps();
                    alivePlayer.trapInMouseTrap(mouseTraps.get(ThreadLocalRandom.current().nextInt(mouseTraps.size())));
                }

                // even after canceling the event, health level gets reset. So at least reset them to the expected amount.
                player.setHealth(config.getStartingHealthAmount());
                player.setSaturation(config.getStartingSaturationAmount());
                player.setFoodLevel(config.getStartingFoodAmount());
            }

            event.setCancelled(true);
        } else if (ghostGamePlayer instanceof DeadPlayer deadPlayer) {
            event.getPlayer().teleportAsync(getConfig().getPlayerStartLocation());
            makePlayerSpectator(deadPlayer.getBukkitPlayer());
            event.setCancelled(true);
        } else if (spectators.containsKey(event.getPlayer().getUniqueId())) { // how?
            event.getPlayer().teleportAsync(getConfig().getPlayerStartLocation());
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    private void onTrapButtonPress(final @NotNull PlayerInteractEvent event) {
        Block clickedBlock = event.getClickedBlock();
        if (clickedBlock != null) {
            if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
                if (Tag.BUTTONS.isTagged(clickedBlock.getType())) {
                    final AGhostGamePlayer ghostGamePlayer = players.get(event.getPlayer().getUniqueId());
                    // ignore interactions outside the game
                    if (ghostGamePlayer == null && spectators.get(event.getPlayer().getUniqueId()) == null) {
                        return;
                    }

                    for (MouseTrap mouseTrap : getMouseTraps()) {
                        if (mouseTrap.isReleaseBlockLocation(clickedBlock.getLocation())) {
                            // don't allow dead or spectating players to free anyone
                            if (!(ghostGamePlayer instanceof AlivePlayer)) {
                                plugin.getMessageManager().sendLang(event.getPlayer(), GhostLangPath.PLAYER_TRAP_ONLY_ALIVE_CAN_RELEASE);

                                event.setCancelled(true);
                                return;
                            }

                            final Map<@NotNull AlivePlayer, @NotNull Long> trappedPlayers = mouseTrap.getTrappedPlayers();

                            if (trappedPlayers.isEmpty()) {
                                continue;
                            }

                            if (clickedBlock.getBlockData() instanceof Powerable powerable) {
                                powerable.setPowered(true);

                                if (Tag.STONE_BUTTONS.isTagged(clickedBlock.getType())) { // stone buttons do faster power off again
                                    plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                                        powerable.setPowered(false);
                                        clickedBlock.getLocation().getWorld().playSound(clickedBlock.getLocation(), Sound.BLOCK_STONE_BUTTON_CLICK_OFF, SoundCategory.BLOCKS, 1.0f, 1.0f);
                                    }, 20);
                                    clickedBlock.getLocation().getWorld().playSound(clickedBlock.getLocation(), Sound.BLOCK_STONE_BUTTON_CLICK_ON, SoundCategory.BLOCKS, 1.0f, 1.0f);
                                } else {

                                    /* If I had a nickel for every time I had to emulate BlockSetType sounds,
                                    I'd have two nickels.
                                    Which isn't a lot, but it's weird that it happened twice.
                                    */
                                    switch (clickedBlock.getType()) {
                                        case CHERRY_BUTTON ->
                                            clickedBlock.getLocation().getWorld().playSound(clickedBlock.getLocation(), Sound.BLOCK_CHERRY_WOOD_BUTTON_CLICK_ON, SoundCategory.BLOCKS, 1.0f, 1.0f);
                                        case CRIMSON_BUTTON, WARPED_BUTTON ->
                                            clickedBlock.getLocation().getWorld().playSound(clickedBlock.getLocation(), Sound.BLOCK_NETHER_WOOD_BUTTON_CLICK_ON, SoundCategory.BLOCKS, 1.0f, 1.0f);
                                        case BAMBOO_BUTTON ->
                                            clickedBlock.getLocation().getWorld().playSound(clickedBlock.getLocation(), Sound.BLOCK_BAMBOO_WOOD_BUTTON_CLICK_ON, SoundCategory.BLOCKS, 1.0f, 1.0f);
                                        default ->
                                            clickedBlock.getLocation().getWorld().playSound(clickedBlock.getLocation(), Sound.BLOCK_WOODEN_BUTTON_CLICK_ON, SoundCategory.BLOCKS, 1.0f, 1.0f);
                                    }

                                    plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                                        powerable.setPowered(false);

                                        switch (clickedBlock.getType()) {
                                            case CHERRY_BUTTON ->
                                                clickedBlock.getLocation().getWorld().playSound(clickedBlock.getLocation(), Sound.BLOCK_CHERRY_WOOD_BUTTON_CLICK_OFF, SoundCategory.BLOCKS, 1.0f, 1.0f);
                                            case CRIMSON_BUTTON, WARPED_BUTTON ->
                                                clickedBlock.getLocation().getWorld().playSound(clickedBlock.getLocation(), Sound.BLOCK_NETHER_WOOD_BUTTON_CLICK_OFF, SoundCategory.BLOCKS, 1.0f, 1.0f);
                                            case BAMBOO_BUTTON ->
                                                clickedBlock.getLocation().getWorld().playSound(clickedBlock.getLocation(), Sound.BLOCK_BAMBOO_WOOD_BUTTON_CLICK_OFF, SoundCategory.BLOCKS, 1.0f, 1.0f);
                                            default ->
                                                clickedBlock.getLocation().getWorld().playSound(clickedBlock.getLocation(), Sound.BLOCK_WOODEN_BUTTON_CLICK_OFF, SoundCategory.BLOCKS, 1.0f, 1.0f);
                                        }
                                    }, 30);
                                }
                            }

                            broadcastAll(GhostLangPath.PLAYER_TRAP_RELEASE_BROADCAST,
                                Placeholder.component(SharedPlaceHolder.PLAYER.getKey(), event.getPlayer().displayName()),
                                Placeholder.component(SharedPlaceHolder.TEXT.getKey(), Component.join(JoinConfiguration.commas(true),
                                    trappedPlayers.keySet().stream().map(player -> player.getBukkitPlayer().displayName()).toList())));
                            mouseTrap.releaseAllPlayers();
                            event.setCancelled(true);

                            return; // only one trap per button to not overload on sounds.
                        }
                    }
                }
            }
        }
    }

    public void startStartingCountdown() {
        gameState = GameState.COUNTDOWN;
        doCountdown(10);

        for (String cmd : getConfig().getGameInitCommands()) {
            plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), gameNamePattern.matcher(cmd).replaceAll(getName_id()));
        }
    }

    protected void doCountdown(int secondsRemain) {
        if (secondsRemain > 0) {
            for (Iterator<Map.Entry<UUID, AGhostGamePlayer>> iterator = players.entrySet().iterator(); iterator.hasNext(); ) {
                Map.Entry<UUID, AGhostGamePlayer> entry = iterator.next();
                Player player = plugin.getServer().getPlayer(entry.getKey());

                if (player != null) {
                    plugin.getMessageManager().sendLang(player, GhostLangPath.GAME_COUNTDOWN,
                        Placeholder.component(SharedPlaceHolder.TIME.getKey(), MessageManager.formatTime(Duration.ofSeconds(secondsRemain))));
                    player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.MASTER, 1.0f, 1.0f);
                } else {
                    if (entry.getValue() instanceof AlivePlayer alivePlayer) {
                        MouseTrap trap = alivePlayer.getMouseTrapTrappedIn();

                        if (trap != null) {
                            trap.removePlayer(alivePlayer);
                            alivePlayer.releaseFromTrap();
                        }
                    }

                    iterator.remove();
                }
            }
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> doCountdown(secondsRemain - 1), 20);
        } else {
            startGame();
        }
    }

    protected void startGame() {
        gameState = GameState.STARTING;
        Random random = ThreadLocalRandom.current();

        final List<@NotNull Location> ghostSpawnLocations = config.getGhostSpawnLocations();
        for (int i = 0; i < config.getAmountOfGhosts(); i++) {
            Location spawnLocation = ghostSpawnLocations.get(random.nextInt(ghostSpawnLocations.size()));

            IGhost newGhost = IGhost.spawnNew(spawnLocation, CreatureSpawnEvent.SpawnReason.CUSTOM, this, ghost -> {
                ghost.setPersistent(true);  // don't despawn
                ghost.setCollidable(false); // don't be a push around
                //ghost.setInvulnerable(true);
            });

            ghosts.add(newGhost);
        }

        final @NotNull List<@NotNull Location> vexSpawnLocations = config.getVexSpawnLocations();
        if (!vexSpawnLocations.isEmpty()) {
            for (int i = 0; i < config.getAmountOfVexes(); i++) {
                Location spawnLocation = vexSpawnLocations.get(random.nextInt(vexSpawnLocations.size()));

                IVex newGhost = IVex.spawnNew(spawnLocation, CreatureSpawnEvent.SpawnReason.CUSTOM, this, vex -> {
                    vex.setPersistent(true);  // don't despawn
                    vex.setCollidable(false); // don't be a push around
                    //vex.setInvulnerable(true);
                });

                vexes.add(newGhost);
            }
        }

        if (tickTask != null) {
            tickTask.cancel();
        }

        for (Iterator<Map.Entry<UUID, AGhostGamePlayer>> iterator = players.entrySet().iterator(); iterator.hasNext(); ) {
            Map.Entry<UUID, AGhostGamePlayer> entry = iterator.next();
            Player player = plugin.getServer().getPlayer(entry.getKey());

            if (player == null) {
                if (entry.getValue() instanceof AlivePlayer alivePlayer) {
                    MouseTrap trap = alivePlayer.getMouseTrapTrappedIn();

                    if (trap != null) {
                        trap.removePlayer(alivePlayer);
                        alivePlayer.releaseFromTrap();
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

        tickTask = plugin.getServer().getScheduler().runTaskTimer(plugin, this::tick, 0, 1);
        gameState = GameState.RUNNING;
    }

    @SuppressWarnings("UnstableApiUsage") // tick manager
    protected void tick() {
        amountOfTicksRun++;

        long gameDurationInTicks = config.getGameDuration().toMillis() / 50;

        if (amountOfTicksRun <= gameDurationInTicks) {
            float tickRate = plugin.getServer().getServerTickManager().getTickRate();
            double millisPerTick = 1000D / tickRate;

            // tick player time
            if (config.getStartPlayerTime() >= 0 && config.getEndPlayerTime() >= 0) {
                long playerTimeSpan = config.getEndPlayerTime() - config.getStartPlayerTime();

                if (playerTimeSpan > 0) {
                    long playerTimeNow = config.getStartPlayerTime() + amountOfTicksRun * playerTimeSpan / gameDurationInTicks;

                    // set playerTime for alive players
                    for (Iterator<Map.Entry<UUID, AGhostGamePlayer>> iterator = players.entrySet().iterator(); iterator.hasNext(); ) {
                        Map.Entry<UUID, AGhostGamePlayer> entry = iterator.next();
                        Player player = plugin.getServer().getPlayer(entry.getKey());

                        if (player == null) {
                            if (entry.getValue() instanceof AlivePlayer alivePlayer) {
                                MouseTrap trap = alivePlayer.getMouseTrapTrappedIn();

                                if (trap != null) {
                                    trap.removePlayer(alivePlayer);
                                    alivePlayer.releaseFromTrap();
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
                        Player player = plugin.getServer().getPlayer(entry.getKey());

                        if (player == null) {
                            if (entry.getValue() instanceof AlivePlayer alivePlayer) {
                                MouseTrap trap = alivePlayer.getMouseTrapTrappedIn();

                                if (trap != null) {
                                    trap.removePlayer(alivePlayer);
                                    alivePlayer.releaseFromTrap();
                                }
                            }

                            plugin.getComponentLogger().error("Removed player \"{}\" from game since they got missing. They couldn't get reset correctly!", entry.getKey());
                            iterator.remove();
                        } else {
                            player.setPlayerTime(config.getStartPlayerTime(), false);
                        }
                    }
                }
            }

            // tick players in mousetraps
            for (MouseTrap mouseTrap : getConfig().getMouseTraps()) {
                for (Iterator<Map.Entry<AlivePlayer, Long>> iterator = mouseTrap.getTrappedPlayers().entrySet().iterator(); iterator.hasNext(); ) {
                    Map.Entry<AlivePlayer, Long> entry = iterator.next();

                    Duration durationToStayAlive = getConfig().getDurationTrappedUntilDeath().minusMillis(System.currentTimeMillis() - entry.getValue());
                    final long millisToStayAlive = durationToStayAlive.toMillis();
                    if (millisToStayAlive < 0) {
                        iterator.remove();

                        makePerishedPlayer(entry.getKey().getUuid());
                        entry.getKey().getBukkitPlayer().teleportAsync(getConfig().getPlayerStartLocation());

                        if (areAllPlayersDead()) {
                            endGame(EndReason.ALL_DEAD);
                        } else {
                            broadcastAll(GhostLangPath.PLAYER_TRAP_PERISH,
                                Placeholder.component(SharedPlaceHolder.PLAYER.getKey(), entry.getKey().getBukkitPlayer().displayName()));
                        }
                    } else {
                        // checking in seconds will spam, because of 20 ticks / second => 20 messages
                        // don't do full countdown from 10000 to 1000, if many players got trapped around the same time the game will spam otherwise
                        if (((millisToStayAlive <= 1000 + millisPerTick) && (millisToStayAlive > 1000)) || // >=1s
                            ((millisToStayAlive <= 2000 + millisPerTick) && (millisToStayAlive > 2000)) || // >=2s
                            ((millisToStayAlive <= 3000 + millisPerTick) && (millisToStayAlive > 3000)) || // >=3s
                            ((millisToStayAlive <= 4000 + millisPerTick) && (millisToStayAlive > 4000)) || // >=4s
                            ((millisToStayAlive <= 5000 + millisPerTick) && (millisToStayAlive > 5000)) || // >=5s
                            ((millisToStayAlive <= 10000 + millisPerTick) && (millisToStayAlive > 10000)) || // >=10s
                            ((millisToStayAlive <= 30000 + millisPerTick) && (millisToStayAlive > 30000)) || // >=30s
                            ((millisToStayAlive <= 60000 + millisPerTick) && (millisToStayAlive > 60000)) || // >=1m
                            ((millisToStayAlive <= 120000 + millisPerTick) && (millisToStayAlive > 120000))) { // >=2m

                            broadcastAll(GhostLangPath.PLAYER_TRAP_TIME_REMAINING,
                                Placeholder.component(SharedPlaceHolder.TIME.getKey(), MessageManager.formatTime(durationToStayAlive.plusMillis((long) millisPerTick).truncatedTo(ChronoUnit.SECONDS))), // hide ticks
                                Placeholder.component(SharedPlaceHolder.PLAYER.getKey(), entry.getKey().getBukkitPlayer().displayName()));
                        } else {// check if the amount of millis left is in function f(0) = 300000, f(x+1) = 2 * f(x)
                            // so 5m, 10m, 20m, 40m... But please, for the love of cod, don't make use of this case.
                            // Players shouldn't stay 5 minutes or longer trapped. That's boring!
                            // I will come and slap a fish in your face if you do!
                            if (millisToStayAlive > 120000 && millisToStayAlive % 300000 <= millisPerTick) {
                                broadcastAll(GhostLangPath.PLAYER_TRAP_TIME_REMAINING,
                                    Placeholder.component(SharedPlaceHolder.TIME.getKey(), MessageManager.formatTime(durationToStayAlive.plusMillis((long) millisPerTick).truncatedTo(ChronoUnit.SECONDS))), // hide ticks
                                    Placeholder.component(SharedPlaceHolder.PLAYER.getKey(), entry.getKey().getBukkitPlayer().displayName()));
                            }
                        }
                    }
                }
            }

            // tick unsafe areas
            for (AGhostGamePlayer ghostGamePlayer : players.values()) {
                if (ghostGamePlayer instanceof AlivePlayer alivePlayer) {
                    alivePlayer.tickUnsafeAreas(millisPerTick);
                }
            }

            // tick feeding
            if ((amountOfTicksRun * millisPerTick) % getConfig().getFeedDuration().toMillis() == 0) {
                for (AGhostGamePlayer ghostGamePlayer : players.values()) {
                    final Player bukkitPlayer = ghostGamePlayer.getBukkitPlayer();

                    // skip feeding the player if worldguard is enabled but the player wasn't in the correct region
                    // todo somehow make caching work without hard depending on worldguard
                    if (plugin.getDependencyManager().isWorldGuardEnabled()) {
                        com.sk89q.worldedit.world.World weWorld = BukkitAdapter.adapt(bukkitPlayer.getWorld());
                        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
                        RegionManager rm = container.get(weWorld);

                        if (rm != null) {
                            ProtectedRegion protectedRegion = rm.getRegion(getConfig().getFeedRegionName());

                            com.sk89q.worldedit.entity.Player wePlayer = BukkitAdapter.adapt(bukkitPlayer);
                            if (!protectedRegion.contains(wePlayer.getLocation().toVector().toBlockPoint())) {
                                continue;
                            }
                        }
                    }

                    int foodLevel = bukkitPlayer.getFoodLevel();

                    if (foodLevel < getConfig().getFeedMaxAmount()) {
                        bukkitPlayer.setFoodLevel(foodLevel + getConfig().getFeedAmount());
                    }
                }
            }

            // check if players just perished
            if (players.isEmpty()) {
                endGame(EndReason.GAME_EMPTY);
            }
        } else {
            endGame(EndReason.TIME);
        }
    }

    public void disableGame() {
        endGame(GhostGame.EndReason.EXTERN);

        for (SpectatingPlayer spectatingPlayer : spectators.values()) {
            playerQuit(spectatingPlayer.getBukkitPlayer(), true);
        }

        HandlerList.unregisterAll(this);
    }

    public void endGame(final @NotNull EndReason reason) {
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
                playerQuit(player, true);
            }

            iterator.remove();
        }
        offlinePlayers.clear();

        for (Iterator<Map.Entry<UUID, SpectatingPlayer>> iterator = spectators.entrySet().iterator(); iterator.hasNext(); ) {
            Map.Entry<UUID, SpectatingPlayer> entry = iterator.next();
            @Nullable Player player = plugin.getServer().getPlayer(entry.getKey());
            if (player != null) {
                playerQuit(player, true);
            }

            iterator.remove();
        }

        // kill entities
        for (IGhost ghost : ghosts) {
            ghost.remove();
        }
        ghosts.clear();
        for (IVex vex : vexes) {
            vex.remove();
        }
        vexes.clear();

        if (tickTask != null) {
            tickTask.cancel();
        }
        gainedPoints = 0;
        amountOfTicksRun = 0;
        gameState = GameState.IDLE;
    }

    public boolean isRoomForAnotherPlayer() {
        return config.getMaxAmountPlayers() == -1 || players.size() <= config.getMaxAmountPlayers();
    }

    public void playerSpectate(final @NotNull Player player) {
        if (ghostModul.getGameParticipatingIn(player) == null) {
            makePlayerSpectator(player);
        } else {
            plugin.getMessageManager().sendLang(player, GhostLangPath.ERROR_ALREADY_PARTICIPATING);
        }
    }

    public void playerJoin(final @NotNull Player player) { // todo permission
        if (ghostModul.getGameParticipatingIn(player) == null) {
            switch (gameState) {
                case IDLE -> {
                    if (isRoomForAnotherPlayer()) {
                        makeAlivePlayer(player);
                        player.teleportAsync(config.getLobbyLocation());
                    } else {
                        plugin.getMessageManager().sendLang(player, GhostLangPath.ERROR_GAME_FULL);
                    }
                }
                case RUNNING -> {
                    if (config.isLateJoinAllowed()) {
                        if (isRoomForAnotherPlayer()) {
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
                                player.teleportAsync(config.getPlayerStartLocation());
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
            if (alivePlayer.getQuestModifier() != null && questModifier.getQuestIdentifier().equals(alivePlayer.getQuestModifier().getQuestIdentifier())) {
                SimpleQuests.getInstance().getDatabaseManager().setTimesQuestFinished(player, questModifier.getRequiredQuestIdentifier(), 1);
            } else {
                SimpleQuests.getInstance().getDatabaseManager().setTimesQuestFinished(player, questModifier.getRequiredQuestIdentifier(), 0);
            }

            SimpleQuests.getInstance().getDatabaseManager().setTimesQuestFinished(player, questModifier.getQuestIdentifier(), 0);
        }

        player.setGameMode(GameMode.SURVIVAL);

        AttributeInstance healthAttribute = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        if (config.getStartingHealthAmount() >= healthAttribute.getValue()) {
            healthAttribute.setBaseValue(config.getStartingHealthAmount());
        }
        player.setHealth(config.getStartingHealthAmount());
        player.setSaturation(config.getStartingSaturationAmount());
        player.setFoodLevel(config.getStartingFoodAmount());

        plugin.getMessageManager().sendLang(player, GhostLangPath.PLAYER_GAME_JOIN,
            Placeholder.component(SharedPlaceHolder.TEXT.getKey(), getConfig().getDisplayName()));
        broadcastExcept(GhostLangPath.PLAYER_GAME_JOIN_BROADCAST, player.getUniqueId(),
            Placeholder.component(SharedPlaceHolder.PLAYER.getKey(), player.displayName()));
    }

    // used for rejoining a game
    protected void makeAlivePlayerAgain(final @NotNull AlivePlayer oldAlivePlayer) {
        final @Nullable Player player = oldAlivePlayer.getBukkitPlayer();
        if (player == null) {
            return;
        }

        final AlivePlayer newAlivePlayer = new AlivePlayer(oldAlivePlayer);
        players.put(oldAlivePlayer.getUuid(), newAlivePlayer);

        if (oldAlivePlayer.getMouseTrapTrappedIn() == null) {
            player.teleportAsync(config.getPlayerStartLocation());
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

        final QuestModifier oldAlivePlayerQuestModifier = oldAlivePlayer.getQuestModifier();

        for (QuestModifier questModifier : getConfig().getTasks().values()) {
            if (oldAlivePlayerQuestModifier != null && questModifier.getQuestIdentifier().equals(oldAlivePlayerQuestModifier.getQuestIdentifier())) {
                SimpleQuests.getInstance().getDatabaseManager().setTimesQuestFinished(player, questModifier.getRequiredQuestIdentifier(), 1);
            } else {
                SimpleQuests.getInstance().getDatabaseManager().setTimesQuestFinished(player, questModifier.getRequiredQuestIdentifier(), 0);
            }

            SimpleQuests.getInstance().getDatabaseManager().setTimesQuestFinished(player, questModifier.getQuestIdentifier(), 0);
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

    /**
     * technical internal method, don't use.
     * Instead, use {@link #makePlayerSpectator(Player)} even if you have easy access to the player data!
     * using this method means you are missing out of a potential check on game end!
     **/
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

        plugin.getMessageManager().sendLang(player, GhostLangPath.PLAYER_START_SPECTATING);
    }

    protected void makePerishedPlayer(final @NotNull UUID uuid) throws PlayerNotAliveException {
        final @Nullable AGhostGamePlayer ghostGamePlayer = players.get(uuid);

        if (ghostGamePlayer instanceof AlivePlayer alivePlayer) {
            final @Nullable Player player = ghostGamePlayer.getBukkitPlayer();
            players.put(uuid, new DeadPlayer(plugin, this, uuid, alivePlayer.getPlayerData(), alivePlayer.generatePerishedTasks()));

            perishedTeam.addPlayer(player);
            player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, -1, 0, false, false, false));
            player.setGameMode(GameMode.SURVIVAL);

            plugin.getMessageManager().sendLang(player, GhostLangPath.PLAYER_START_PERISHED);
        } else {
            throw new PlayerNotAliveException("Tried to perish player with UUID " + uuid + ", but they where not alive!");
        }
    }

    protected void rePerishPlayer(final @NotNull DeadPlayer deadPlayer) { // damn necromancers!
        final @Nullable Player player = deadPlayer.getBukkitPlayer();
        players.put(deadPlayer.getUuid(), new DeadPlayer(plugin, this, deadPlayer.getUuid(), new PlayerData(plugin, player), deadPlayer.getGhostTasks()));
        player.teleportAsync(config.getPlayerStartLocation());
        player.setGameMode(GameMode.SURVIVAL);

        perishedTeam.addPlayer(player);
        player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, -1, 0, false, false, false));

        for (SpectatingPlayer spectatingPlayer : spectators.values()) {
            player.hidePlayer(plugin, spectatingPlayer.getBukkitPlayer());
        }

        final QuestModifier deadPlayerQuestModifier = deadPlayer.getQuestModifier();
        for (QuestModifier questModifier : getConfig().getTasks().values()) {
            if (deadPlayerQuestModifier != null && questModifier.getQuestIdentifier().equals(deadPlayerQuestModifier.getQuestIdentifier())) {
                SimpleQuests.getInstance().getDatabaseManager().setTimesQuestFinished(player, questModifier.getRequiredQuestIdentifier(), 1);
            } else {
                SimpleQuests.getInstance().getDatabaseManager().setTimesQuestFinished(player, questModifier.getRequiredQuestIdentifier(), 0);
            }

            SimpleQuests.getInstance().getDatabaseManager().setTimesQuestFinished(player, questModifier.getQuestIdentifier(), 0);
        }

        plugin.getMessageManager().sendLang(player, GhostLangPath.PLAYER_GAME_JOIN,
            Placeholder.component(SharedPlaceHolder.TEXT.getKey(), getConfig().getDisplayName()));
        plugin.getMessageManager().sendLang(player, GhostLangPath.PLAYER_START_PERISHED);
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

            for (SpectatingPlayer spectatingPlayer : spectators.values()) {
                player.showPlayer(plugin, spectatingPlayer.getBukkitPlayer());
            }

            // reset Scoreboard
            perishedTeam.removePlayer(player);
            // player will get potion effects removed and dead players will be visible again, after the player data was restored
            ghostGamePlayer.restorePlayer();

            if (ghostGamePlayer instanceof AlivePlayer alivePlayer) {
                MouseTrap trap = alivePlayer.getMouseTrapTrappedIn();

                if (trap != null) {
                    trap.removePlayer(alivePlayer);
                    alivePlayer.releaseFromTrap();
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

            // reset all quests
            for (QuestModifier questModifier : getConfig().getTasks().values()) {
                SimpleQuests.getInstance().getDatabaseManager().setTimesQuestFinished(player, questModifier.getRequiredQuestIdentifier(), 0);
                SimpleQuests.getInstance().getDatabaseManager().setTimesQuestFinished(player, questModifier.getQuestIdentifier(), 0);
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
                }

                plugin.getMessageManager().sendLang(player, GhostLangPath.PLAYER_GAME_QUIT);
                broadcastExcept(GhostLangPath.PLAYER_GAME_QUIT_BROADCAST, player.getUniqueId(),
                    Placeholder.component(SharedPlaceHolder.PLAYER.getKey(), player.displayName()));
            }
        } else {
            SpectatingPlayer spectatingPlayer = spectators.remove(player.getUniqueId());

            if (spectatingPlayer != null) {
                if (teleport) {
                    player.teleport(config.getEndLocation());
                    player.resetPlayerTime();
                }

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
                            alivePlayer.releaseFromTrap();
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
                            alivePlayer.releaseFromTrap();
                        }
                    }

                    iterator.remove();
                }
            }
        }

        if (players.isEmpty()) {
            endGame(EndReason.GAME_EMPTY);
        }

        for (Iterator<Map.Entry<UUID, SpectatingPlayer>> iterator = spectators.entrySet().iterator(); iterator.hasNext(); ) {
            Map.Entry<UUID, SpectatingPlayer> entry = iterator.next();
            if (!entry.getKey().equals(exception)) {
                Player player = plugin.getServer().getPlayer(entry.getKey());

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

    public @NotNull CompletableFuture<@NotNull Boolean> reload() {
        endGame(EndReason.EXTERN);
        gameState = GameState.RESETTING; // lock from joining while we wait for config

        return config.reload().thenApply(result -> {
            gameState = GameState.IDLE;
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

    protected boolean wouldAllPlayersBeDead(final @NotNull AlivePlayer alivePlayer) {
        for (AGhostGamePlayer ghostGamePlayer : players.values()) {
            if (ghostGamePlayer instanceof AlivePlayer otherAlivePlayer &&
                !alivePlayer.getUuid().equals(otherAlivePlayer.getUuid()) &&
                otherAlivePlayer.getMouseTrapTrappedIn() == null) { // trapped players alone can't win!
                return false;
            }
        }

        return true;
    }

    protected boolean areAllPlayersDead() {
        for (AGhostGamePlayer ghostGamePlayer : players.values()) {
            if (ghostGamePlayer instanceof AlivePlayer alivePlayer &&
                alivePlayer.getMouseTrapTrappedIn() == null) { // trapped players alone can't win!
                return false;
            }
        }

        return true;
    }

    public @Nullable AGhostGamePlayer getGhostGamePlayer(final @NotNull Player player) {
        return this.players.get(player.getUniqueId());
    }

    public @Nullable AGhostGamePlayer getGhostGamePlayer(final @NotNull UUID playerUUID) {
        return this.players.get(playerUUID);
    }

    public @Nullable AGhostGameParticipant getGhostGhostGameParticipant(final @NotNull Player player) {
        AGhostGamePlayer ghostGamePlayer = this.players.get(player.getUniqueId());
        return ghostGamePlayer == null ? spectators.get(player.getUniqueId()) : ghostGamePlayer;
    }

    public void gainPoints(final @NotNull AGhostGamePlayer pointGainingPlayer, final @Range(from = 0, to = Integer.MAX_VALUE) double newGainedPoints) {
        this.gainedPoints += newGainedPoints;

        if (gainedPoints >= getConfig().getPointGoal()) {
            endGame(EndReason.WIN);
        } else {
            float percent = (float) (gainedPoints / getConfig().getPointGoal());
            float stepSize = 1.0f / config.getPointGoal();

            for (Iterator<Map.Entry<UUID, AGhostGamePlayer>> iterator = players.entrySet().iterator(); iterator.hasNext(); ) {
                Map.Entry<UUID, AGhostGamePlayer> entry = iterator.next();

                // sync xp bar for points display
                Player playerInGame = plugin.getServer().getPlayer(entry.getKey());

                if (playerInGame == null) { // sanity check - should never trigger
                    if (entry.getValue() instanceof AlivePlayer alivePlayer) {
                        MouseTrap trap = alivePlayer.getMouseTrapTrappedIn();

                        if (trap != null) {
                            trap.removePlayer(alivePlayer);
                            alivePlayer.releaseFromTrap();
                        }
                    }

                    plugin.getComponentLogger().warn("removed player with uuid {} from the ghost game {}, because they where missing on the server (sync points xp)", entry.getKey(), getName_id());
                    iterator.remove();
                } else {
                    playerInGame.setExp(percent);
                }
            }

            if (players.isEmpty()) {
                endGame(EndReason.ALL_DEAD);

                return;
            }

            for (Iterator<Map.Entry<UUID, SpectatingPlayer>> iterator = spectators.entrySet().iterator(); iterator.hasNext(); ) {
                Map.Entry<UUID, SpectatingPlayer> entry = iterator.next();

                // sync xp bar for points display
                Player playerInGame = plugin.getServer().getPlayer(entry.getKey());

                if (playerInGame == null) {
                    plugin.getComponentLogger().warn("removed spectator with uuid {} from the ghost game {}, because they where missing on the server (sync points xp)", entry.getKey(), getName_id());
                    iterator.remove();
                } else {
                    playerInGame.setExp(percent);
                }
            }

            // should never be null, we just did a sanity check on all players in game
            final @NotNull Player bukkitPlayer = pointGainingPlayer.getBukkitPlayer();

            plugin.getMessageManager().sendLang(bukkitPlayer, GhostLangPath.GAME_POINTS_MSG,
                Placeholder.unparsed(SharedPlaceHolder.NUMBER.getKey(), String.valueOf(newGainedPoints)));

            // annouce for mile stones

            if (percent >= 0.25f && (percent - 0.25f) < stepSize) {
                broadcastAll(GhostLangPath.GAME_POINTS_MILESTONE_25,
                    Placeholder.component(SharedPlaceHolder.PLAYER.getKey(), bukkitPlayer.displayName()),
                    Placeholder.unparsed(SharedPlaceHolder.NUMBER.getKey(), String.valueOf(newGainedPoints)));
            } else if (percent >= 0.5f && (percent - 0.5) < stepSize) {
                broadcastAll(GhostLangPath.GAME_POINTS_MILESTONE_50,
                    Placeholder.component(SharedPlaceHolder.PLAYER.getKey(), bukkitPlayer.displayName()),
                    Placeholder.unparsed(SharedPlaceHolder.NUMBER.getKey(), String.valueOf(newGainedPoints)));
            } else if (percent >= 0.75 && (percent - 0.75f) < stepSize) {
                broadcastAll(GhostLangPath.GAME_POINTS_MILESTONE_75,
                    Placeholder.component(SharedPlaceHolder.PLAYER.getKey(), bukkitPlayer.displayName()),
                    Placeholder.unparsed(SharedPlaceHolder.NUMBER.getKey(), String.valueOf(newGainedPoints)));
            } else if (percent >= 0.9 && (percent - 0.9f) < stepSize) {
                broadcastAll(GhostLangPath.GAME_POINTS_MILESTONE_90,
                    Placeholder.component(SharedPlaceHolder.PLAYER.getKey(), bukkitPlayer.displayName()),
                    Placeholder.unparsed(SharedPlaceHolder.NUMBER.getKey(), String.valueOf(newGainedPoints)));
            }
        }
    }

    public @NotNull List<@NotNull MouseTrap> getMouseTraps() {
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

    public static class PlayerNotAliveException extends IllegalArgumentException {
        @Serial
        private static final long serialVersionUID = 6702629890736910312L;

        /**
         * Constructs an {@code NoAlivePlayerException} with no
         * detail message.
         */
        public PlayerNotAliveException() {
            super();
        }

        /**
         * Constructs an {@code NoAlivePlayerException} with the
         * specified detail message.
         *
         * @param s the detail message.
         */
        public PlayerNotAliveException(String s) {
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
        public PlayerNotAliveException(String message, Throwable cause) {
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
        public PlayerNotAliveException(Throwable cause) {
            super(cause);
        }
    }
}
