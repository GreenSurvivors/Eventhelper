package de.greensurvivors.eventhelper.modules;

import de.greensurvivors.eventhelper.EventHelper;
import org.jetbrains.annotations.NotNull;

public abstract class AModul<ConfigType extends AModulConfig<?>> {
    protected final @NotNull EventHelper plugin;
    protected final @NotNull ConfigType config;

    protected AModul(final @NotNull EventHelper plugin, final @NotNull ConfigType config) {
        this.plugin = plugin;
        this.config = config;
    }

    public abstract @NotNull String getName();

    public abstract void onEnable();

    public abstract void onDisable();

    public @NotNull ConfigType getConfig() {
        return config;
    }
}
