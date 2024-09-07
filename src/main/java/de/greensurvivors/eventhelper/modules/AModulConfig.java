package de.greensurvivors.eventhelper.modules;

import de.greensurvivors.eventhelper.EventHelper;
import de.greensurvivors.eventhelper.config.ConfigOption;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

public abstract class AModulConfig<ModulType extends AModul<?>> {
    protected final @NotNull ConfigOption<Boolean> isEnabled = new ConfigOption<>("enabled", Boolean.TRUE);
    protected final @NotNull EventHelper plugin;
    protected @Nullable ModulType modul;
    protected @NotNull Path configPath;

    public AModulConfig(final @NotNull EventHelper plugin) {
        this(plugin, "config.yaml");
    }

    public AModulConfig(final @NotNull EventHelper plugin, final @NotNull String configFileName) {
        this.plugin = plugin;

        configPath = plugin.getDataFolder().toPath().resolve(Path.of(modul.getName(), configFileName));
    }

    public @Nullable ModulType getModul() { // always have to be called!
        return modul;
    }

    public void setModul(@NotNull ModulType modul) {
        this.modul = modul;
    }

    public abstract @NotNull CompletableFuture<@NotNull Boolean> reload();

    public boolean isEnabled() {
        return isEnabled.getValueOrFallback();
    }
}
