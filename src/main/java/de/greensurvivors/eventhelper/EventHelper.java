package de.greensurvivors.eventhelper;

import com.google.common.collect.ImmutableMap;
import de.greensurvivors.eventhelper.command.MainCmd;
import de.greensurvivors.eventhelper.config.SharedConfig;
import de.greensurvivors.eventhelper.messages.MessageManager;
import de.greensurvivors.eventhelper.modules.ModulRegistery;
import de.greensurvivors.eventhelper.modules.ghost.ghostentity.NMSGhostEntity;
import de.greensurvivors.eventhelper.modules.ghost.ghostentity.NMSUnderWorldGhostEntity;
import de.greensurvivors.eventhelper.modules.ghost.vex.NMSVexEntity;
import net.minecraft.core.registries.BuiltInRegistries;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.craftbukkit.v1_20_R3.util.CraftNamespacedKey;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;

public class EventHelper extends JavaPlugin {
    private static @NotNull EventHelper instance;

    private @NotNull MainCmd mainCmd;
    private @NotNull MessageManager messageManager;
    private @NotNull SharedConfig sharedConfig;
    private @NotNull DependencyManager dependencyManager;
    private @NotNull ModulRegistery modulRegistery;

    // the instance of a Java plugin is created before the whole server api is loaded.
    // that's why we make use of the {@link #onEnable()} method.
    public EventHelper() {
        super();

        instance = this;
    }

    @Deprecated // only use if you really have to!
    public static @NotNull EventHelper getPlugin() {
        return instance;
    }

    /**
     * The onDisable Method of java plugin is after the plugin was marked as disabled by the server.
     * If a plugin is marked as disabled all it's EventHandlers are dead.
     * We need them, since the StateChange event stops all modules.
     * That's why we make use of the {@link #onEnable()} method, but not of the {@link #onDisable()} one.
     */
    @Override
    public void onEnable() {
        // listener
        messageManager = new MessageManager(this);
        sharedConfig = new SharedConfig(this);
        sharedConfig.reload(); // this sets the language of the message manager
        dependencyManager = new DependencyManager(this);
        mainCmd = new MainCmd(this);
        // register modules
        modulRegistery = new ModulRegistery(this);
        modulRegistery.onEnable();

        try {
            // don't break expectations by using another data type
            ImmutableMap.Builder<NamespacedKey, org.bukkit.entity.EntityType> builder = ImmutableMap.builder();

            // re-add all other entities
            for (org.bukkit.entity.EntityType bukkitEntityType : Registry.ENTITY_TYPE) {
                builder.put(Registry.ENTITY_TYPE.getKey(bukkitEntityType), bukkitEntityType);
            }

            // add our own entities
            NamespacedKey ghostNamespacedKey = CraftNamespacedKey.fromMinecraft(BuiltInRegistries.ENTITY_TYPE.getResourceKey(NMSGhostEntity.GHOST_TYPE).orElseThrow().location());
            NamespacedKey underworldNamespacedKey = CraftNamespacedKey.fromMinecraft(BuiltInRegistries.ENTITY_TYPE.getResourceKey(NMSUnderWorldGhostEntity.UNDERWORLD_GHOST_TYPE).orElseThrow().location());
            NamespacedKey vexNamespacedKey = CraftNamespacedKey.fromMinecraft(BuiltInRegistries.ENTITY_TYPE.getResourceKey(NMSVexEntity.VEX_TYPE).orElseThrow().location());
            builder.put(ghostNamespacedKey, org.bukkit.entity.EntityType.GHAST);
            builder.put(underworldNamespacedKey, org.bukkit.entity.EntityType.ALLAY);
            builder.put(vexNamespacedKey, org.bukkit.entity.EntityType.VEX);

            // Access the private field
            Field privateAttributesField = Registry.SimpleRegistry.class.getDeclaredField("map");

            // Make the field accessible
            privateAttributesField.setAccessible(true);

            // set the new map.
            privateAttributesField.set(Registry.ENTITY_TYPE, builder.build());

            // set the accessibility back
            privateAttributesField.setAccessible(false);

            getComponentLogger().info("Successfully hacked into Bukkits entity type registry");
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
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

    public void reloadAll() {
        sharedConfig.reload();
        modulRegistery.reloadAll();
    }
}
