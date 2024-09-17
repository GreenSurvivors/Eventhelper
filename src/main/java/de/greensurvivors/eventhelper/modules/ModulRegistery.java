package de.greensurvivors.eventhelper.modules;

import de.greensurvivors.eventhelper.EventHelper;
import de.greensurvivors.eventhelper.modules.ghost.GhostModul;
import de.greensurvivors.eventhelper.modules.inventory.InventoryRegionModul;
import de.greensurvivors.eventhelper.modules.tnt.TNTKnockbackModul;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public class ModulRegistery {
    private final @NotNull Map<@NotNull String, @NotNull AModul<?>> registeredModules = new HashMap<>();
    private final @NotNull EventHelper plugin;

    public ModulRegistery(@NotNull EventHelper plugin) {
        this.plugin = plugin;
    }

    public boolean registerNewModule(final @NotNull AModul<?> newModule) {
        return registeredModules.put(newModule.getName(), newModule) != null;
    }

    public @Nullable AModul<?> getModuleByName(final @NotNull String modulName) {
        return registeredModules.get(modulName);
    }

    public void registerDefault() {
        registerNewModule(new GhostModul(plugin));

        if (plugin.getDependencyManager().isWorldGuardEnabled()) {
            registerNewModule(new TNTKnockbackModul(plugin));
            registerNewModule(new InventoryRegionModul(plugin));
        } else {
            plugin.getComponentLogger().warn("Could not enable all Modules because optional dependency Worldguard is missing!");
        }
    }

    public void onEnable() {
        for (final @NotNull AModul<?> modul : registeredModules.values()) {
            final boolean wasEnabled = modul.getConfig().isEnabled();

            modul.getConfig().reload().thenAccept(isEnabled -> {
                if (isEnabled) {
                    modul.onEnable();
                } else if (wasEnabled){
                    modul.onDisable();
                }
            });
        }
    }

    public void disableAll() {
        for (final @NotNull AModul<?> modul : registeredModules.values()) {
            if (modul.getConfig().isEnabled()) {
                modul.onDisable();
            }
        }
    }
}

