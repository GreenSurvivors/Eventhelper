package de.greensurvivors.eventhelper.modules.ghost;

import de.greensurvivors.eventhelper.EventHelper;
import de.greensurvivors.eventhelper.config.ConfigOption;
import de.greensurvivors.eventhelper.modules.AModulConfig;
import org.apache.maven.artifact.versioning.ComparableVersion;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Range;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

// wanders around looking for player if no target. can target through walls but must path find there
public class GhostGameConfig extends AModulConfig<GhostModul> {
    private final @NotNull ConfigOption<@NotNull Map<Material, PathModifier>> pathFindableMats = new ConfigOption<>("ghost.pathfind.validMaterials", new HashMap<>(Map.of(Material.YELLOW_GLAZED_TERRACOTTA, new PathModifier())));
    private final @NotNull ConfigOption<@NotNull Integer> pathFindOffset = new ConfigOption<>("ghost.pathfind.offset", -30);
    private final @NotNull ConfigOption<@NotNull Integer> followRange = new ConfigOption<>("ghost.follow.range", 30); // in blocks
    private final @NotNull ConfigOption<@NotNull Long> followTimeOut = new ConfigOption<>("ghost.follow.timeout", 1L); // in milliseconds
    private final @NotNull ConfigOption<@NotNull Double> idleVelocity = new ConfigOption<>("ghost.velocity.idle", 1.0D);  // in blocks / s ?
    private final @NotNull ConfigOption<@NotNull Double> followVelocity = new ConfigOption<>("ghost.velocity.follow", 1.5D); // in blocks / s ?
    private final @NotNull ConfigOption<@NotNull List<@NotNull Location>> ghostSpawnLocations = new ConfigOption<>("ghost.spawnLocations", List.of()); // we need fast random access. But the locations should be unique!
    private final @NotNull ConfigOption<@NotNull @Range(from = 1, to = Integer.MAX_VALUE) Integer> amount = new ConfigOption<>("ghost.amount", 1);
    private final @NotNull ConfigOption<@NotNull List<@NotNull MouseTrap>> mouseTraps = new ConfigOption<>("ghost.mouseTraps", List.of()); // we need fast random access. But the MouseTraps should be unique!

    private final @NotNull String gameName;

    public GhostGameConfig(final @NotNull EventHelper plugin, final @NotNull String gameName) {
        super(plugin, Path.of("ghost", gameName + ".yaml"));

        this.gameName = gameName;
    }

