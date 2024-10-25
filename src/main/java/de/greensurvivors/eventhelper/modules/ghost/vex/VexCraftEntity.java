package de.greensurvivors.eventhelper.modules.ghost.vex;

import de.greensurvivors.eventhelper.modules.ghost.GhostGame;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_20_R3.CraftServer;
import org.bukkit.craftbukkit.v1_20_R3.CraftWorld;
import org.bukkit.craftbukkit.v1_20_R3.entity.CraftEnemy;
import org.bukkit.craftbukkit.v1_20_R3.entity.CraftEntity;
import org.bukkit.craftbukkit.v1_20_R3.entity.CraftMob;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
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

    @Override
    public int getAnger() {
        return this.getHandle().getAngerManagement().getActiveAnger(this.getHandle().getTarget());
    }

    @Override
    public int getAnger(@NotNull Entity entity) {
        return this.getHandle().getAngerManagement().getActiveAnger(((CraftEntity) entity).getHandle());
    }

    // Paper start
    @Override
    public int getHighestAnger() {
        return this.getHandle().getAngerManagement().getActiveAnger(null);
    }
    // Paper end

    @Override
    public void increaseAnger(@NotNull Entity entity, int increase) {
        this.getHandle().getAngerManagement().increaseAnger(((CraftEntity) entity).getHandle(), increase);
    }

    @Override
    public void setAnger(@NotNull Entity entity, int anger) {
        this.getHandle().clearAnger(((CraftEntity) entity).getHandle());
        this.getHandle().getAngerManagement().increaseAnger(((CraftEntity) entity).getHandle(), anger);
    }

    @Override
    public void clearAnger(@NotNull Entity entity) {
        this.getHandle().clearAnger(((CraftEntity) entity).getHandle());
    }

    @Override
    public LivingEntity getEntityAngryAt() {
        return (LivingEntity) this.getHandle().getEntityAngryAt().map(net.minecraft.world.entity.Entity::getBukkitEntity).orElse(null);
    }

    @Override
    public void setDisturbanceLocation(@NotNull Location location) {
        VexAI.setDisturbanceLocation(this.getHandle(), BlockPos.containing(location.getX(), location.getY(), location.getZ()));
    }

    @Override
    public @NotNull AngerLevel getAngerLevel() {
        return switch (this.getHandle().getAngerLevel()) {
            case CALM -> AngerLevel.CALM;
            case AGITATED -> AngerLevel.AGITATED;
            case ANGRY -> AngerLevel.ANGRY;
        };
    }
}
