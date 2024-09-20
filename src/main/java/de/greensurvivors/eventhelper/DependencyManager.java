package de.greensurvivors.eventhelper;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class DependencyManager {
    private final @NotNull EventHelper plugin;
    private final @Nullable Plugin worldGuard;

    public DependencyManager(final @NotNull EventHelper plugin) {
        this.plugin = plugin;

        this.worldGuard = Bukkit.getPluginManager().getPlugin("WorldGuard");
    }

    public boolean isWorldGuardEnabled() {
        return worldGuard != null && worldGuard.isEnabled();
    }

    public boolean isWorldGuardInstanceSafe() {
        return worldGuard != null;
    }
}
