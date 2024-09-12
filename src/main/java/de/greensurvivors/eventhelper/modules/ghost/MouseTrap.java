package de.greensurvivors.eventhelper.modules.ghost;

import de.greensurvivors.eventhelper.EventHelper;
import de.greensurvivors.eventhelper.Utils;
import io.papermc.paper.math.Position;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.NoSuchElementException;

@SuppressWarnings("UnstableApiUsage") // Position
public class MouseTrap implements ConfigurationSerializable {
    private final static @NotNull String WORLD_NAME_KEY = "worldName";
    private final static @NotNull String SPAWN_POS_KEY = "spawnPosition";
    private final static @NotNull String REDSTONE_PULSE_POS_KEY = "redstonePulsePosition";

    static {
        ConfigurationSerialization.registerClass(MouseTrap.class);
    }

    private final @NotNull Position spawnPosition;
    private final @NotNull String worldName;
    private final @NotNull Position redstonePulsePosition;
    private final @NotNull EventHelper plugin;

    public MouseTrap(final @NotNull EventHelper plugin,
                     final @NotNull String worldName,
                     final @NotNull Position spawnPosition,
                     final @NotNull Position redstonePulsePosition) {
        this.plugin = plugin;
        this.spawnPosition = spawnPosition;
        this.worldName = worldName;
        this.redstonePulsePosition = redstonePulsePosition;
    }

    public static @NotNull MouseTrap deserialize(final @NotNull Map<String, Object> map) throws RuntimeException {
        if (map.get(WORLD_NAME_KEY) instanceof String worldName) {
            if (map.get(SPAWN_POS_KEY) instanceof Map<?, ?> spawnPosMap) {
                final @NotNull Position spawnPosition = Utils.deserializePosition(Utils.checkSerialzedMap(spawnPosMap, ignored -> {
                }));

                if (map.get(REDSTONE_PULSE_POS_KEY) instanceof Map<?, ?> redstonePulsePosMap) {
                    Position redstonePulsePosition = Utils.deserializePosition(Utils.checkSerialzedMap(redstonePulsePosMap, ignored -> {
                    }));

                    return new MouseTrap(EventHelper.getPlugin(), worldName, spawnPosition, redstonePulsePosition);
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

    @Override
    public @NotNull Map<String, Object> serialize() {
        return Map.of(
            WORLD_NAME_KEY, worldName,
            SPAWN_POS_KEY, Utils.serializePosition(spawnPosition),
            REDSTONE_PULSE_POS_KEY, Utils.serializePosition(redstonePulsePosition));
    }

    public boolean trapPlayer(final @NotNull Player player) {
        final @Nullable World world = Bukkit.getWorld(worldName);

        if (world != null) {
            player.teleport(spawnPosition.toLocation(world), PlayerTeleportEvent.TeleportCause.PLUGIN);
            final Location redstonePulseLocation = redstonePulsePosition.toLocation(world);
            final BlockData data = redstonePulseLocation.getBlock().getBlockData();
            redstonePulseLocation.getBlock().setType(Material.REDSTONE_BLOCK);

            Bukkit.getScheduler().runTaskLater(plugin, () -> redstonePulseLocation.getBlock().setBlockData(data), 2);

            return true;
        } else {
            return false;
        }
    }
}
