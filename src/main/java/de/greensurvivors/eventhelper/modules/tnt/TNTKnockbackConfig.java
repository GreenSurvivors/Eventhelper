package de.greensurvivors.eventhelper.modules.tnt;

import de.greensurvivors.eventhelper.EventHelper;
import de.greensurvivors.eventhelper.modules.AModulConfig;
import org.apache.maven.artifact.versioning.ComparableVersion;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.util.concurrent.CompletableFuture;


public class TNTKnockbackConfig extends AModulConfig<TNTKnockbackModul> {

    public TNTKnockbackConfig(@NotNull EventHelper plugin) {
        super(plugin);
    }

    @Override
    public @NotNull CompletableFuture<@NotNull Boolean> reload() {
        final CompletableFuture<Boolean> runAfter = new CompletableFuture<>();

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            synchronized (this) {
                if (this.modul != null) {
                    plugin.saveResource(modul.getName() + "/" + configPath.getFileName().toString(), false);

                    try (BufferedReader bufferedReader = Files.newBufferedReader(configPath)) {
                        @NotNull YamlConfiguration config = YamlConfiguration.loadConfiguration(bufferedReader);

                        @Nullable String dataVersionStr = config.getString(VERSION_PATH);
                        if (dataVersionStr != null) {
                            ComparableVersion lastVersion = new ComparableVersion(dataVersionStr);

                            if (dataVersion.compareTo(lastVersion) < 0) {
                                plugin.getComponentLogger().warn("Found modul config for \"{}\" was saved in a newer data version ({}), " +
                                    "expected: {}. Trying to load anyway but some this most definitely will be broken!",
                                    modul.getName(), lastVersion, dataVersion);
                            }
                        } else {
                            plugin.getComponentLogger().warn("The data version for modul config for \"{}\" was missing." +
                                " Proceed with care!", modul.getName());
                        }

                        isEnabled.setValue(config.getBoolean(isEnabled.getPath()));
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

                        config.set(VERSION_PATH, dataVersion);
                        config.set(isEnabled.getPath(), isEnabled.getValueOrFallback());

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
