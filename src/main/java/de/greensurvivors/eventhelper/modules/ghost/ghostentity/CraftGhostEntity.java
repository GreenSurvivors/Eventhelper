package de.greensurvivors.eventhelper.modules.ghost.ghostentity;

import de.greensurvivors.eventhelper.modules.ghost.GhostGame;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.MobSpawnType;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_20_R3.CraftServer;
import org.bukkit.craftbukkit.v1_20_R3.CraftWorld;
import org.bukkit.craftbukkit.v1_20_R3.entity.CraftEnemy;
import org.bukkit.craftbukkit.v1_20_R3.entity.CraftMob;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;


public class CraftGhostEntity extends CraftMob implements IGhost, CraftEnemy {
    public CraftGhostEntity(final @NotNull CraftServer server, final @NotNull NMSGhostEntity entity) {
        super(server, entity);
    }

    public static IGhost spawnNew(final @NotNull Location location,
                                  final @NotNull CreatureSpawnEvent.SpawnReason reason,
                                  final @NotNull GhostGame ghostGame,
                                  final @Nullable Consumer<IGhost> function) {
        ServerLevel serverLevel = ((CraftWorld) location.getWorld()).getHandle();

        NMSGhostEntity ghostNMSEntity = new NMSGhostEntity(
            serverLevel.getMinecraftWorld(),
            ghostGame);

        // first set position, then finalize to spawn underworld counterpart.
        // This is because we set the position of the counterpart to the position of the gost, then update every ai tick
        // would we spawn the entity at the same time as the ghost and then set the position it wouldn't have any effect
        ghostNMSEntity.absMoveTo(location.x(), location.y(), location.z(), location.getYaw(), location.getPitch());
        ghostNMSEntity.setYHeadRot(location.getYaw());
        ghostNMSEntity.finalizeSpawn(serverLevel.getMinecraftWorld(), serverLevel.getMinecraftWorld().getCurrentDifficultyAt(ghostNMSEntity.blockPosition()), MobSpawnType.COMMAND, null, null);

        return ((CraftWorld) location.getWorld()).addEntity(ghostNMSEntity, reason, function, false);
    }

    @Override
    public @NotNull NMSGhostEntity getHandle() {
        return (NMSGhostEntity) this.entity;
    }

    @Override
    public @NotNull String toString() {
        return "CraftGhost";
    }
}
