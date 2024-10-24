package de.greensurvivors.eventhelper.modules.ghost.vex;

import de.greensurvivors.eventhelper.modules.ghost.GhostGame;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_20_R3.CraftServer;
import org.bukkit.craftbukkit.v1_20_R3.CraftWorld;
import org.bukkit.craftbukkit.v1_20_R3.entity.CraftEnemy;
import org.bukkit.craftbukkit.v1_20_R3.entity.CraftMob;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

public class VexCraftEntity extends CraftMob implements IVex, CraftEnemy {
    public VexCraftEntity(final @NotNull CraftServer server, final @NotNull Mob entity) {
        super(server, entity);
    }

    public static IVex spawnNew(final @NotNull Location location,
                                final @NotNull CreatureSpawnEvent.SpawnReason reason,
                                final @NotNull GhostGame ghostGame,
                                final @Nullable Consumer<IVex> function) {

        ServerLevel serverLevel = ((CraftWorld) location.getWorld()).getHandle();
        VexNMSEntity nmsVexEntity = new VexNMSEntity(
            serverLevel.getMinecraftWorld(),
            ghostGame);

        nmsVexEntity.absMoveTo(location.x(), location.y(), location.z(), location.getYaw(), location.getPitch());
        nmsVexEntity.setYHeadRot(location.getYaw());

        return ((CraftWorld) location.getWorld()).addEntity(nmsVexEntity, reason, function, false);
    }

    @Override
    public @NotNull VexNMSEntity getHandle() {
        return (VexNMSEntity) this.entity;
    }

    @Override
    public @NotNull String toString() {
        return "CraftGhostVex";
    }
}
