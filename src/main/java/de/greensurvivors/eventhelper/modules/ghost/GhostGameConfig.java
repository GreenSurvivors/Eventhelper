package de.greensurvivors.eventhelper.modules.ghost;

import de.greensurvivors.eventhelper.EventHelper;
import de.greensurvivors.eventhelper.config.ConfigOption;
import de.greensurvivors.eventhelper.modules.AModulConfig;
import io.papermc.paper.math.Position;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.apache.commons.collections4.list.SetUniqueList;
import org.apache.maven.artifact.versioning.ComparableVersion;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Range;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@SuppressWarnings("UnstableApiUsage") // Position
public class GhostGameConfig extends AModulConfig<GhostModul> { // todo create and call events
    static {
        ConfigurationSerialization.registerClass(PathModifier.class);
        ConfigurationSerialization.registerClass(MouseTrap.class);
        ConfigurationSerialization.registerClass(QuestModifier.class);
        ConfigurationSerialization.registerClass(UnsafeArea.class);
    }
    // ghost
    private final @NotNull ConfigOption<@NotNull Map<Material, PathModifier>> pathFindableMats = new ConfigOption<>("ghost.pathfind.pathFindables", new HashMap<>(Map.of(Material.YELLOW_GLAZED_TERRACOTTA, new PathModifier())));
    private final @NotNull ConfigOption<@NotNull Double> pathFindOffset = new ConfigOption<>("ghost.pathfind.offset", 30.0D);
    private final @NotNull ConfigOption<@NotNull Integer> followRange = new ConfigOption<>("ghost.follow.range", 30); // in blocks
    private final @NotNull ConfigOption<@NotNull Long> followTimeOut = new ConfigOption<>("ghost.follow.timeout", 1L); // in milliseconds
    private final @NotNull ConfigOption<@NotNull Double> idleVelocity = new ConfigOption<>("ghost.velocity.idle", 0.39D);  // in blocks / t
    private final @NotNull ConfigOption<@NotNull Double> followVelocity = new ConfigOption<>("ghost.velocity.follow", 0.39D); // in blocks / t
    private final @NotNull ConfigOption<@NotNull List<@NotNull Location>> ghostSpawnLocations = new ConfigOption<>("ghost.spawnLocations", List.of()); // we need fast random access. But the locations should be unique! // todo check for very close together locations!
    private final @NotNull ConfigOption<@NotNull List<@NotNull Position>> ghostIdlePositions = new ConfigOption<>("ghost.idlePositions", List.of()); // we need fast random access. But the locations should be unique! // todo check for very close together locations!
    private final @NotNull ConfigOption<@NotNull @Range(from = 1, to = Integer.MAX_VALUE) Integer> ghostAmount = new ConfigOption<>("ghost.amount", 1);
    // vex
    private final @NotNull ConfigOption<@NotNull List<@NotNull Location>> vexSpawnLocations = new ConfigOption<>("vex.spawnLocations", List.of()); // we need fast random access. But the locations should be unique! // todo check for very close together locations!
    private final @NotNull ConfigOption<@NotNull @Range(from = 1, to = Integer.MAX_VALUE) Integer> vexAmount = new ConfigOption<>("vex.amount", 1);
    // general
    private final @NotNull String name_id;
    private final @NotNull ConfigOption<@NotNull Component> displayName;
    private final @NotNull ConfigOption<@NotNull List<@NotNull MouseTrap>> mouseTraps = new ConfigOption<>("game.mouseTraps", List.of()); // we need fast random access. But the MouseTraps should be unique!
    private final @NotNull ConfigOption<@NotNull List<@NotNull String>> gameInitCommands = new ConfigOption<>("game.commands.gameInit", List.of());
    private final @NotNull ConfigOption<@NotNull Location> lobbyLocation = new ConfigOption<>("game.lobbyLocation", Bukkit.getWorlds().get(0).getSpawnLocation());
    private final @NotNull ConfigOption<@NotNull List<@NotNull String>> gameStartCommands = new ConfigOption<>("game.commands.gameStart", List.of());
    private final @NotNull ConfigOption<@NotNull Location> playerStartLocation = new ConfigOption<>("game.playerStartLocation", Bukkit.getWorlds().get(0).getSpawnLocation());
    private final @NotNull ConfigOption<@NotNull Location> spectatorStartLocation = new ConfigOption<>("game.spectatorStartLocation", Bukkit.getWorlds().get(0).getSpawnLocation());
    private final @NotNull ConfigOption<@NotNull List<@NotNull String>> gameEndCommands = new ConfigOption<>("game.commands.gameEnd", List.of());
    private final @NotNull ConfigOption<@NotNull Location> endLocation = new ConfigOption<>("game.endLocation", Bukkit.getWorlds().get(0).getSpawnLocation());
    private final @NotNull ConfigOption<@NotNull Duration> gameDuration = new ConfigOption<>("game.durationSeconds", Duration.of(10, ChronoUnit.MINUTES));
    private final @NotNull ConfigOption<@NotNull @Range(from = 0, to = 24000) Long> startPlayerTime = new ConfigOption<>("game.playerTime.startTicks", 14000L);
    private final @NotNull ConfigOption<@NotNull @Range(from = 0, to = 24000) Long> endPlayerTime = new ConfigOption<>("game.playerTime.endTicks", 23000L);
    private final @NotNull ConfigOption<@NotNull Boolean> allowLateJoin = new ConfigOption<>("game.allowLateJoin", false);
    private final @NotNull ConfigOption<@NotNull Boolean> allowRejoin = new ConfigOption<>("game.allowRejoin", false);
    private final @NotNull ConfigOption<@NotNull @Range(from = -1, to = Integer.MAX_VALUE) Integer> minAmountPlayers = new ConfigOption<>("game.minAmountPlayers", -1);
    private final @NotNull ConfigOption<@NotNull @Range(from = -1, to = Integer.MAX_VALUE) Integer> maxAmountPlayers = new ConfigOption<>("game.maxAmountPlayers", -1);
    private final @NotNull ConfigOption<@NotNull Double> playerSpreadDistance = new ConfigOption<>("game.teleport.playerSpread.distance", 0.5);
    private final @NotNull ConfigOption<@NotNull @Range(from = 1, to = Integer.MAX_VALUE) Integer> pointGoal = new ConfigOption<>("game.points.goal", 100);
    private final @NotNull ConfigOption<@NotNull Map<@NotNull String, @NotNull QuestModifier>> quests = new ConfigOption<>("game.tasks.points", Map.of());
    private final @NotNull ConfigOption<@NotNull Duration> durationInTrapUntilDeath = new ConfigOption<>("game.mouseTrap.secondsUntilDeath", Duration.ofSeconds(90));
    private final @NotNull ConfigOption<@NotNull @Range(from = 0, to = Integer.MAX_VALUE) Integer> perishedTaskAmount = new ConfigOption<>("game.tasks.perished.amount", 3);
    private final @NotNull ConfigOption<@NotNull @Range(from = 0, to = Integer.MAX_VALUE) Integer> startingFoodAmount = new ConfigOption<>("game.food.amount.starting", 8);
    private final @NotNull ConfigOption<@NotNull @Range(from = 0, to = Integer.MAX_VALUE) Integer> startingSaturationAmount = new ConfigOption<>("game.startingSaturationAmount", 0);
    private final @NotNull ConfigOption<@NotNull @Range(from = 0, to = Integer.MAX_VALUE) Double> startingHealthAmount = new ConfigOption<>("game.startingHealthAmount", 5D);
    private final @NotNull ConfigOption<@NotNull String> feedRegion = new ConfigOption<>("game.food.region", "__global__");
    private final @NotNull ConfigOption<@NotNull Duration> feedDelay = new ConfigOption<>("game.food.delayTicks", Duration.ofSeconds(20));
    private final @NotNull ConfigOption<@NotNull @Range(from = 0, to = Integer.MAX_VALUE) Integer> feedAmount = new ConfigOption<>("game.food.feeding.tick", 1);
    private final @NotNull ConfigOption<@NotNull @Range(from = 0, to = Integer.MAX_VALUE) Integer> maxFeedAmount = new ConfigOption<>("game.food.feeding.max", 6);

