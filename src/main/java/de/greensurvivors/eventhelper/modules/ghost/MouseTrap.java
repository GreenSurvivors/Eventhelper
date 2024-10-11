package de.greensurvivors.eventhelper.modules.ghost;

import de.greensurvivors.eventhelper.EventHelper;
import de.greensurvivors.eventhelper.Utils;
import de.greensurvivors.eventhelper.modules.ghost.payer.AlivePlayer;
import io.papermc.paper.math.Position;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

@SuppressWarnings("UnstableApiUsage") // Position
public class MouseTrap implements ConfigurationSerializable, Listener { // todo freeing mechanism
    private final static @NotNull String
        WORLD_NAME_KEY = "worldName",
        SPAWN_POS_KEY = "spawnPosition",
        REDSTONE_PULSE_POS_KEY = "redstonePulsePosition",
        RELEASE_BUTTON_POS_KEY = "releaseButtonPosition";

    private final @NotNull EventHelper plugin;
    private final @NotNull Position spawnPosition;
    private final @NotNull String worldName;
    private final @NotNull Position redstonePulsePosition;
    private final @NotNull Position releaseButtonPosition;

    private final transient @NotNull Map<@NotNull AlivePlayer, @NotNull Integer> trappedPlayers = new HashMap<>();

    /**
     * Don't forget to call {@link #onEnable()}
     */
    public MouseTrap(final @NotNull EventHelper plugin,
                     final @NotNull String worldName,
                     final @NotNull Position spawnPosition,
                     final @NotNull Position redstonePulsePosition,
                     final @NotNull Position releaseButtonPosition) {
        this.plugin = plugin;
        this.spawnPosition = spawnPosition;
        this.worldName = worldName;
        this.redstonePulsePosition = redstonePulsePosition;
        this.releaseButtonPosition = releaseButtonPosition;
    }

    /**
     * Don't forget to call {@link #onEnable()}
     */
    @SuppressWarnings("unused") // used in ConfigurationSerializable
    public static @NotNull MouseTrap deserialize(final @NotNull Map<String, Object> map) throws RuntimeException {
        if (map.get(WORLD_NAME_KEY) instanceof String worldName) {
            if (map.get(SPAWN_POS_KEY) instanceof Map<?, ?> spawnPosMap) {
                final @NotNull Position spawnPosition = Utils.deserializePosition(Utils.checkSerialzedMap(spawnPosMap, ignored -> {
                }));

                if (map.get(REDSTONE_PULSE_POS_KEY) instanceof Map<?, ?> redstonePulsePosMap) {
                    Position redstonePulsePosition = Utils.deserializePosition(Utils.checkSerialzedMap(redstonePulsePosMap, ignored -> {
                    }));

                    if (map.get(RELEASE_BUTTON_POS_KEY) instanceof Map<?, ?> releaseButtonPosMap) {
                        Position releaseButtonPosition = Utils.deserializePosition(Utils.checkSerialzedMap(releaseButtonPosMap, ignored -> {
                        }));

                        return new MouseTrap(EventHelper.getPlugin(), worldName, spawnPosition, redstonePulsePosition, releaseButtonPosition);
                    } else {
                        throw new NoSuchElementException("Serialized MouseTrap " + map + " does not contain a release button position value.");
                    }
                } else {
                    throw new NoSuchElementException("Serialized MouseTrap " + map + " does not contain a redstone pulse position value.");
                }
            } else {
                throw new NoSuchElementException("Serialized MouseTrap " + map + " does not contain a spawn position value.");
            }
        } else {
            throw new NoSuchElementException("Serialized MouseTrap " + map + " does not contain a valid world name value.");
        }
    }

    public void onEnable() {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    public void onDisable() {
        HandlerList.unregisterAll(this);
        trappedPlayers.clear();
    }

    @Override
    public @NotNull Map<String, Object> serialize() {
        return Map.of(
            WORLD_NAME_KEY, worldName,
            SPAWN_POS_KEY, Utils.serializePosition(spawnPosition),
            REDSTONE_PULSE_POS_KEY, Utils.serializePosition(redstonePulsePosition),
            RELEASE_BUTTON_POS_KEY, Utils.serializePosition(releaseButtonPosition));
    }

    public boolean trapPlayer(final @NotNull AlivePlayer alivePlayer) {
        final @Nullable World world = Bukkit.getWorld(worldName);

        if (world != null) {
            alivePlayer.getBukkitPlayer().teleport(spawnPosition.toLocation(world), PlayerTeleportEvent.TeleportCause.PLUGIN);
            final Location redstonePulseLocation = redstonePulsePosition.toLocation(world);
            final BlockData data = redstonePulseLocation.getBlock().getBlockData();

            // close door, update redstone and wait for a redstone tick
            redstonePulseLocation.getBlock().setType(Material.REDSTONE_BLOCK);
            Bukkit.getScheduler().runTaskLater(plugin, () -> redstonePulseLocation.getBlock().setBlockData(data), 2);

            trappedPlayers.put(alivePlayer, 0);

            return true;
        } else {
            return false;
        }
    }

    public void removePlayer(final @NotNull AlivePlayer alivePlayer) {
        trappedPlayers.remove(alivePlayer);
    }

    public void releaseAllPlayers() { // todo optional message
        trappedPlayers.clear();
    }

    public @NotNull Map<@NotNull AlivePlayer, @NotNull Integer> getTrappedPlayers() {
        return trappedPlayers;
    }
}
