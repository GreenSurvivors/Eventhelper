package de.greensurvivors.eventhelper.modules.ghost.ghostEntity;

import com.mojang.serialization.Dynamic;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.protocol.game.DebugPackets;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;

public class GhostNMSEntity extends Monster implements Enemy {
    private static final EntityDataAccessor<Boolean> DATA_IS_CHARGING = SynchedEntityData.defineId(GhostNMSEntity.class, EntityDataSerializers.BOOLEAN);

    @SuppressWarnings("unchecked")
    protected static final EntityType<GhostNMSEntity> GHOST_TYPE =
        Registry.register(BuiltInRegistries.ENTITY_TYPE, "ghast", // register as ghast to display a ghast to the client
            ((EntityType<GhostNMSEntity>) (EntityType<? extends GhostNMSEntity>) EntityType.Builder.
                of(GhostNMSEntity::new, MobCategory.MONSTER).
                sized(4.0F, 4.0F).
                noSave(). // don't save this entity to disk.
                    clientTrackingRange(10).
                build("ghost")));

    private volatile GhostCraftEntity bukkitEntity;

    public GhostNMSEntity(EntityType<? extends GhostNMSEntity> type, Level world) {
        super(type, world);

        navigation.setCanFloat(true);
    }

    @Override
    public @NotNull GhostCraftEntity getBukkitEntity() {
        if (this.bukkitEntity == null) {
            synchronized (this) {
                if (this.bukkitEntity == null) {
                    return this.bukkitEntity = new GhostCraftEntity(this.level().getCraftServer(), this);
                }
            }
        }
        return this.bukkitEntity;
    }

    @Override
    protected @NotNull Brain<?> makeBrain(@NotNull Dynamic<?> dynamic) {
        return GhostAi.makeBrain(this.brainProvider().makeBrain(dynamic));
    }

    @Override
    @SuppressWarnings("unchecked") // don't worry. Over overridden #makeBrain() does ensure it has the right type.
    public @NotNull Brain<GhostNMSEntity> getBrain() {
        return (Brain<GhostNMSEntity>) super.getBrain();
    }

    @Override
    protected @NotNull Brain.Provider<GhostNMSEntity> brainProvider() {
        return Brain.provider(GhostAi.MEMORY_TYPES, GhostAi.SENSOR_TYPES);
    }

    public boolean isCharging() {
        return (Boolean) this.entityData.get(DATA_IS_CHARGING);
    }

    public void setCharging(boolean shooting) {
        this.entityData.set(DATA_IS_CHARGING, shooting);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_IS_CHARGING, false);
    }

    @Override
    public void onSyncedDataUpdated(@NotNull EntityDataAccessor<?> data) {
        // todo

        super.onSyncedDataUpdated(data);
    }

    @Override
    public void tick() {
        // todo
        super.tick();
    }

    @Override
    public void playAmbientSound() {
        this.level().playLocalSound(this, this.getAmbientSound(), this.getSoundSource(), 1.0F, 1.0F);
    }

    @Override
    public @NotNull SoundSource getSoundSource() {
        return SoundSource.HOSTILE;
    }

    @Override
    public SoundEvent getDeathSound() {
        return SoundEvents.GHAST_DEATH;
    }

    @Override
    protected SoundEvent getHurtSound(@NotNull DamageSource source) {
        return SoundEvents.GHAST_HURT;
    }

    @Override
    protected @NotNull SoundEvent getAmbientSound() {
        return SoundEvents.GHAST_AMBIENT;
    }

    @Override
    protected void sendDebugPackets() {
        super.sendDebugPackets();
        DebugPackets.sendEntityBrain(this);
    }

    @Override
    public boolean canAttackType(@NotNull EntityType<?> type) {
        return type == EntityType.PLAYER;
    }

    @Override
    public int getMaxHeadYRot() {
        return 30;
    }

    @Override
    public int getHeadRotSpeed() {
        return 25;
    }

    public double getSnoutYPosition() {
        return this.getEyeY() - 0.4;
    }

    @Override
    public boolean isInvulnerableTo(@NotNull DamageSource damageSource) { // is invulnerable to everything <-- todo don't brick killing it via plugin
        return !damageSource.is(DamageTypeTags.BYPASSES_INVULNERABILITY);
    }

    @Override
    public double getFluidJumpThreshold() {
        return this.getEyeHeight();
    }

    @Override
    public boolean causeFallDamage(float fallDistance, float damageMultiplier, @NotNull DamageSource damageSource) {
        if (fallDistance > 3.0F) {
            this.playSound(SoundEvents.ELDER_GUARDIAN_AMBIENT_LAND, 1.0F, 1.0F);
        }

        return false; // immune to fall damage
    }

    @Override
    protected @NotNull Vector3f getPassengerAttachmentPoint(@NotNull Entity passenger, @NotNull EntityDimensions dimensions, float scaleFactor) {
        return new Vector3f(0.0F, dimensions.height + 0.0625F * scaleFactor, 0.0F);
    }

    @Override
    protected float ridingOffset(@NotNull Entity vehicle) {
        return 0.5F;
    }
}
