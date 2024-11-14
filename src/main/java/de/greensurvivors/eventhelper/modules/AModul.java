package de.greensurvivors.eventhelper.modules;

import de.greensurvivors.eventhelper.EventHelper;
import org.jetbrains.annotations.NotNull;

public abstract class AModul<Config extends AModulConfig<?>> {
    protected final @NotNull EventHelper plugin;
    protected final @NotNull Config config;

    protected AModul(final @NotNull EventHelper plugin, final @NotNull Config config) {
        this.plugin = plugin;
        this.config = config;
    }

    /**
     * @return the unique unchanging name, of this modul
     */
    public abstract @NotNull String getName();

    /**
     * start of the modules activity
     */
    public abstract void onEnable();

    /**
     * end of the modules activity
     */
    public abstract void onDisable();

    public @NotNull Config getConfig() {
        return config;
    }
}
