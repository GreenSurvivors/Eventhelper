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


public class TNTKnockbackConfig extends AModulConfig {

    public TNTKnockbackConfig(@NotNull EventHelper plugin, @NotNull String modulID) {
        super(plugin, modulID);
    }

    @Override
    public @NotNull CompletableFuture<@NotNull Boolean> reload() {
        final CompletableFuture<Boolean> runAfter = new CompletableFuture<>();

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            synchronized (this) {
                if (!Files.isRegularFile(getConfigPath())) {
                    plugin.saveResource(getModulID() + "/" + getConfigPath().getFileName().toString(), false);
                }

                try (BufferedReader bufferedReader = Files.newBufferedReader(getConfigPath())) {
                    @NotNull YamlConfiguration config = YamlConfiguration.loadConfiguration(bufferedReader);

                    @Nullable String dataVersionStr = config.getString(VERSION_PATH);
                    if (dataVersionStr != null) {
                        ComparableVersion lastVersion = new ComparableVersion(dataVersionStr);

                        if (dataVersion.compareTo(lastVersion) < 0) {
                            plugin.getComponentLogger().warn("Found modul config for \"{}\" was saved in a newer data version ({}), " +
                                    "expected: {}. Trying to load anyway but some this most definitely will be broken!",
                                getModulID(), lastVersion, dataVersion);
                        }
                    } else {
                        plugin.getComponentLogger().warn("The data version for modul config for \"{}\" was missing." +
                            " Proceed with care!", getModulID());
                    }

                    isEnabled.setValue(config.getBoolean(isEnabled.getPath()));
                    Bukkit.getScheduler().runTask(plugin, () -> runAfter.complete(isEnabled.getValueOrFallback())); // back to main thread
                } catch (IOException e) {
                    plugin.getComponentLogger().error("Could not load modul config for {} from file!", getModulID(), e);

                    isEnabled.setValue(Boolean.FALSE);
                    Bukkit.getScheduler().runTask(plugin, () -> runAfter.complete(Boolean.FALSE)); // back to main thread
                }
            }
        });

        return runAfter;
    }

    @Override
    public @NotNull CompletableFuture<@NotNull Boolean> save() {
        final CompletableFuture<@NotNull Boolean> runAfter = new CompletableFuture<>();

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            synchronized (this) {
                if (!Files.isRegularFile(getConfigPath())) {
                    plugin.saveResource(getModulID() + "/" + getConfigPath().getFileName().toString(), false);
                }

                try (BufferedReader bufferedReader = Files.newBufferedReader(getConfigPath())) {
                    @NotNull YamlConfiguration config = YamlConfiguration.loadConfiguration(bufferedReader);

                    config.set(VERSION_PATH, dataVersion.toString());
                    config.set(isEnabled.getPath(), isEnabled.getValueOrFallback());

                    config.options().parseComments(true);
                    config.save(getConfigPath().toFile());

                    Bukkit.getScheduler().runTask(plugin, () -> runAfter.complete(Boolean.TRUE)); // back to main thread
                } catch (IOException e) {
                    plugin.getComponentLogger().error("Could not load modul config for {} from file!", getModulID(), e);

                    isEnabled.setValue(Boolean.FALSE);
                    Bukkit.getScheduler().runTask(plugin, () -> runAfter.complete(Boolean.TRUE)); // back to main thread
                }
            }
        });

        return runAfter;
    }
}
