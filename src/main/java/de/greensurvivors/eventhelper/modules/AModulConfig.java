package de.greensurvivors.eventhelper.modules;

import de.greensurvivors.eventhelper.EventHelper;
import de.greensurvivors.eventhelper.config.ConfigOption;
import org.apache.maven.artifact.versioning.ComparableVersion;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

public abstract class AModulConfig<Modul extends AModul<?>> {
    /// config key for {@link #dataVersion}
    protected final static @NotNull String VERSION_PATH = "dataVersion";
    /// the version this config is saved under. Will be important, when the config data evolves, and we need a datafixerupper
    protected final @NotNull ComparableVersion dataVersion;
    /// whenever or not this module is enabled
    protected final @NotNull ConfigOption<Boolean> isEnabled = new ConfigOption<>("enabled", Boolean.TRUE);
    private final @NotNull Path configSpecificPath;
    protected final @NotNull EventHelper plugin;
    /**
     * will be null, if the {@link #modul} wasn't set yet.
     *
     * @see #setModul(AModul)
     */
    protected @Nullable Path configPath = null;
    /// @see #setModul(AModul)
    private @Nullable Modul modul = null;

    /// DON'T FORGET TO CALL {@link #setModul(AModul)} AS SOON AS POSSIBLE}
    public AModulConfig(final @NotNull EventHelper plugin) {
        this(plugin, Path.of("config.yaml"));
    }

    /// DON'T FORGET TO CALL {@link #setModul(AModul)} AS SOON AS POSSIBLE}
    public AModulConfig(final @NotNull EventHelper plugin, final @NotNull Path configSpecificPath) {
        this(plugin, configSpecificPath, new ComparableVersion("1.0.0"));
    }

    /// DON'T FORGET TO CALL {@link #setModul(AModul)} AS SOON AS POSSIBLE}
    public AModulConfig(final @NotNull EventHelper plugin,
                        final @NotNull Path configSpecificPath,
                        final @NotNull ComparableVersion dataVersion) {
        this.plugin = plugin;
        this.configSpecificPath = configSpecificPath;
        this.dataVersion = dataVersion;
    }

    /**
     * if this ever returns {@code null},
     * that means this class wasn't properly initialized and {@link #setModul(AModul)} wasn't called.
     */
    public @Nullable Modul getModul() {
        return modul;
    }

    /**
     * THIS MUST BE CALLED AS SOON AS POSSIBLE AFTER CREATING A NEW INSTANCE, ONCE AND ONLY ONCE!!
     * <p>
     * The module and its config are circular dependencies.
     * And since you can't provide the Module instance in the Module constructor, so before the super call,
     * nor a {@link  java.util.concurrent.Future} (problem of saving the created instance),
     * this has to be called right after the super call in the Module's constructor.
     */
    public void setModul(final @NotNull Modul modul) throws UnsupportedOperationException {
        if (this.modul == null) {
            this.modul = modul;
            configPath = plugin.getDataFolder().toPath().resolve(modul.getName()).resolve(configSpecificPath);
        } else {
            throw new UnsupportedOperationException("module can only assigned once!");
        }
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
