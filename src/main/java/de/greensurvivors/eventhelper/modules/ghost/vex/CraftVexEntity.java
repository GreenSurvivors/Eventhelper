package de.greensurvivors.eventhelper.modules.ghost.vex;

import de.greensurvivors.eventhelper.modules.ghost.GhostGame;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import org.bukkit.Location;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.entity.CraftEnemy;
import org.bukkit.craftbukkit.entity.CraftMob;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

public class CraftVexEntity extends CraftMob implements IVex, CraftEnemy {
    public CraftVexEntity(final @NotNull CraftServer server, final @NotNull Mob entity) {
        super(server, entity);
    }

    public static IVex spawnNew(final @NotNull Location location,
                                final @NotNull CreatureSpawnEvent.SpawnReason reason,
                                final @NotNull GhostGame ghostGame,
                                final @Nullable Consumer<IVex> function) {

        ServerLevel serverLevel = ((CraftWorld) location.getWorld()).getHandle();
        NMSVexEntity nmsVexEntity = new NMSVexEntity(
            serverLevel.getMinecraftWorld(),
            ghostGame, location);

        nmsVexEntity.setYHeadRot(location.getYaw());

        return ((CraftWorld) location.getWorld()).addEntity(nmsVexEntity, reason, function, false);
    }

    @Override
    public @NotNull NMSVexEntity getHandle() {
        return (NMSVexEntity) this.entity;
    }

    @Override
    public @NotNull String toString() {
        return "CraftGhostVex";
    }
}