    @Override
    public @NotNull CompletableFuture<@NotNull Boolean> reload() {
        final CompletableFuture<Boolean> runAfter = new CompletableFuture<>();

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            synchronized (this) {
                if (this.modul != null) {
                    plugin.saveResource(modul.getName() + "/defaultGhostGame.yaml", false);

                    try (BufferedReader bufferedReader = Files.newBufferedReader(configPath)) {
                        final @NotNull YamlConfiguration config = YamlConfiguration.loadConfiguration(bufferedReader);

                        @Nullable String dataVersionStr = config.getString(VERSION_PATH);
                        if (dataVersionStr != null) {
                            ComparableVersion lastVersion = new ComparableVersion(dataVersionStr);

                            if (dataVersion.compareTo(lastVersion) < 0) {
                                plugin.getLogger().warning("Found ghost game config for \"" + gameName + "\" was saved in a newer data version " +
                                    "(" + lastVersion + "), expected: " + dataVersion + ". " +
                                    "Trying to load anyway but some this most definitely will be broken!");
                            }
                        } else {
                            plugin.getLogger().warning("The data version for ghost game \"" + gameName + "\" was missing." +
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
                        } else {
                            pathFindableMats.setValue(null);
                        }

                        pathFindOffset.setValue(config.getInt(pathFindOffset.getPath(), pathFindOffset.getFallback()));
                        followRange.setValue(config.getInt(followRange.getPath(), followRange.getFallback()));
                        followTimeOut.setValue(config.getLong(followTimeOut.getPath(), followTimeOut.getFallback()));
                        idleVelocity.setValue(config.getDouble(idleVelocity.getPath(), idleVelocity.getFallback()));
                        followVelocity.setValue(config.getDouble(followVelocity.getPath(), followVelocity.getFallback()));
                        mouseTraps.setValue((List<MouseTrap>) config.getList(mouseTraps.getPath()));

                        final @NotNull ArrayList<@NotNull Location> newSpawnLocations = new ArrayList<>();
                        final @NotNull List<?> objectList = config.getList(ghostSpawnLocations.getPath(), List.of());

                        for (Object object : objectList) {
                            if (object instanceof Location location) {
                                if (!newSpawnLocations.contains(location)) {
                                    newSpawnLocations.add(location);
                                }
                            } else if (object instanceof Map<?,?> map) {
                                Location location = Location.deserialize((Map<String, Object>)map);

                                if (!newSpawnLocations.contains(location)) {
                                    newSpawnLocations.add(location);
                                }
                            } else if (object instanceof ConfigurationSection section) {
                                Map<String, Object> map = new HashMap<>();

                                Set<String> keys = section.getKeys(false);

                                for (String key : keys) {
                                    map.put(key, section.get(key));
                                }

                                Location location = Location.deserialize(map);

                                if (!newSpawnLocations.contains(location)) {
                                    newSpawnLocations.add(location);
                                }
                            }
                        }
                        ghostSpawnLocations.setValue(newSpawnLocations);

                        amount.setValue(config.getInt(amount.getPath(), amount.getFallback()));

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
    public @NotNull CompletableFuture<Void> save() {
        final CompletableFuture<Void> runAfter = new CompletableFuture<>();

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            synchronized (this) {
                if (this.modul != null) {
                    plugin.saveResource(modul.getName() + "/" + configPath.getFileName().toString(), false);

                    try (BufferedReader bufferedReader = Files.newBufferedReader(configPath)) {
                        @NotNull YamlConfiguration config = YamlConfiguration.loadConfiguration(bufferedReader);

                        config.set(VERSION_PATH, dataVersion.toString());

                        config.set(pathFindableMats.getPath(),
                            pathFindableMats.getValueOrFallback().entrySet().stream().collect(Collectors.toMap(
                                e -> e.getKey().getKey().asString(), // map materials to namespaced strings
                                Map.Entry::getValue)));
                        config.set(followRange.getPath(), followRange.getValueOrFallback());
                        config.set(followTimeOut.getPath(), followTimeOut.getValueOrFallback());
                        config.set(idleVelocity.getPath(), idleVelocity.getValueOrFallback());
                        config.set(followVelocity.getPath(), followVelocity.getValueOrFallback());
                        config.set(ghostSpawnLocations.getPath(), ghostSpawnLocations.getValueOrFallback());
                        config.set(amount.getPath(), amount.getValueOrFallback());
                        config.set(mouseTraps.getPath(), mouseTraps.getValueOrFallback());

                        config.options().parseComments(true);
                        config.save(configPath.toFile());

                        Bukkit.getScheduler().runTask(plugin, () -> runAfter.complete(null)); // back to main thread
                    } catch (IOException e) {
                        plugin.getComponentLogger().error("Could not load modul config for {} from file!", modul.getName(), e);

                        isEnabled.setValue(Boolean.FALSE);
                        Bukkit.getScheduler().runTask(plugin, () -> runAfter.complete(null)); // back to main thread
                    }
                } else {
                    plugin.getComponentLogger().error("Could not save modul config, since the module of {} was not set!", this.getClass().getName());

                    isEnabled.setValue(Boolean.FALSE);
                    Bukkit.getScheduler().runTask(plugin, () -> runAfter.complete(null)); // back to main thread
                }
            }
        });

        return runAfter;
    }
}