    public GhostGameConfig(final @NotNull EventHelper plugin, final @NotNull String name_id, final @NotNull GhostModul modul) {
        super(plugin, Path.of("games", name_id + ".yaml"));
        super.setModul(modul);

        this.name_id = name_id;
        this.displayName = new ConfigOption<>("game.displayName", Component.text(name_id));
    }

    @Override
    public @NotNull CompletableFuture<@NotNull Boolean> reload() {
        final CompletableFuture<Boolean> runAfter = new CompletableFuture<>();

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            synchronized (this) {
                if (this.modul != null) {

                    if (!Files.isRegularFile(configPath)) {
                        try (final InputStream inputStream = plugin.getResource(modul.getName() + "/defaultGhostGame.yaml")) {
                            if (inputStream != null) {
                                Files.createDirectories(configPath.getParent());
                                //Files.createFile(configPath);
                                Files.copy(inputStream, configPath);
                            } else {
                                plugin.getComponentLogger().error("Could not find defaultGhostGame.yaml");
                                runAfter.complete(false);
                                return;
                            }
                        } catch (final @NotNull IOException e) {
                            plugin.getComponentLogger().error("Exception was thrown when trying to save default ghost game config!", e);
                        }
                    }

                    try (BufferedReader bufferedReader = Files.newBufferedReader(configPath)) {
                        final @NotNull YamlConfiguration config = YamlConfiguration.loadConfiguration(bufferedReader);

                        @Nullable String dataVersionStr = config.getString(VERSION_PATH);
                        if (dataVersionStr != null) {
                            ComparableVersion lastVersion = new ComparableVersion(dataVersionStr);

                            if (dataVersion.compareTo(lastVersion) < 0) {
                                plugin.getLogger().warning("Found ghost game config for \"" + name_id + "\" was saved in a newer data version " +
                                    "(" + lastVersion + "), expected: " + dataVersion + ". " +
                                    "Trying to load anyway but some this most definitely will be broken!");
                            }
                        } else {
                            plugin.getLogger().warning("The data version for ghost game \"" + name_id + "\" was missing." +
                                "proceed with care!");
                        }

                        isEnabled.setValue(config.getBoolean(isEnabled.getPath()));

                        Object pathFindableMatsObj = config.get(pathFindableMats.getPath());
                        if (pathFindableMatsObj instanceof Map<?, ?> map) {
                            final @NotNull Map<Material, PathModifier> result = new LinkedHashMap<>(map.size());

                            for (Map.Entry<?, ?> entry : map.entrySet()) {
                                if (entry.getKey() instanceof String key && entry.getValue() instanceof PathModifier pathModifier) {
                                    final @Nullable Material material = Material.matchMaterial(key);

                                    if (material != null) {
                                        result.put(material, pathModifier);
                                    }
                                } else {
                                    plugin.getLogger().warning("Could not read data \"" + entry + "\" for " + pathFindableMats.getPath() + " in ghost config!");
                                }
                            }

                            pathFindableMats.setValue(result);
                        } else if (pathFindableMatsObj instanceof ConfigurationSection pathFindableMatsSection) {
                            Map<String, Object> map = pathFindableMatsSection.getValues(false);

                            final @NotNull Map<Material, PathModifier> result = new LinkedHashMap<>(map.size());

                            for (Map.Entry<?, ?> entry : map.entrySet()) {
                                if (entry.getKey() instanceof String key && entry.getValue() instanceof PathModifier pathModifier) {
                                    final @Nullable Material material = Material.matchMaterial(key);

                                    if (material != null) {
                                        result.put(material, pathModifier);
                                    }
                                } else {
                                    plugin.getLogger().warning("Could not read data \"" + entry + "\" for " + pathFindableMats.getPath() + " in ghost config!");
                                }
                            }

                            pathFindableMats.setValue(result);
                        } else {
                            pathFindableMats.setValue(null);
                        }

                        pathFindOffset.setValue(config.getDouble(pathFindOffset.getPath(), pathFindOffset.getFallback()));
                        followRange.setValue(config.getInt(followRange.getPath(), followRange.getFallback()));
                        followTimeOut.setValue(config.getLong(followTimeOut.getPath(), followTimeOut.getFallback()));
                        idleVelocity.setValue(config.getDouble(idleVelocity.getPath(), idleVelocity.getFallback()));
                        followVelocity.setValue(config.getDouble(followVelocity.getPath(), followVelocity.getFallback()));

                        final @NotNull List<?> ghostSpawnObjectList = config.getList(ghostSpawnLocations.getPath(), List.of());
                        final @NotNull ArrayList<@NotNull Location> newGhostSpawnLocations = new ArrayList<>(ghostSpawnObjectList.size());

                        for (Object object : ghostSpawnObjectList) {
                            if (object instanceof Location location) {
                                if (!newGhostSpawnLocations.contains(location)) {
                                    newGhostSpawnLocations.add(location);
                                }
                            } else if (object instanceof Map<?, ?> map) {
                                Location location = Location.deserialize((Map<String, Object>) map);

                                if (!newGhostSpawnLocations.contains(location)) {
                                    newGhostSpawnLocations.add(location);
                                }
                            } else if (object instanceof ConfigurationSection section) {
                                Map<String, Object> map = new HashMap<>();

                                Set<String> keys = section.getKeys(false);

                                for (String key : keys) {
                                    map.put(key, section.get(key));
                                }

                                Location location = Location.deserialize(map);

                                if (!newGhostSpawnLocations.contains(location)) {
                                    newGhostSpawnLocations.add(location);
                                }
                            }
                        }
                        ghostSpawnLocations.setValue(newGhostSpawnLocations);

                        final @NotNull List<?> idleObjectList = config.getList(ghostIdlePositions.getPath(), List.of());
                        final @NotNull ArrayList<@NotNull Position> newIdlePositions = new ArrayList<>(idleObjectList.size());
                        for (Object object : idleObjectList) {
                            if (object instanceof Map<?, ?> map) {
                                Position position = deserializePosition((Map<String, Object>) map);

                                newIdlePositions.add(position);
                            } else if (object instanceof ConfigurationSection section) {
                                Map<String, Object> map = new HashMap<>();

                                Set<String> keys = section.getKeys(false);

                                for (String key : keys) {
                                    map.put(key, section.get(key));
                                }

                                Position position = deserializePosition(map);
                                newIdlePositions.add(position);
                            } else {
                                plugin.getComponentLogger().warn("unknown idle position config: {}", object);
                            }
                        }
                        ghostIdlePositions.setValue(newIdlePositions);

                        ghostAmount.setValue(config.getInt(ghostAmount.getPath(), ghostAmount.getFallback()));

                        final @NotNull List<?> vexSpawnObjectList = config.getList(vexSpawnLocations.getPath(), List.of());
                        final @NotNull ArrayList<@NotNull Location> newVexSpawnLocations = new ArrayList<>(vexSpawnObjectList.size());

                        for (Object object : vexSpawnObjectList) {
                            if (object instanceof Location location) {
                                if (!newVexSpawnLocations.contains(location)) {
                                    newVexSpawnLocations.add(location);
                                }
                            } else if (object instanceof Map<?, ?> map) {
                                Location location = Location.deserialize((Map<String, Object>) map);

                                if (!newVexSpawnLocations.contains(location)) {
                                    newVexSpawnLocations.add(location);
                                }
                            } else if (object instanceof ConfigurationSection section) {
                                Map<String, Object> map = new HashMap<>();

                                Set<String> keys = section.getKeys(false);

                                for (String key : keys) {
                                    map.put(key, section.get(key));
                                }

                                Location location = Location.deserialize(map);

                                if (!newVexSpawnLocations.contains(location)) {
                                    newVexSpawnLocations.add(location);
                                }
                            }
                        }
                        vexSpawnLocations.setValue(newVexSpawnLocations);

                        vexAmount.setValue(config.getInt(vexAmount.getPath(), vexAmount.getFallback()));

                        final @Nullable String rawDisplayName = config.getString(displayName.getPath());
                        if (rawDisplayName == null) {
                            displayName.setValue(displayName.getFallback());
                        } else {
                            displayName.setValue(MiniMessage.miniMessage().deserialize(rawDisplayName));
                        }

                        @Nullable List<@NotNull MouseTrap> loadedMouseTraps = (List<MouseTrap>) config.getList(mouseTraps.getPath());
                        mouseTraps.setValue(loadedMouseTraps == null ? mouseTraps.getFallback() : SetUniqueList.setUniqueList(new ArrayList<>(loadedMouseTraps)));

                        gameInitCommands.setValue(config.getStringList(gameInitCommands.getPath()));
                        lobbyLocation.setValue(config.getLocation(lobbyLocation.getPath(), lobbyLocation.getFallback()));
                        gameStartCommands.setValue(config.getStringList(gameStartCommands.getPath()));
                        playerStartLocation.setValue(config.getLocation(playerStartLocation.getPath(), playerStartLocation.getFallback()));
                        spectatorStartLocation.setValue(config.getLocation(spectatorStartLocation.getPath(), spectatorStartLocation.getFallback()));
                        gameEndCommands.setValue(config.getStringList(gameEndCommands.getPath()));
                        endLocation.setValue(config.getLocation(endLocation.getPath(), endLocation.getFallback()));

                        gameDuration.setValue(Duration.ofSeconds(config.getLong(gameDuration.getPath(), gameDuration.getFallback().toSeconds()))); // todo check if bigger than 0
                        startPlayerTime.setValue(config.getLong(startPlayerTime.getPath(), startPlayerTime.getFallback()));
                        endPlayerTime.setValue(config.getLong(endPlayerTime.getPath(), endPlayerTime.getFallback()));
                        allowLateJoin.setValue(config.getBoolean(allowLateJoin.getPath(), allowLateJoin.getFallback()));
                        allowRejoin.setValue(config.getBoolean(allowRejoin.getPath(), allowRejoin.getFallback()));
                        minAmountPlayers.setValue(config.getInt(minAmountPlayers.getPath(), minAmountPlayers.getFallback()));
                        maxAmountPlayers.setValue(config.getInt(maxAmountPlayers.getPath(), maxAmountPlayers.getFallback()));
                        playerSpreadDistance.setValue(config.getDouble(playerSpreadDistance.getPath(), playerSpreadDistance.getFallback()));

                        Map<String, QuestModifier> questModifierMap = new HashMap<>();
                        for (Object rawQuestModifier : config.getList(quests.getPath(), Collections.emptyList())) {
                            if (rawQuestModifier instanceof QuestModifier questModifier) {
                                questModifierMap.put(questModifier.getQuestIdentifier(), questModifier);
                            } else {
                                plugin.getComponentLogger().warn("Object in config of module {}, found with key {}  {} is not a valid QuestModifier", modul.getName(), quests.getPath(), rawQuestModifier);
                            }
                        }
                        quests.setValue(questModifierMap);

                        pointGoal.setValue(config.getInt(pointGoal.getPath(), pointGoal.getFallback()));
                        durationInTrapUntilDeath.setValue(Duration.ofSeconds(config.getLong(durationInTrapUntilDeath.getPath(), durationInTrapUntilDeath.getFallback().toSeconds())));
                        perishedTaskAmount.setValue(config.getInt(perishedTaskAmount.getPath(), perishedTaskAmount.getFallback()));

                        startingFoodAmount.setValue(config.getInt(startingFoodAmount.getPath(), startingFoodAmount.getFallback()));
                        startingSaturationAmount.setValue(config.getInt(startingSaturationAmount.getPath(), startingSaturationAmount.getFallback()));
                        startingHealthAmount.setValue(config.getDouble(startingHealthAmount.getPath(), startingHealthAmount.getFallback()));
                        feedRegion.setValue(config.getString(feedRegion.getPath(), feedRegion.getFallback()));
                        long feedDelayTicks = config.getLong(feedDelay.getPath(), feedDelay.getFallback().toSeconds() * 20L);
                        feedDelay.setValue(Duration.ofSeconds(feedDelayTicks / 20));
                        feedAmount.setValue(config.getInt(feedAmount.getPath(), feedAmount.getFallback()));
                        maxFeedAmount.setValue(config.getInt(maxFeedAmount.getPath(), maxFeedAmount.getFallback()));

                        Bukkit.getScheduler().runTask(plugin, () -> runAfter.complete(isEnabled.getValueOrFallback())); // back to main thread
                    } catch (IOException e) {
                        plugin.getComponentLogger().error("Could not load modul config for {} from file!", modul.getName(), e);

                        isEnabled.setValue(Boolean.FALSE);
                        Bukkit.getScheduler().runTask(plugin, () -> runAfter.complete(Boolean.FALSE)); // back to main thread
                    }
                } else {
                    plugin.getComponentLogger().error("Could not load modul config, since the module of {} was not set!", this.getClass().getName());

                    isEnabled.setValue(Boolean.FALSE);
                    Bukkit.getScheduler().runTask(plugin, () -> runAfter.complete(Boolean.FALSE)); // back to main thread
                }
            }
        });

        return runAfter;
    }

    @Override
    public @NotNull CompletableFuture<@NotNull Boolean> save() { // todo commands
        final CompletableFuture<@NotNull Boolean> runAfter = new CompletableFuture<>();

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            synchronized (this) {
                if (this.modul != null) {
                    if (!Files.isRegularFile(configPath)) {
                        try (final InputStream inputStream = plugin.getResource(modul.getName() + "/defaultGhostGame.yaml")) {
                            if (inputStream != null) {
                                Files.createDirectories(configPath.getParent());
                                //Files.createFile(configPath);
                                Files.copy(inputStream, configPath);
                            } else {
                                plugin.getComponentLogger().error("Could not find defaultGhostGame.yaml");
                                runAfter.complete(Boolean.FALSE);
                                return;
                            }
                        } catch (final @NotNull IOException e) {
                            plugin.getComponentLogger().error("Exception was thrown when trying to save default ghost game config!", e);
                        }
                    }

                    try (BufferedReader bufferedReader = Files.newBufferedReader(configPath)) {
                        @NotNull YamlConfiguration config = YamlConfiguration.loadConfiguration(bufferedReader);

                        config.set(VERSION_PATH, dataVersion.toString());

                        config.set(pathFindableMats.getPath(),
                            pathFindableMats.getValueOrFallback().entrySet().stream().collect(Collectors.toMap(
                                e -> e.getKey().getKey().asString(), // map materials to namespaced strings
                                Map.Entry::getValue)));
                        config.set(pathFindOffset.getPath(), pathFindOffset.getValueOrFallback());
                        config.set(followRange.getPath(), followRange.getValueOrFallback());
                        config.set(followTimeOut.getPath(), followTimeOut.getValueOrFallback());
                        config.set(idleVelocity.getPath(), idleVelocity.getValueOrFallback());
                        config.set(followVelocity.getPath(), followVelocity.getValueOrFallback());
                        config.set(ghostSpawnLocations.getPath(), ghostSpawnLocations.getValueOrFallback());
                        config.set(ghostIdlePositions.getPath(), ghostIdlePositions.getValueOrFallback().stream().map(AModulConfig::serializePosition).toList());
                        config.set(ghostAmount.getPath(), ghostAmount.getValueOrFallback());

                        config.set(vexSpawnLocations.getPath(), vexSpawnLocations.getValueOrFallback());
                        config.set(vexAmount.getPath(), vexAmount.getValueOrFallback());

                        config.set(gameInitCommands.getPath(), gameInitCommands.getValueOrFallback());
                        config.set(gameStartCommands.getPath(), gameStartCommands.getValueOrFallback());
                        config.set(gameEndCommands.getPath(), gameEndCommands.getValueOrFallback());
                        config.set(displayName.getPath(), MiniMessage.miniMessage().serialize(displayName.getValueOrFallback()));
                        config.set(mouseTraps.getPath(), List.copyOf(mouseTraps.getValueOrFallback()));
                        config.set(lobbyLocation.getPath(), lobbyLocation.getValueOrFallback());
                        config.set(playerStartLocation.getPath(), playerStartLocation.getValueOrFallback());
                        config.set(spectatorStartLocation.getPath(), spectatorStartLocation.getValueOrFallback());
                        config.set(endLocation.getPath(), endLocation.getValueOrFallback());
                        config.set(gameDuration.getPath(), gameDuration.getValueOrFallback().toSeconds());
                        config.set(startPlayerTime.getPath(), startPlayerTime.getValueOrFallback());
                        config.set(endPlayerTime.getPath(), endPlayerTime.getValueOrFallback());
                        config.set(allowLateJoin.getPath(), allowLateJoin.getValueOrFallback());
                        config.set(allowRejoin.getPath(), allowRejoin.getValueOrFallback());
                        config.set(minAmountPlayers.getPath(), minAmountPlayers.getValueOrFallback());
                        config.set(maxAmountPlayers.getPath(), maxAmountPlayers.getValueOrFallback());
                        config.set(playerSpreadDistance.getPath(), playerSpreadDistance.getValueOrFallback());
                        config.set(quests.getPath(), List.copyOf(quests.getValueOrFallback().values()));
                        config.set(pointGoal.getPath(), pointGoal.getValueOrFallback());
                        config.set(durationInTrapUntilDeath.getPath(), durationInTrapUntilDeath.getValueOrFallback().toSeconds());
                        config.set(perishedTaskAmount.getPath(), perishedTaskAmount.getValueOrFallback());

                        config.set(startingFoodAmount.getPath(), startingFoodAmount.getValueOrFallback());
                        config.set(startingSaturationAmount.getPath(), startingSaturationAmount.getValueOrFallback());
                        config.set(startingHealthAmount.getPath(), startingHealthAmount.getValueOrFallback());
                        config.set(feedRegion.getPath(), feedRegion.getValueOrFallback());
                        config.set(feedDelay.getPath(), feedDelay.getValueOrFallback().toSeconds() * 20);
                        config.set(feedAmount.getPath(), feedAmount.getValueOrFallback());
                        config.set(maxFeedAmount.getPath(), maxFeedAmount.getValueOrFallback());

                        config.options().parseComments(true);
                        config.save(configPath.toFile());

                        Bukkit.getScheduler().runTask(plugin, () -> runAfter.complete(Boolean.TRUE)); // back to main thread
                    } catch (IOException e) {
                        plugin.getComponentLogger().error("Could not load modul config for {} from file!", modul.getName(), e);

                        isEnabled.setValue(Boolean.FALSE);
                        Bukkit.getScheduler().runTask(plugin, () -> runAfter.complete(Boolean.FALSE)); // back to main thread
                    }
                } else {
                    plugin.getComponentLogger().error("Could not save modul config, since the module of {} was not set!", this.getClass().getName());

                    isEnabled.setValue(Boolean.FALSE);
                    Bukkit.getScheduler().runTask(plugin, () -> runAfter.complete(Boolean.FALSE)); // back to main thread
                }
            }
        });

        return runAfter;
    }

    public double getPathfindOffset() {
        return pathFindOffset.getValueOrFallback();
    }

    public void setPathfindOffset(double newOffset) {
        pathFindOffset.setValue(newOffset);

        save().thenAccept(result -> {
            if (result) {
                reload();
            }
        });
    }

    public int getFollowRangeAt(final @NotNull Material material) { // todo setter
        PathModifier modifier = pathFindableMats.getValueOrFallback().get(material);

        if (modifier != null) {
            Integer overwrite = modifier.getOverwriteFollowRange();

            if (overwrite != null) {
                return overwrite;
            }
        }

        return followRange.getValueOrFallback();
    }

    public double getIdleVelocityAt(final @NotNull Material material) { // todo setter
        PathModifier modifier = pathFindableMats.getValueOrFallback().get(material);

        if (modifier != null) {
            Double overwrite = modifier.getOverwriteIdleVelocity();

            if (overwrite != null) {
                return overwrite;
            }
        }

        return idleVelocity.getValueOrFallback();
    }

    public double getFollowVelocityAt(final @NotNull Material material) { // todo setter
        PathModifier modifier = pathFindableMats.getValueOrFallback().get(material);

        if (modifier != null) {
            Double overwrite = modifier.getOverwriteFollowVelocity();

            if (overwrite != null) {
                return overwrite;
            }
        }

        return followVelocity.getValueOrFallback();
    }

    public @NotNull List<@NotNull Location> getGhostSpawnLocations() {
        return ghostSpawnLocations.getValueOrFallback();
    }

    public void addGhostSpawnLocation(final @NotNull Location newLocation) {
        if (ghostSpawnLocations.hasValue()) {

            if (!ghostSpawnLocations.getValueOrFallback().contains(newLocation)) {
                ghostSpawnLocations.getValueOrFallback().add(newLocation);
            }
        } else {
            List<Location> locations = new ArrayList<>();
            locations.add(newLocation);

            ghostSpawnLocations.setValue(locations);
        }

        save().thenAccept(result -> {
            if (result) {
                reload();
            }
        });
    }

    public void removeGhostSpawnLocation(final @NotNull Location location) {
        if (ghostSpawnLocations.hasValue()) {

            if (ghostSpawnLocations.getValueOrFallback().remove(location)) {
                save().thenAccept(result -> {
                    if (result) {
                        reload();
                    }
                });
            }
        }
    }

    public void removeAllGhostSpawnLocations() {
        if (ghostSpawnLocations.hasValue()) {
            ghostSpawnLocations.getValueOrFallback().clear();

            save().thenAccept(result -> {
                if (result) {
                    reload();
                }
            });
        }
    }

    public @NotNull List<@NotNull Location> getVexSpawnLocations() {
        return vexSpawnLocations.getValueOrFallback();
    }

    public void addVexSpawnLocation(final @NotNull Location newLocation) {
        if (vexSpawnLocations.hasValue()) {

            if (!vexSpawnLocations.getValueOrFallback().contains(newLocation)) {
                vexSpawnLocations.getValueOrFallback().add(newLocation);
            }
        } else {
            List<Location> locations = new ArrayList<>();
            locations.add(newLocation);

            vexSpawnLocations.setValue(locations);
        }

        save().thenAccept(result -> {
            if (result) {
                reload();
            }
        });
    }

    public void removeVexSpawnLocation(final @NotNull Location location) {
        if (vexSpawnLocations.hasValue()) {

            if (vexSpawnLocations.getValueOrFallback().remove(location)) {
                save().thenAccept(result -> {
                    if (result) {
                        reload();
                    }
                });
            }
        }
    }

    public void removeAllVexSpawnLocations() {
        if (vexSpawnLocations.hasValue()) {
            vexSpawnLocations.getValueOrFallback().clear();

            save().thenAccept(result -> {
                if (result) {
                    reload();
                }
            });
        }
    }

    public @NotNull List<@NotNull Position> getIdlePositions() { // todo make configuable
        return ghostIdlePositions.getValueOrFallback();
    }

    public void addGhostIdlePosition(final @NotNull Position newPosition) {
        if (ghostIdlePositions.hasValue()) {

            if (!ghostIdlePositions.getValueOrFallback().contains(newPosition)) {
                ghostIdlePositions.getValueOrFallback().add(newPosition);
            }
        } else {
            List<Position> positions = new ArrayList<>();
            positions.add(newPosition);

            ghostIdlePositions.setValue(positions);
        }

        save().thenAccept(result -> {
            if (result) {
                reload();
            }
        });
    }

    public void removeGhostIdlePosition(final @NotNull Position position) {
        if (ghostIdlePositions.hasValue()) {

            if (ghostIdlePositions.getValueOrFallback().remove(position)) {
                save().thenAccept(result -> {
                    if (result) {
                        reload();
                    }
                });
            }
        }
    }

    public void removeAllGhostIdlePositions() {
        if (ghostIdlePositions.hasValue()) {
            ghostIdlePositions.getValueOrFallback().clear();

            save().thenAccept(result -> {
                if (result) {
                    reload();
                }
            });
        }
    }

    public int getAmountOfGhosts() {
        return ghostAmount.getValueOrFallback();
    }

    public void setAmountOfGhosts(@Range(from = 1, to = Integer.MAX_VALUE) int newAmount) {
        ghostAmount.setValue(newAmount);

        save().thenAccept(result -> {
            if (result) {
                reload();
            }
        });
    }

    public int getAmountOfVexes() {
        return vexAmount.getValueOrFallback();
    }

    public void setAmountOfVexes(@Range(from = 1, to = Integer.MAX_VALUE) int newAmount) {
        vexAmount.setValue(newAmount);

        save().thenAccept(result -> {
            if (result) {
                reload();
            }
        });
    }

    public @NotNull Component getDisplayName() {
        return displayName.getValueOrFallback();
    }

    public @NotNull List<@NotNull MouseTrap> getMouseTraps() { // todo make editable via command
        return mouseTraps.getValueOrFallback();
    }

    public @NotNull List<String> getGameInitCommands() { // todo use and makle editable
        return gameInitCommands.getValueOrFallback();
    }

    public @NotNull Location getLobbyLocation() {
        return lobbyLocation.getValueOrFallback();
    }

    public void setLobbyLocation(final @NotNull Location newLobbyLocation) {
        lobbyLocation.setValue(newLobbyLocation);

        save().thenAccept(result -> {
            if (result) {
                reload();
            }
        });
    }

    public @NotNull List<String> getGameStartCommands() { // todo use and make editable
        return gameStartCommands.getValueOrFallback();
    }

    public @NotNull Location getPlayerStartLocation() {
        return playerStartLocation.getValueOrFallback();
    }

    public void setPlayerStartLocation(final @NotNull Location newStartingLocation) {
        playerStartLocation.setValue(newStartingLocation);

        save().thenAccept(result -> {
            if (result) {
                reload();
            }
        });
    }

    public @NotNull Location getSpectatorStartLocation() { // todo
        return spectatorStartLocation.getValueOrFallback();
    }

    public void setSpectatorStartLocation(final @NotNull Location newStartingLocation) { // todo
        spectatorStartLocation.setValue(newStartingLocation);
    }

    public @NotNull List<String> getGameEndCommands() { // todo use and make editable
        return gameEndCommands.getValueOrFallback();
    }

    public @NotNull Location getEndLocation() {
        return endLocation.getValueOrFallback();
    }

    public void setEndLocation(final @NotNull Location newEndLocation) {
        endLocation.setValue(newEndLocation);

        save().thenAccept(result -> {
            if (result) {
                reload();
            }
        });
    }

    public @NotNull Duration getGameDuration() {
        return gameDuration.getValueOrFallback();
    }

    public void setGameDuration(final @NotNull Duration newGameDuration) { // todo maybe make all the setters nullable to reset to default?
        gameDuration.setValue(newGameDuration);

        save().thenAccept(result -> {
            if (result) {
                reload();
            }
        });
    }

    public long getStartPlayerTime() {
        return startPlayerTime.getValueOrFallback();
    }

    public void setStartPlayerTime(@Range(from = 0, to = 24000) long newStartPlayerTime) {
        startPlayerTime.setValue(newStartPlayerTime);

        save().thenAccept(result -> {
            if (result) {
                reload();
            }
        });
    }

    public long getEndPlayerTime() {
        return endPlayerTime.getValueOrFallback();
    }

    public void setEndPlayerTime(@Range(from = 0, to = 24000) long newEndPlayerTime) {
        endPlayerTime.setValue(newEndPlayerTime);

        save().thenAccept(result -> {
            if (result) {
                reload();
            }
        });
    }

    public boolean isLateJoinAllowed() {
        return allowLateJoin.getValueOrFallback();
    }

    public void setIsLateJoinAllowed(boolean isAllowed) {
        allowLateJoin.setValue(isAllowed);

        save().thenAccept(result -> {
            if (result) {
                reload();
            }
        });
    }

    public boolean isRejoinAllowed() {
        return allowRejoin.getValueOrFallback();
    }

    public void setIsRejoinAllowed(boolean isAllowed) { // todo
        allowRejoin.setValue(isAllowed);

        save().thenAccept(result -> {
            if (result) {
                reload();
            }
        });
    }

    public double getMinAmountPlayers() { // todo use
        return minAmountPlayers.getValueOrFallback();
    }

    public void setMinAmountPlayers(@Range(from = -1, to = Integer.MAX_VALUE) int newMaxAmount) {
        minAmountPlayers.setValue(newMaxAmount);

        save().thenAccept(result -> {
            if (result) {
                reload();
            }
        });
    }

    public @Range(from = -1, to = Integer.MAX_VALUE) int getMaxAmountPlayers() {
        return maxAmountPlayers.getValueOrFallback();
    }

    public void setMaxAmountPlayers(@Range(from = -1, to = Integer.MAX_VALUE) int newMaxAmount) {
        maxAmountPlayers.setValue(newMaxAmount);

        save().thenAccept(result -> {
            if (result) {
                reload();
            }
        });
    }

    public double getMaxPlayerSpreadTeleport() { // todo use
        return playerSpreadDistance.getValueOrFallback();
    }

    public void setPlayerSpreadDistanceTeleport(double newDistance) {
        playerSpreadDistance.setValue(newDistance);

        save().thenAccept(result -> {
            if (result) {
                reload();
            }
        });
    }

    public @Nullable Double getPointsOfTask(final @NotNull String questIdentifier) { // todo setter
        final @Nullable QuestModifier questModifier = quests.getValueOrFallback().get(questIdentifier);
        if (questModifier != null) {
            return questModifier.getPointsRewarded();
        } else {
            return null;
        }
    }

    public int getPointGoal() {
        return pointGoal.getValueOrFallback();
    }

    public void setPointGoal(int newGoal) { // todo
        pointGoal.setValue(newGoal);
    }

    public Duration getDurationTrappedUntilDeath() {
        return durationInTrapUntilDeath.getValueOrFallback();
    }

    public void setDurationTrappedUntilDeath(final @NotNull Duration newDuration) { // todo
        durationInTrapUntilDeath.setValue(newDuration);
    }

    public @Range(from = 0, to = Integer.MAX_VALUE) int getPerishedTaskAmount() {
        return perishedTaskAmount.getValueOrFallback();
    }

    public void setPerishedTaskAmount(final @Range(from = 0, to = Integer.MAX_VALUE) int newAmountOfTasks) {
        perishedTaskAmount.setValue(newAmountOfTasks);
    }

    public @Range(from = 0, to = Integer.MAX_VALUE) int getStartingFoodAmount() {
        return startingFoodAmount.getValueOrFallback();
    }

    public void setStartingFoodAmount(final @Range(from = 0, to = Integer.MAX_VALUE) int newStartingFoodAmount) { // todo
        startingFoodAmount.setValue(newStartingFoodAmount);
    }

    public @Range(from = 0, to = Integer.MAX_VALUE) int getStartingSaturationAmount() {
        return startingSaturationAmount.getValueOrFallback();
    }

    public void setStartingSaturationAmount(final @Range(from = 0, to = Integer.MAX_VALUE) int newStartingSaturationAmount) { // todo
        startingSaturationAmount.setValue(newStartingSaturationAmount);
    }

    public @Range(from = 0, to = Integer.MAX_VALUE) double getStartingHealthAmount() {
        return startingHealthAmount.getValueOrFallback();
    }

    public void setStartingHealthAmount(final @Range(from = 0, to = Integer.MAX_VALUE) double newStartingHealthAmount) { // todo
        startingHealthAmount.setValue(newStartingHealthAmount);
    }

    public @NotNull String getFeedRegionName() {
        return feedRegion.getValueOrFallback();
    }

    public void setFeedRegionName(final @NotNull String newFeedRegionName) { // todo
        feedRegion.setValue(newFeedRegionName);
    }

    public @NotNull Duration getFeedDuration() {
        return feedDelay.getValueOrFallback();
    }

    public void setFeedDuration(final @NotNull Duration newFeedDelay) { // todo
        feedDelay.setValue(newFeedDelay);
    }

    public @Range(from = 0, to = Integer.MAX_VALUE) int getFeedAmount() {
        return feedAmount.getValueOrFallback();
    }

    public void setFeedAmount(final @Range(from = 0, to = Integer.MAX_VALUE) int newFeedAmount) { // todo
        feedAmount.setValue(newFeedAmount);
    }

    public @Range(from = 0, to = Integer.MAX_VALUE) int getFeedMaxAmount() {
        return maxFeedAmount.getValueOrFallback();
    }

    public void setFeedMaxAmount(final @Range(from = 0, to = Integer.MAX_VALUE) int newMaxFeedAmount) { // todo
        maxFeedAmount.setValue(newMaxFeedAmount);
    }

    public @NotNull Map<@NotNull String, @NotNull QuestModifier> getTasks() {
        return quests.getValueOrFallback();
    }
}
