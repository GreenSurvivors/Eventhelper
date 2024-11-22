package de.greensurvivors.eventhelper.modules.ghost;

import de.greensurvivors.eventhelper.EventHelper;
import de.greensurvivors.eventhelper.Utils;
import de.greensurvivors.eventhelper.messages.MessageManager;
import de.greensurvivors.eventhelper.messages.SharedPlaceHolder;
import de.greensurvivors.eventhelper.modules.ghost.player.AlivePlayer;
import io.papermc.paper.math.Position;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

@SuppressWarnings("UnstableApiUsage") // Position
public class MouseTrap implements ConfigurationSerializable, Listener {
    private final static @NotNull String
        WORLD_NAME_KEY = "worldName",
        SPAWN_POS_IN_KEY = "spawnPositionIn",
        SPAWN_POS_OUT_KEY = "spawnPositionOut",
        RELEASE_BUTTON_POS_KEY = "releaseButtonPosition",
        UNSAFE_AREA_KEY = "unsafeArea";

    private final @NotNull EventHelper plugin;
    private final @NotNull Position spawnPositionIn;
    private final @NotNull Position spawnPositionOut;
    private final @NotNull String worldName;
    private final @NotNull Position releaseButtonPosition;
    private final @Nullable UnsafeArea unsafeArea;

    private final transient @NotNull Map<@NotNull AlivePlayer, @NotNull Long> trappedPlayers = new HashMap<>();

    /**
     * Don't forget to call {@link #onEnable()}
     */
    public MouseTrap(final @NotNull EventHelper plugin,
                     final @NotNull String worldName,
                     final @NotNull Position spawnPositionIn,
                     final @NotNull Position spawnPositionOut,
                     final @NotNull Position releaseButtonPosition,
                     final @Nullable UnsafeArea unsafeArea) {
        this.plugin = plugin;
        this.spawnPositionIn = spawnPositionIn;
        this.worldName = worldName;
        this.spawnPositionOut = spawnPositionOut;
        this.releaseButtonPosition = releaseButtonPosition;
        this.unsafeArea = unsafeArea;
    }

    /**
     * Don't forget to call {@link #onEnable()}
     */
    @SuppressWarnings("unused") // used in ConfigurationSerializable
    public static @NotNull MouseTrap deserialize(final @NotNull Map<String, Object> map) throws RuntimeException {
        if (map.get(WORLD_NAME_KEY) instanceof String worldName) {
            if (map.get(SPAWN_POS_IN_KEY) instanceof Map<?, ?> spawnPosInMap) {
                final @NotNull Position spawnPositionIn = Utils.deserializePosition(Utils.checkSerialzedMap(spawnPosInMap, ignored -> {
                }));

                if (map.get(SPAWN_POS_OUT_KEY) instanceof Map<?, ?> spawnPosOutMap) {
                    final @NotNull Position spawnPositionOut = Utils.deserializePosition(Utils.checkSerialzedMap(spawnPosOutMap, ignored -> {
                    }));

                    if (map.get(RELEASE_BUTTON_POS_KEY) instanceof Map<?, ?> releaseButtonPosMap) {
                        Position releaseButtonPosition = Utils.deserializePosition(Utils.checkSerialzedMap(releaseButtonPosMap, ignored -> {
                        }));
                        if (map.get(UNSAFE_AREA_KEY) instanceof UnsafeArea unsafeArea) {
                            return new MouseTrap(EventHelper.getPlugin(), worldName, spawnPositionIn, spawnPositionOut, releaseButtonPosition, unsafeArea);
                        } else {
                            return new MouseTrap(EventHelper.getPlugin(), worldName, spawnPositionIn, spawnPositionOut, releaseButtonPosition, null);
                        }
                    } else {
                        throw new NoSuchElementException("Serialized MouseTrap " + map + " does not contain a release button position value.");
                    }
                } else {
                    throw new NoSuchElementException("Serialized MouseTrap " + map + " does not contain a release spawn position value.");
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

    /// will stop keeping track on all trapped players but will not release them!
    public void onDisable() {
        HandlerList.unregisterAll(this);
        trappedPlayers.clear();
    }

    @Override
    public @NotNull Map<@NotNull String, @Nullable Object> serialize() {
        final @NotNull Map<@NotNull String, @Nullable Object> serializedMap = new LinkedHashMap<>();
        serializedMap.put(WORLD_NAME_KEY, worldName);
        serializedMap.put(SPAWN_POS_IN_KEY, Utils.serializePosition(spawnPositionIn));
        serializedMap.put(SPAWN_POS_OUT_KEY, Utils.serializePosition(spawnPositionOut));
        serializedMap.put(RELEASE_BUTTON_POS_KEY, Utils.serializePosition(releaseButtonPosition));
        serializedMap.put(UNSAFE_AREA_KEY, unsafeArea);
        return serializedMap;
    }

    /**
     * important: use AlivePlayer.trapInMouseTrap instead of this one!
     * the AlivePlayer will call this method. But if you skip that part they will not know they are trapped!
     */
    public boolean trapPlayer(final @NotNull AlivePlayer alivePlayer) {
        final @Nullable World world = Bukkit.getWorld(worldName);

        if (world != null) {
            alivePlayer.getBukkitPlayer().teleportAsync(spawnPositionIn.toLocation(world), PlayerTeleportEvent.TeleportCause.PLUGIN);

            trappedPlayers.put(alivePlayer, System.currentTimeMillis());

            alivePlayer.getGame().broadcastAll(GhostLangPath.PLAYER_CAPTURED,
                Placeholder.component(SharedPlaceHolder.PLAYER.getKey(), alivePlayer.getBukkitPlayer().displayName()),
                Placeholder.component(SharedPlaceHolder.TIME.getKey(), MessageManager.formatTime(alivePlayer.getGame().getConfig().getDurationTrappedUntilDeath())));
            return true;
        } else {
            return false;
        }
    }

    /// this does NOT release the player but simply stop tracking them
    public void removePlayer(final @NotNull AlivePlayer alivePlayer) {
        trappedPlayers.remove(alivePlayer);
    }

    public void releaseAllPlayers() {
        final @Nullable World world = Bukkit.getWorld(worldName);
        final @Nullable Location spawnLocationOut;
        if (world != null) {
            spawnLocationOut = spawnPositionOut.toLocation(world);
        } else {
            spawnLocationOut = null;
        }

        for (Iterator<AlivePlayer> iterator = trappedPlayers.keySet().iterator(); iterator.hasNext(); ) {
            AlivePlayer alivePlayer = iterator.next();

            if (spawnLocationOut != null) {
                alivePlayer.getBukkitPlayer().teleport(spawnLocationOut, PlayerTeleportEvent.TeleportCause.PLUGIN);
            }

            alivePlayer.releaseFromTrap();

            iterator.remove();
        }

        trappedPlayers.clear();
    }

    public @NotNull Map<@NotNull AlivePlayer, @NotNull Long> getTrappedPlayers() {
        return trappedPlayers;
    }

    /// the location of the block an alive player have to interact with in order to release all trapped players of this trap
    public boolean isReleaseBlockLocation(final @NotNull Location location) {
        return worldName.equals(location.getWorld().getName()) &&
            (Math.sqrt(releaseButtonPosition.x() - location.x()) +
                Math.sqrt(releaseButtonPosition.y() - location.y()) +
                Math.sqrt(releaseButtonPosition.z() - location.z())) <= 0.25;
    }

    /// to avoid players camping at the {@link #isReleaseBlockLocation(Location)}
    public @Nullable UnsafeArea getUnsafeArea() {
        return unsafeArea;
    }
}
