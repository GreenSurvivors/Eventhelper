package de.greensurvivors.eventhelper.modules.ghost;

import de.greensurvivors.eventhelper.config.ConfigOption;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * The ghost may move with a different speed, when following or being idle as well as having a different tracking range,
 * depending on the block the underworld ghost is walking on
 */
public class PathModifier implements ConfigurationSerializable {
    private final static String
        FOLLOW_RANGE_KEY = "follow.range",
        IDLE_VELOCITY_KEY = "velocity.idle",
        FOLLOW_VELOCITY_KEY = "velocity.follow";
    private final @NotNull ConfigOption<@Nullable Integer> followRange = new ConfigOption<>(FOLLOW_RANGE_KEY, -1); // in blocks
    private final @NotNull ConfigOption<@Nullable Double> idleVelocity = new ConfigOption<>(IDLE_VELOCITY_KEY, -1.0D);  // in blocks / s ?
    private final @NotNull ConfigOption<@Nullable Double> followVelocity = new ConfigOption<>(FOLLOW_VELOCITY_KEY, -1.0D); // in blocks / s ?

    public PathModifier() {
    }

    @SuppressWarnings("unused") // used by ConfigurationSerialization
    public static @NotNull PathModifier deserialize(final @NotNull Map<String, Object> map) {
        final PathModifier result = new PathModifier();

        if (map.get(FOLLOW_RANGE_KEY) instanceof Number newFollowRange && newFollowRange.intValue() > 0) {
            result.followRange.setValue(newFollowRange.intValue());
        }
        if (map.get(IDLE_VELOCITY_KEY) instanceof Number idleVelocity && idleVelocity.doubleValue() > 0) {
            result.idleVelocity.setValue(idleVelocity.doubleValue());
        }
        if (map.get(FOLLOW_VELOCITY_KEY) instanceof Number newFollowVelocity && newFollowVelocity.doubleValue() > 0) {
            result.followVelocity.setValue(newFollowVelocity.doubleValue());
        }

        return result;
    }

    @Override
    public @NotNull Map<String, Object> serialize() {
        final @NotNull Map<String, Object> result = new HashMap<>(5);

        result.put(followRange.getPath(), followRange.getValueOrFallback());
        result.put(idleVelocity.getPath(), idleVelocity.getValueOrFallback());
        result.put(followVelocity.getPath(), followVelocity.getValueOrFallback());

        return result;
    }

    public @Nullable Integer getOverwriteFollowRange() {
        return followRange.getValueOrFallback() > 0 ? followRange.getValueOrFallback() : null;
    }

    public @Nullable Double getOverwriteIdleVelocity() {
        return idleVelocity.getValueOrFallback() > 0.0D ? idleVelocity.getValueOrFallback() : null;
    }

    public @Nullable Double getOverwriteFollowVelocity() {
        return followVelocity.getValueOrFallback() > 0.0D ? followVelocity.getValueOrFallback() : null;
    }

    public void setOverwriteFollowRange(@Nullable Integer followRange) {
        this.followRange.setValue(followRange == null ? -1 : followRange);
    }

    public void setOverwriteIdleVelocity(@Nullable Double idleVelocity) {
        this.idleVelocity.setValue(idleVelocity == null ? -1.0D : idleVelocity);
    }

    public void setOverwriteFollowVelocity(@Nullable Double followVelocity) {
        this.followVelocity.setValue(followVelocity == null ? -1.0D : followVelocity);
    }

    @Override
    public @NotNull String toString() {
        return "PathModifier [" +
            followRange.getPath() + ":" + followRange.getValueOrFallback() + ", " +
            idleVelocity.getPath() + ":" + idleVelocity.getValueOrFallback() + ", " +
            followVelocity.getPath() + ":" + followVelocity.getValueOrFallback() +
            "]";
    }
}
