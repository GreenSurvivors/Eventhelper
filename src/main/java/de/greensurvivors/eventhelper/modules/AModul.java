package de.greensurvivors.eventhelper.modules;

import de.greensurvivors.eventhelper.EventHelper;
import net.kyori.adventure.key.KeyPattern;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;

public abstract class AModul<Config extends AModulConfig> implements Listener {
    protected final @NotNull EventHelper plugin;
    protected final @NotNull Config config;

    protected AModul(final @NotNull EventHelper plugin, final @NotNull Config config) {
        this.plugin = plugin;
        this.config = config;
    }

    /**
     * @return the unique unchanging name, of this modul
     */
    public abstract @NotNull @KeyPattern.Namespace String getName();

    protected abstract void onConfigEnabledChange(final boolean newState);

    public @NotNull Config getConfig() {
        return config;
    }
}
