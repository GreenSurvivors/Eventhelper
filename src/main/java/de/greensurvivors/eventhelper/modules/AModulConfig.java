package de.greensurvivors.eventhelper.modules;

import de.greensurvivors.eventhelper.EventHelper;
import de.greensurvivors.eventhelper.config.ConfigOption;
import org.apache.maven.artifact.versioning.ComparableVersion;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

public abstract class AModulConfig<ModulType extends AModul<?>> {
    protected final static @NotNull String VERSION_PATH = "dataVersion";
    protected final @NotNull ComparableVersion dataVersion;
    protected final @NotNull ConfigOption<Boolean> isEnabled = new ConfigOption<>("enabled", Boolean.TRUE);
    protected final @NotNull EventHelper plugin;
    protected @Nullable ModulType modul;
    protected @NotNull Path configPath;
    private @NotNull Path configSpecificPath;

    public AModulConfig(final @NotNull EventHelper plugin) {
        this(plugin, Path.of("config.yaml"));
    }

    public AModulConfig(final @NotNull EventHelper plugin, final @NotNull Path configSpecificPath) {
        this(plugin, configSpecificPath, new ComparableVersion("1.0.0"));
    }

    public AModulConfig(final @NotNull EventHelper plugin,
                        final @NotNull Path configSpecificPath,
                        final @NotNull ComparableVersion dataVersion) {
        this.plugin = plugin;
        this.configSpecificPath = configSpecificPath;
        this.dataVersion = dataVersion;
    }

    public @Nullable ModulType getModul() { // always have to be called!
        return modul;
    }

    public void setModul(final @NotNull ModulType modul) {
        this.modul = modul;
        configPath = plugin.getDataFolder().toPath().resolve(modul.getName()).resolve(configSpecificPath);
    }

    public abstract @NotNull CompletableFuture<@NotNull Boolean> reload();

    public abstract @NotNull CompletableFuture<Void> save();

    public boolean isEnabled() {
        return isEnabled.getValueOrFallback();
    }
}
