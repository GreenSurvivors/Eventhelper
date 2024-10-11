package de.greensurvivors.eventhelper.config;

import de.greensurvivors.eventhelper.EventHelper;
import org.apache.maven.artifact.versioning.ComparableVersion;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;
import java.util.concurrent.CompletableFuture;

public class SharedConfig { // todo set language locale
    protected final @NotNull ConfigOption<@NotNull Locale> locale = new ConfigOption<>("language", Locale.ENGLISH);
    protected final @NotNull ConfigOption<@NotNull ComparableVersion> dataVersion = new ConfigOption<>("dataVersion", new ComparableVersion("1.0.0"));
    protected final @NotNull EventHelper plugin;

    public SharedConfig(final @NotNull EventHelper plugin) {
        this.plugin = plugin;
    }

    public @NotNull CompletableFuture<@NotNull Boolean> reload() {
        final CompletableFuture<Boolean> runAfter = new CompletableFuture<>();

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            synchronized (this) {
                plugin.saveDefaultConfig();
                @NotNull FileConfiguration config = plugin.getConfig();

                @Nullable String dataVersionStr = config.getString(dataVersion.getPath());
                if (dataVersionStr != null) {
                    ComparableVersion lastVersion = new ComparableVersion(dataVersionStr);

                    if (dataVersion.getValueOrFallback().compareTo(lastVersion) < 0) {
                        plugin.getComponentLogger().warn("Found modul config for shared was saved in a newer data version ({}), " +
                            "expected: {}. Trying to load anyway but some this most definitely will be broken!", lastVersion, dataVersion);
                    }

                    final @Nullable String localeStr = config.getString(locale.getPath());

                    if (localeStr != null) {
                        locale.setValue(Locale.forLanguageTag(localeStr.replace("_", "-")));
                    } else {
                        locale.setValue(null);
                        plugin.getComponentLogger().warn("could not get language setting from config!");
                    }

                    plugin.getMessageManager().setLocale(locale.getValueOrFallback());
                } else {
                    plugin.getComponentLogger().warn("The data version for shared config was missing. Proceed with care!");
                }

                Bukkit.getScheduler().runTask(plugin, () -> runAfter.complete(Boolean.TRUE)); // back to main thread
            }
        });

        return runAfter;
    }

    public @NotNull CompletableFuture<@NotNull Boolean> save() {
        final CompletableFuture<@NotNull Boolean> runAfter = new CompletableFuture<>();

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            synchronized (this) {
                @NotNull FileConfiguration config = plugin.getConfig();

                config.set(dataVersion.getPath(), dataVersion.getValueOrFallback().toString());
                config.set(locale.getPath(), locale.getValueOrFallback().toLanguageTag());

                config.options().parseComments(true);
                plugin.saveConfig();

                Bukkit.getScheduler().runTask(plugin, () -> runAfter.complete(Boolean.TRUE)); // back to main thread
            }
        });

        return runAfter;
    }
}
