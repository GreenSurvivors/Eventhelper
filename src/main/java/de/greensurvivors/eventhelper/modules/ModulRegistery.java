package de.greensurvivors.eventhelper.modules;

import de.greensurvivors.eventhelper.EventHelper;
import de.greensurvivors.eventhelper.modules.ghost.GhostModul;
import de.greensurvivors.eventhelper.modules.inventory.InventoryRegionModul;
import de.greensurvivors.eventhelper.modules.tnt.TNTKnockbackModul;
import net.kyori.adventure.key.Key;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginDisableEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public class ModulRegistery implements Listener {
    private final @NotNull Map<@NotNull String, @NotNull AModul<?>> registeredModules = new HashMap<>();
    private final @NotNull EventHelper plugin;

    public ModulRegistery(@NotNull EventHelper plugin) {
        this.plugin = plugin;

        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    /**
     * registers a new module (will not get automatically reloaded)
     * Every new module will overwrite any already registered module by the same name.
     *
     * @return true, if there was no module previously registered by the same name
     */
    public boolean registerNewModule(final @NotNull AModul<?> newModule) {
        return registeredModules.put(newModule.getName(), newModule) != null;
    }

    /// @return the module associated by the given name, or null if none was found
    public @Nullable AModul<?> getModuleByName(final @NotNull String modulName) {
        return registeredModules.get(modulName);
    }

    /// registers all builtin modules, depending on, if their dependencies are meet
    public void registerDefault() {
        // don't check if worldguard is enabled yet, since we have to do our registration before it does load
        if (plugin.getDependencyManager().isWorldGuardInstanceSafe()) {
            registerNewModule(new TNTKnockbackModul(plugin));
            registerNewModule(new InventoryRegionModul(plugin));

            if (plugin.getDependencyManager().isSimpleQuestsEnabled()) {
                registerNewModule(new GhostModul(plugin));
            } else {
                plugin.getComponentLogger().warn("Could not enable all Modules because optional dependency SimpleQuests is missing!");
            }
        } else {
            plugin.getComponentLogger().warn("Could not enable all Modules because optional dependency Worldguard is missing!");
        }
    }

    /// registers all builtin modules and reloads them
    public void onEnable() {
        registerDefault();
        reloadAll();
    }

    /**
     * disables all modules temporarily.
     * This means, if the module gets reloaded later, and it should be enabled by its config, it will get enabled again.
     */
    public void disableAll() {
        for (final @NotNull AModul<?> modul : registeredModules.values()) {
            if (modul.getConfig().isEnabled()) {
                new StateChangeEvent<>(Key.key(modul.getName(), modul.getName()), false).callEvent();
            }
        }
    }

    ///  reloads all modules
    public void reloadAll() {
        for (final @NotNull AModul<?> modul : registeredModules.values()) {
            final boolean wasEnabled = modul.getConfig().isEnabled();

            modul.getConfig().reload().thenAccept(isEnabled -> {
                final Key modulKey = Key.key(modul.getName(), modul.getName());
                if (isEnabled) {
                    new StateChangeEvent<>(modulKey, true).callEvent();
                } else if (wasEnabled) {
                    new StateChangeEvent<>(modulKey, false).callEvent();
                }
            });
        }
    }

    /**
     * The onDisable Method of java plugin is after the plugin was marked as disabled by the server.
     * If a plugin is marked as disabled all it's EventHandlers are dead.
     */
    @EventHandler(ignoreCancelled = true)
    private void onPluginDisable(final @NotNull PluginDisableEvent event) {
        if (event.getPlugin() == plugin) {
            disableAll();
        }
    }
}

