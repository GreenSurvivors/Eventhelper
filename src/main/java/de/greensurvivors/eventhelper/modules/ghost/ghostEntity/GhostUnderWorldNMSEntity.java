package de.greensurvivors.eventhelper.modules.ghost.ghostEntity;

import de.greensurvivors.eventhelper.modules.ghost.GhostGame;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;

public class GhostUnderWorldNMSEntity extends Entity { // todo register properly!
    private final @NotNull GhostGame ghostGame;
    private final @NotNull GhostNMSEntity parentMob;
    private final EntityDimensions size;

    public GhostUnderWorldNMSEntity(final @NotNull GhostGame ghostGame, @NotNull GhostNMSEntity parentMob, float width, float height) {
        super(parentMob.getType(), parentMob.level());

        this.size = EntityDimensions.scalable(width, height);
        this.ghostGame = ghostGame;
        this.parentMob = parentMob;
    }

    @Override
    protected void defineSynchedData() {
    }

    @Override
    protected void readAdditionalSaveData(@NotNull CompoundTag nbt) {
    }

    @Override
    protected void addAdditionalSaveData(@NotNull CompoundTag nbt) {
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    @Nullable
    @Override
    public ItemStack getPickResult() {
        return this.parentMob.getPickResult();
    }

    @Override
    public boolean hurt(@NotNull DamageSource source, float amount) {
        return !this.isInvulnerableTo(source);
    }

    @Override
    public boolean is(@NotNull Entity entity) {
        return this == entity || this.parentMob == entity;
    }

    @Override
    public @NotNull Packet<ClientGamePacketListener> getAddEntityPacket() {
        throw new UnsupportedOperationException();
    }

    @Override
    public @NotNull EntityDimensions getDimensions(Pose pose) {
        return this.size;
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }
}
