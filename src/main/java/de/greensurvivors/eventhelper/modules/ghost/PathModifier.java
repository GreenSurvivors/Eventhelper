package de.greensurvivors.eventhelper.modules.ghost;

import de.greensurvivors.eventhelper.config.ConfigOption;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public class PathModifier implements ConfigurationSerializable {
    private final static String
        FOLLOW_RANGE_KEY = "follow.range",
        FOLLOW_TIMEOUT_KEY = "follow.timeout",
        IDLE_VELOCITY_KEY = "velocity.idle",
        FOLLOW_VELOCITY_KEY = "velocity.follow";

    static {
        ConfigurationSerialization.registerClass(PathModifier.class);
    }

    private final @NotNull ConfigOption<@Nullable Integer> followRange = new ConfigOption<>(FOLLOW_RANGE_KEY, -1); // in blocks
    private final @NotNull ConfigOption<@Nullable Long> followTimeOut = new ConfigOption<>(FOLLOW_TIMEOUT_KEY, -1L); // in milliseconds
    private final @NotNull ConfigOption<@Nullable Double> idleVelocity = new ConfigOption<>(IDLE_VELOCITY_KEY, -1.0D);  // in blocks / s ?
    private final @NotNull ConfigOption<@Nullable Double> followVelocity = new ConfigOption<>(FOLLOW_VELOCITY_KEY, -1.0D); // in blocks / s ?

    public PathModifier() {
    }

    @Override
    public @NotNull Map<String, Object> serialize() {
        final @NotNull Map<String, Object> result = new HashMap<>(5);

        if (followRange.getValueOrFallback() > 0) {
            result.put(followRange.getPath(), followRange.getValueOrFallback());
        }
        if (followTimeOut.getValueOrFallback() > 0) {
            result.put(followTimeOut.getPath(), followTimeOut.getValueOrFallback());
        }
        if (idleVelocity.getValueOrFallback() > 0) {
            result.put(idleVelocity.getPath(), idleVelocity.getValueOrFallback());
        }
        if (followVelocity.getValueOrFallback() > 0) {
            result.put(followVelocity.getPath(), followVelocity.getValueOrFallback());
        }

        return result;
    }

    @SuppressWarnings("unused") // used by ConfigurationSerialization
    public static @NotNull PathModifier deserialize(final @NotNull Map<String, Object> map) {
        final PathModifier result = new PathModifier();

        if (map.get(FOLLOW_RANGE_KEY) instanceof Number newFollowRange && newFollowRange.intValue() > 0) {
            result.followRange.setValue(newFollowRange.intValue());
        }
        if (map.get(FOLLOW_TIMEOUT_KEY) instanceof Number newFollowTimeout && newFollowTimeout.intValue() > 0) {
            result.followTimeOut.setValue(newFollowTimeout.longValue());
        }
        if (map.get(IDLE_VELOCITY_KEY) instanceof Number idleVelocity && idleVelocity.intValue() > 0) {
            result.idleVelocity.setValue(idleVelocity.doubleValue());
        }
        if (map.get(FOLLOW_VELOCITY_KEY) instanceof Number newFollowVelocity && newFollowVelocity.intValue() > 0) {
            result.followVelocity.setValue(newFollowVelocity.doubleValue());
        }

        return result;
    }

    public @Nullable Integer getOverwriteFollowRange() {
        return followRange.getValueOrFallback() > 0 ? followRange.getValueOrFallback() : null;
    }

    public @Nullable Long getOverwriteTimeout() {
        return followTimeOut.getValueOrFallback() > 0L ? followTimeOut.getValueOrFallback() : null;
    }

    public @Nullable Double getOverwriteIdleVelocity() {
        return idleVelocity.getValueOrFallback() > 0.0D ? idleVelocity.getValueOrFallback() : null;
    }

    public @Nullable Double getOverwriteFollowVelocity() {
        return followVelocity.getValueOrFallback() > 0.0D ? followVelocity.getValueOrFallback() : null;
    }
}
