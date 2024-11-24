package de.greensurvivors.eventhelper.modules.ghost.ghostentity;

import com.mojang.serialization.Dynamic;
import de.greensurvivors.eventhelper.EventHelper;
import de.greensurvivors.eventhelper.modules.ghost.GhostGame;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.DebugPackets;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.bukkit.Material;
import org.bukkit.craftbukkit.block.CraftBlockType;
import org.bukkit.craftbukkit.entity.CraftLivingEntity;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityRemoveEvent;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;

// wanders around looking for player if no target. can target through walls but must path find there
public class NMSGhostEntity extends Monster implements Enemy {
    /**
     * do NOT - I repeat - do NOT call GHOST_TYPE.create!
     * There is no way to add the important game parameter there!
     */
    public static final EntityType<NMSGhostEntity> GHOST_TYPE = registerEntityType(
        (EntityType.Builder.
            of(null, MobCategory.MONSTER).
            sized(6.0F, 6.0F).
            passengerAttachments(4.0625F).
            noSave(). // don't save this entity to disk.
                clientTrackingRange(10)));
    protected final @NotNull GhostGame ghostGame;
    private volatile @Nullable CraftGhostEntity bukkitEntity;
    protected NMSUnderWorldGhostEntity underWorldGhost;

    @SuppressWarnings("unchecked")
    // has to be called while the server is bootstrapping, or else the registry will be frozen!
    private static <T extends Entity> @NotNull EntityType<T> registerEntityType(final @NotNull EntityType.Builder<Entity> type) {
        return (EntityType<T>) Registry.register(BuiltInRegistries.ENTITY_TYPE, "ghost",
            type.build(ResourceKey.create(Registries.ENTITY_TYPE, ResourceLocation.withDefaultNamespace("ghost"))));
    }

    public NMSGhostEntity(final @NotNull Level world, final @NotNull GhostGame ghostGame) {
        super(GHOST_TYPE, world);

        this.ghostGame = ghostGame;
        this.setPersistenceRequired();
    }

    @Override
    protected boolean shouldDespawnInPeaceful() {
        return false;
    }

