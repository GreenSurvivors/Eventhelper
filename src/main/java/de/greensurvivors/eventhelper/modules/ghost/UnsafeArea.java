package de.greensurvivors.eventhelper.modules.ghost;

import de.greensurvivors.eventhelper.modules.ghost.player.AlivePlayer;
import org.bukkit.Location;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * An unsafe area is an area, where the player dies if they stay too long
 */
public class UnsafeArea implements ConfigurationSerializable {
    private final static @NotNull String
        CENTER_LOCATION_KEY = "centerLocation",
        RADIUS_KEY = "distance",
        WARN_INTERVAL_KEY = "warnInterval",
        TIME_UNTIL_DEATH = "timeUntilDeath";

    private final @NotNull Location center;
    private final int radius;
    private transient final long radiusSquared;
    private final @NotNull Duration warnInterval;
    private final @NotNull Duration timeUntilDeath;

    public UnsafeArea(final @NotNull Location center,
                      final int radius,
                      final @NotNull Duration warnInterval,
                      final @NotNull Duration timeUntilDeath) {
        this.center = center;
        this.radius = radius;
        this.radiusSquared = (long) radius * (long) radius;
        this.warnInterval = warnInterval;
        this.timeUntilDeath = timeUntilDeath;
    }

    @SuppressWarnings("unused") // used by ConfigurationSerializable
    public static UnsafeArea deserialize(@NotNull Map<String, Object> serializedMap) throws NoSuchElementException {
        if (serializedMap.get(CENTER_LOCATION_KEY) instanceof Location center) {
            if (serializedMap.get(RADIUS_KEY) instanceof Number radius) {
                if (serializedMap.get(WARN_INTERVAL_KEY) instanceof Number warnInterval) {
                    if (serializedMap.get(TIME_UNTIL_DEATH) instanceof Number timeUntilDeath) {
                        return new UnsafeArea(center, radius.intValue(), Duration.ofMillis(warnInterval.longValue() * 50), Duration.ofMillis(timeUntilDeath.longValue() * 50));
                    } else {
                        throw new NoSuchElementException("Serialized UnsafeArea " + serializedMap + " does not contain a valid time until death value.");
                    }
                } else {
                    throw new NoSuchElementException("Serialized UnsafeArea " + serializedMap + " does not contain a valid warn interval value.");
                }
            } else {
                throw new NoSuchElementException("Serialized UnsafeArea " + serializedMap + " does not contain a valid distance value.");
            }
        } else {
            throw new NoSuchElementException("Serialized UnsafeArea " + serializedMap + " does not contain a valid center location value.");
        }
    }

    @Override
    public @NotNull Map<String, Object> serialize() {
        return Map.of(
            CENTER_LOCATION_KEY, center,
            RADIUS_KEY, radius,
            WARN_INTERVAL_KEY, warnInterval.toMillis() / 50,
            TIME_UNTIL_DEATH, timeUntilDeath.toMillis() / 50
        );
    }

    public boolean isInArea(final @NotNull AlivePlayer alivePlayer) {
        final @NotNull Location playerLocation = alivePlayer.getBukkitPlayer().getLocation();

        if (playerLocation.getWorld() == center.getWorld()) {
            return playerLocation.distanceSquared(center) <= radiusSquared;
        } else {
            return false;
        }
    }

    public @NotNull Duration getWarnInterval() {
        return warnInterval;
    }

    public @NotNull Duration getTimeUntilDeath() {
        return timeUntilDeath;
    }
}
