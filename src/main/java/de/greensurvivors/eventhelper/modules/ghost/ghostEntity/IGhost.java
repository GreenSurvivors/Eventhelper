package de.greensurvivors.eventhelper.modules.ghost.ghostEntity;

import org.bukkit.Location;
import org.bukkit.entity.Enemy;
import org.bukkit.entity.Flying;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

public interface IGhost extends Flying, Enemy {
    static IGhost spawnNew(final @NotNull Location location,
                           final @NotNull CreatureSpawnEvent.SpawnReason reason,
                           final @Nullable Consumer<IGhost> function) {
        return GhostCraftEntity.spawnNew(location, reason, function);
    }
}
