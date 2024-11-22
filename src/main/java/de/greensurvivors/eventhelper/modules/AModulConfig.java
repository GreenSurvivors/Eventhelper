package de.greensurvivors.eventhelper.modules;

import de.greensurvivors.eventhelper.EventHelper;
import de.greensurvivors.eventhelper.config.ConfigOption;
import net.kyori.adventure.key.KeyPattern;
import org.apache.maven.artifact.versioning.ComparableVersion;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

public abstract class AModulConfig {
    /// config key for {@link #dataVersion}
    protected final static @NotNull String VERSION_PATH = "dataVersion";
    /// the version this config is saved under. Will be important, when the config data evolves, and we need a datafixerupper
    protected final @NotNull ComparableVersion dataVersion;
    /// whenever or not this module is enabled
    protected final @NotNull ConfigOption<Boolean> isEnabled = new ConfigOption<>("enabled", Boolean.TRUE);
    protected final @NotNull EventHelper plugin;
    private final @NotNull Path configPath;
    private final @NotNull
    @KeyPattern.Namespace String modulID;

    public AModulConfig(final @NotNull EventHelper plugin, final @NotNull @KeyPattern.Namespace String modulId) {
        this(plugin, modulId, Path.of("config.yaml"));
    }

    public AModulConfig(final @NotNull EventHelper plugin,
                        final @NotNull @KeyPattern.Namespace String modulId,
                        final @NotNull Path configSpecificPath) {
        this(plugin, modulId, configSpecificPath, new ComparableVersion("1.0.0"));
    }

    public AModulConfig(final @NotNull EventHelper plugin,
                        final @NotNull @KeyPattern.Namespace String modulId,
                        final @NotNull Path configSpecificPath,
                        final @NotNull ComparableVersion dataVersion) {
        this.plugin = plugin;
        this.modulID = modulId;
        configPath = plugin.getDataFolder().toPath().resolve(modulId).resolve(configSpecificPath);
        this.dataVersion = dataVersion;
    }

    public @NotNull Path getConfigPath() {
        return configPath;
    }

    public @NotNull @KeyPattern.Namespace String getModulID() {
        return modulID;
    }

    /**
     * reloads this config. Because this may happen async, this will return a CompletableFuture.
     *
     * @return a {@code CompletableFuture}, that will complete with true if the config was successfully loaded, and false if not.
     */
    public abstract @NotNull CompletableFuture<@NotNull Boolean> reload();

    /**
     * saves this config. Because this may happen async, this will return a CompletableFuture.
     * @return a {@code CompletableFuture}, that will complete with true if the config was successfully saved, and false if not.
     */
    public abstract @NotNull CompletableFuture<@NotNull Boolean> save();

    /**
     * @return if the modul this config is associated to is enabled
     */
    public boolean isEnabled() {
        return isEnabled.getValueOrFallback();
    }
}
