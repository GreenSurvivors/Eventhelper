package de.greensurvivors.eventhelper;

import de.greensurvivors.eventhelper.command.MainCmd;
import de.greensurvivors.eventhelper.messages.MessageManager;
import de.greensurvivors.eventhelper.modules.ModulRegistery;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

public class EventHelper extends JavaPlugin {
    private static @NotNull EventHelper instance;

    private @NotNull MainCmd mainCmd;
    private @NotNull MessageManager messageManager;
    private @NotNull DependencyManager dependencyManager;
    private @NotNull ModulRegistery modulRegistery;

    public EventHelper() {
        super();

        instance = this;
    }

    @Deprecated // only use if you really have to!
    public static @NotNull EventHelper getPlugin() {
        return instance;
    }

    @Override
    public void onEnable() {
        // listener
        messageManager = new MessageManager(this);
        dependencyManager = new DependencyManager(this);
        mainCmd = new MainCmd(this);

        // register modules
        modulRegistery = new ModulRegistery(this);
        modulRegistery.registerDefault();
        modulRegistery.onEnable();
    }

    @Override
    public void onDisable() {
        getModulRegistery().disableAll();
    }

    public @NotNull MainCmd getMainCmd() {
        return mainCmd;
    }

    public @NotNull MessageManager getMessageManager() {
        return messageManager;
    }

    public @NotNull ModulRegistery getModulRegistery() {
        return modulRegistery;
    }

    public @NotNull DependencyManager getDependencyManager() {
        return dependencyManager;
    }
}