    public static @NotNull AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().
            add(Attributes.MAX_HEALTH, 500.0D).
            add(Attributes.MOVEMENT_SPEED, 0.30000001192092896D).
            add(Attributes.KNOCKBACK_RESISTANCE, 1.0D).add(Attributes.ATTACK_KNOCKBACK, 1.5D).
            add(Attributes.ATTACK_DAMAGE, 30.0D);
    }

    @Override
    public @NotNull Packet<ClientGamePacketListener> getAddEntityPacket(@NotNull ServerEntity entityTrackerEntry) { // overwrite with allay type
        return new ClientboundAddEntityPacket(
            getId(), getUUID(),
            getX(), getY(), getZ(),
            entityTrackerEntry.getLastSentXRot(), entityTrackerEntry.getLastSentYRot(),
            EntityType.GHAST, 0,
            entityTrackerEntry.getLastSentMovement(),
            entityTrackerEntry.getLastSentYHeadRot());
    }

    @Override
    protected @NotNull Brain<?> makeBrain(@NotNull Dynamic<?> dynamic) {
        return GhostAI.makeBrain(this, this.brainProvider().makeBrain(dynamic));
    }

    @Override
    @SuppressWarnings("unchecked") // don't worry. Over overridden #makeBrain() does ensure it has the right type.
    public @NotNull Brain<NMSGhostEntity> getBrain() {
        return (Brain<NMSGhostEntity>) super.getBrain();
    }

    @Override
    protected @NotNull Brain.Provider<NMSGhostEntity> brainProvider() {
        return Brain.provider(GhostAI.MEMORY_TYPES, GhostAI.SENSOR_TYPES);
    }

    @Override
    public @NotNull CraftLivingEntity getBukkitLivingEntity() {
        return getBukkitEntity();
    }

    @Override
    @SuppressWarnings("resource") // ignore level being auto closeable
    public @NotNull CraftGhostEntity getBukkitEntity() {
        if (this.bukkitEntity == null) {
            synchronized (this) {
                if (this.bukkitEntity == null) {
                    return this.bukkitEntity = new CraftGhostEntity(this.level().getCraftServer(), this);
                }
            }
        }
        //noinspection DataFlowIssue
        return this.bukkitEntity;
    }

    public @NotNull PathNavigation getNavigation() {
        if (underWorldGhost == null) {
            return super.getNavigation();
        } else {
            return underWorldGhost.getNavigation();
        }
    }

    @Override
    @SuppressWarnings("resource") // ignore level being auto closeable
    public @Nullable SpawnGroupData finalizeSpawn(final @NotNull ServerLevelAccessor world,
                                                  final @NotNull DifficultyInstance difficulty,
                                                  final @NotNull EntitySpawnReason spawnReason,
                                                  final @Nullable SpawnGroupData entityData) {
        this.underWorldGhost = new NMSUnderWorldGhostEntity(this, ghostGame);

        level().addFreshEntity(underWorldGhost, CreatureSpawnEvent.SpawnReason.CUSTOM);

        return entityData;
    }

    @Override
    public void remove(final @NotNull Entity.RemovalReason entity_removalreason, final @NotNull EntityRemoveEvent.Cause cause) {
        super.remove(entity_removalreason, cause);

        if (!underWorldGhost.isRemoved()) {
            underWorldGhost.remove(entity_removalreason, cause);
        }
    }

    @Override
    public void tick() {
        super.tick();

        this.oRun = this.run;
        this.run = 0.0F;
        this.resetFallDistance();
        this.setDeltaMovement(Vec3.ZERO);
    }

    @Override
    protected void customServerAiStep(final @NotNull ServerLevel world) {
        ProfilerFiller gameprofilerfiller = Profiler.get();

        gameprofilerfiller.push("ghostBrain");
        this.getBrain().tick(world, this);
        GhostAI.updateActivity(this);
        gameprofilerfiller.pop();

        super.customServerAiStep(world);
    }

    @SuppressWarnings("resource") // ignore level being auto closeable
    @Override
    public void playAmbientSound() {
        this.level().playLocalSound(this, this.getAmbientSound(), this.getSoundSource(), 1.0F, 1.0F);
    }

    protected void playAngrySound() {
        this.playSound(SoundEvents.WARDEN_ANGRY, 1.0F, this.getVoicePitch());
    }

    @Override
    public @NotNull SoundSource getSoundSource() {
        return SoundSource.HOSTILE;
    }

    @Override
    public @NotNull SoundEvent getDeathSound() {
        return SoundEvents.WARDEN_DEATH;
    }

    @Override
    protected @NotNull SoundEvent getHurtSound(@NotNull DamageSource source) {
        return SoundEvents.GHAST_HURT;
    }

    @Override
    protected @NotNull SoundEvent getAmbientSound() {
        return SoundEvents.WARDEN_AMBIENT;
    }

    @Override
    protected void sendDebugPackets() {
        super.sendDebugPackets();
        DebugPackets.sendEntityBrain(this);
    }

    @Override
    public boolean canAttackType(final @NotNull EntityType<?> type) {
        return type == EntityType.PLAYER;
    }

    @Override
    public int getMaxHeadXRot() { // don't turn your head if you are all head
        return 1;
    }

    @Override
    public int getMaxHeadYRot() { // don't turn your head if you are all head
        return 1;
    }

    @Override
    public boolean isInvulnerableTo(final @NotNull ServerLevel world, final @NotNull DamageSource damageSource) { // is invulnerable to everything
        return !damageSource.is(DamageTypeTags.BYPASSES_INVULNERABILITY);
    }

    @Override
    public double getFluidJumpThreshold() {
        return this.getEyeHeight();
    }

    @Override
    public boolean causeFallDamage(final float fallDistance, final float damageMultiplier, final @NotNull DamageSource damageSource) {
        if (fallDistance > 3.0F) {
            this.playSound(this.getFallSounds().big(), 1.0F, 1.0F);
            this.playBlockFallSound();
        } else {
            this.playSound(this.getFallSounds().small(), 1.0F, 1.0F);
        }

        return false; // immune to fall damage
    }

    @Override
    public float getWalkTargetValue(@NotNull BlockPos pos, @NotNull LevelReader world) {
        return 0.0F;
    }

    @Override
    public boolean canUsePortal(boolean allowVehicles) {
        return false;
    }

    @Override
    public @NotNull Entity getRootVehicle() {
        if (underWorldGhost == null) {
            return this;
        } else {
            @NotNull Entity entity = underWorldGhost;
            while (entity.isPassenger()) {
                entity = entity.getVehicle();
            }

            return entity;
        }
    }

    @Override
    protected void addPassenger(final @NotNull Entity passenger) {
        super.addPassenger(passenger); // here for protected access
    }

    @Override
    protected boolean removePassenger(final @NotNull Entity entity, final boolean suppressCancellation) {
        return super.removePassenger(entity, suppressCancellation); // here for protected access
    }

    @Override
    public boolean isPassenger() {
        if (underWorldGhost == null) {
            return false;
        }

        return underWorldGhost.isPassenger();
    }

    @Override
    public @Nullable Entity getVehicle() {
        if (underWorldGhost == null) {
            return null;
        }

        return underWorldGhost.getVehicle();
    }

    @Override
    protected @NotNull AABB getHitbox() {
        if (underWorldGhost == null) {
            return this.getBoundingBox();
        }

        AABB axisalignedbb = this.getBoundingBox();
        return axisalignedbb.setMinY(Math.max(underWorldGhost.getPassengerRidingPosition(this).y, axisalignedbb.minY));
    }

    @Override
    public boolean startRiding(final @NotNull Entity entity, final boolean force) {
        if (underWorldGhost == null) {
            return false;
        }

        return underWorldGhost.startRiding(entity, force);
    }

    @Override
    public void stopRiding(final boolean suppressCancellation) {
        if (underWorldGhost != null) {
            underWorldGhost.stopRiding(suppressCancellation);
        }
    }

    @SuppressWarnings("resource") // ignore level being auto closeable
    public boolean canTargetEntity(final @NotNull LivingEntity entity) {
        return this.level() == entity.level() &&
            EntitySelector.NO_CREATIVE_OR_SPECTATOR.test(entity) &&
            !this.isAlliedTo(entity) &&
            entity.getType() != EntityType.ARMOR_STAND &&
            entity.getType() != GHOST_TYPE &&
            !entity.isInvulnerable() && !entity.isDeadOrDying() &&
            this.level().getWorldBorder().isWithinBounds(entity.getBoundingBox());
    }

    public double getIdleVelocity() {
        if (underWorldGhost == null) {
            return 0.3D;
        }

        final Material material = CraftBlockType.minecraftToBukkit(underWorldGhost.getBlockStateOn().getBlock());
        double idleVelocityAt = ghostGame.getConfig().getIdleVelocityAt(material);

        if (idleVelocityAt <= 0) {
            return 0.3D;
        } else {
            return idleVelocityAt;
        }
    }

    public int getFollowRangeAt() {
        return (int) underWorldGhost.getAttributeValue(Attributes.FOLLOW_RANGE);
    }

    public double getFollowVelocityAt() {
        final Material material = CraftBlockType.minecraftToBukkit(underWorldGhost.getBlockStateOn().getBlock());

        double followVelocityAt = ghostGame.getConfig().getFollowVelocityAt(material);
        if (followVelocityAt <= 0) {
            followVelocityAt = 0.39284D;
        }

        EventHelper.getPlugin().getComponentLogger().info("requested FollowVelocity at " + underWorldGhost.getOnPos() + ", mat: " + material + " velocity: " + followVelocityAt);

        return followVelocityAt;
    }
}
