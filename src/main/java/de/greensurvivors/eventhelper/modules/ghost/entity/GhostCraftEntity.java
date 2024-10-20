package de.greensurvivors.eventhelper.modules.ghost.entity;

import de.greensurvivors.eventhelper.modules.ghost.GhostGame;
import net.minecraft.server.level.ServerLevel;
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
                                  final @NotNull GhostGame ghostGame,
                                  final @Nullable Consumer<IGhost> function) {
        ServerLevel serverLevel = ((CraftWorld) location.getWorld()).getHandle();

        GhostNMSEntity ghostNMSEntity = new GhostNMSEntity(
            serverLevel.getMinecraftWorld(),
            ghostGame);

        UnderWorldGhostNMSEntity underWorldGhostNMSEntity = new UnderWorldGhostNMSEntity(ghostNMSEntity, ghostGame);
        ghostNMSEntity.setUnderworldGhost(underWorldGhostNMSEntity);

        ghostNMSEntity.absMoveTo(location.x(), location.y(), location.z(), location.getYaw(), location.getPitch());
        ghostNMSEntity.setYHeadRot(location.getYaw());
        underWorldGhostNMSEntity.absMoveTo(location.x(), location.y(), location.z(), location.getYaw(), location.getPitch());
        underWorldGhostNMSEntity.setYHeadRot(location.getYaw());

        ghostNMSEntity.startRiding(underWorldGhostNMSEntity, true);

        ((CraftWorld) location.getWorld()).addEntity(underWorldGhostNMSEntity, reason, null, false);
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
