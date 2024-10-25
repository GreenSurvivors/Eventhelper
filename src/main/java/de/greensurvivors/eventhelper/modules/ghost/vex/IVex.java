package de.greensurvivors.eventhelper.modules.ghost.vex;

import de.greensurvivors.eventhelper.modules.ghost.GhostGame;
import org.bukkit.Location;
import org.bukkit.entity.Enemy;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Flying;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

public interface IVex extends Flying, Enemy {
    static IVex spawnNew(final @NotNull Location location,
                         final @NotNull CreatureSpawnEvent.SpawnReason reason,
                         final @NotNull GhostGame ghostGame,
                         final @Nullable Consumer<IVex> function) {
        return CraftVexEntity.spawnNew(location, reason, ghostGame, function);
    }

    /**
     * Gets the anger level of this warden.
     * <p>
     * Anger is an integer from 0 to 150. Once a Warden reaches 80 anger at a
     * target it will actively pursue it.
     *
     * @return anger level
     */
    int getAnger();

    /**
     * Gets the anger level of this warden.
     * <p>
     * Anger is an integer from 0 to 150. Once a Warden reaches 80 anger at a
     * target it will actively pursue it.
     *
     * @param entity target entity
     * @return anger level
     */
    int getAnger(@NotNull Entity entity);

    /**
     * Gets the highest anger level of this warden.
     * <p>
     * Anger is an integer from 0 to 150. Once a Warden reaches 80 anger at a
     * target it will actively pursue it.
     *
     * @return highest anger level
     */
    int getHighestAnger();

    /**
     * Increases the anger level of this warden.
     * <p>
     * Anger is an integer from 0 to 150. Once a Warden reaches 80 anger at a
     * target it will actively pursue it.
     *
     * @param entity   target entity
     * @param increase number to increase by
     * @see #getAnger(org.bukkit.entity.Entity)
     */
    void increaseAnger(@NotNull Entity entity, int increase);

    /**
     * Sets the anger level of this warden.
     * <p>
     * Anger is an integer from 0 to 150. Once a Warden reaches 80 anger at a
     * target it will actively pursue it.
     *
     * @param entity target entity
     * @param anger  new anger level
     * @see #getAnger(org.bukkit.entity.Entity)
     */
    void setAnger(@NotNull Entity entity, int anger);

    /**
     * Clears the anger level of this warden.
     *
     * @param entity target entity
     */
    void clearAnger(@NotNull Entity entity);

    /**
     * Gets the {@link LivingEntity} at which this warden is most angry.
     *
     * @return The target {@link LivingEntity} or null
     */
    @Nullable LivingEntity getEntityAngryAt();

    /**
     * Make the warden sense a disturbance in the force at the location given.
     *
     * @param location location of the disturbance
     */
    void setDisturbanceLocation(@NotNull Location location);

    /**
     * Get the level of anger of this warden.
     *
     * @return The level of anger
     */
    @NotNull AngerLevel getAngerLevel();

    public enum AngerLevel {
        /**
         * Anger level 0-39.
         */
        CALM,
        /**
         * Anger level 40-79.
         */
        AGITATED,
        /**
         * Anger level 80 or above.
         */
        ANGRY;
    }
}
