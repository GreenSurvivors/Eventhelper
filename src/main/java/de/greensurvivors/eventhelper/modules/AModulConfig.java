package de.greensurvivors.eventhelper.modules;

import de.greensurvivors.eventhelper.EventHelper;
import de.greensurvivors.eventhelper.config.ConfigOption;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

public abstract class AModulConfig<ModulType extends AModul<?>> {
    protected final @NotNull ConfigOption<Boolean> isEnabled = new ConfigOption<>("enabled", Boolean.TRUE);
    protected final @NotNull EventHelper plugin;
    protected @Nullable ModulType modul;

    public AModulConfig(@NotNull EventHelper plugin) {
        this.plugin = plugin;
    }

    public @Nullable ModulType getModul() {
        return modul;
    }

    public void setModul(@NotNull ModulType modul) {
        this.modul = modul;
    }

    public abstract CompletableFuture<Boolean> reload();

    public boolean isEnabled() {
        return isEnabled.getValueOrFallback();
    }
}
