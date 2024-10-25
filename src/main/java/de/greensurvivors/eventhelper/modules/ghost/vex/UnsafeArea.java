package de.greensurvivors.eventhelper.modules.ghost.vex;

import de.greensurvivors.eventhelper.modules.ghost.player.AlivePlayer;
import org.bukkit.Location;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.Map;

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

    @Override
    public @NotNull Map<String, Object> serialize() {
        return Map.of(
            CENTER_LOCATION_KEY, center,
            RADIUS_KEY, radius,
            WARN_INTERVAL_KEY, warnInterval,
            TIME_UNTIL_DEATH, timeUntilDeath
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
