package de.greensurvivors.eventhelper.modules.ghost.ghostEntity;

import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_20_R3.CraftServer;
import org.bukkit.craftbukkit.v1_20_R3.CraftWorld;
import org.bukkit.craftbukkit.v1_20_R3.entity.CraftEnemy;
import org.bukkit.craftbukkit.v1_20_R3.entity.CraftMob;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;


public class GhostCraftEntity extends CraftMob implements IGhost, CraftEnemy {
    public GhostCraftEntity(final @NotNull CraftServer server, final @NotNull GhostNMSEntity entity) {
        super(server, entity);
    }

    public static IGhost spawnNew(final @NotNull Location location,
                                  final @NotNull CreatureSpawnEvent.SpawnReason reason,
                                  final @Nullable Consumer<IGhost> function) {
        GhostNMSEntity ghostNMSEntity = GhostNMSEntity.GHOST_TYPE.create(((CraftWorld) location.getWorld()).getHandle().getMinecraftWorld());

        ghostNMSEntity.absMoveTo(location.x(), location.y(), location.z(), location.getYaw(), location.getPitch());
        ghostNMSEntity.setYHeadRot(location.getYaw());

        return ((CraftWorld) location.getWorld()).addEntity(ghostNMSEntity, reason, function, false);
    }

    @Override
    public GhostNMSEntity getHandle() {
        return (GhostNMSEntity) this.entity;
    }

    @Override
    public String toString() {
        return "CraftGhost";
    }
}
