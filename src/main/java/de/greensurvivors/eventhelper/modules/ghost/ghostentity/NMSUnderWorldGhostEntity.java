package de.greensurvivors.eventhelper.modules.ghost.ghostentity;

import de.greensurvivors.eventhelper.modules.ghost.GhostGame;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeMap;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.bukkit.Material;
import org.bukkit.craftbukkit.block.CraftBlockType;
import org.bukkit.craftbukkit.entity.CraftLivingEntity;
import org.bukkit.event.entity.EntityRemoveEvent;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;

public class NMSUnderWorldGhostEntity extends Monster {
    /**
     * do NOT - I repeat - do NOT call UNDERWORLD_GHOST_TYPE.create!
     * There is no way to add the important game parameter there!
     */
    public static final @NotNull EntityType<NMSUnderWorldGhostEntity> UNDERWORLD_GHOST_TYPE = registerEntityType(
        (EntityType.Builder.
            of(null, MobCategory.MISC).
            sized(0.6F, 1F). // 1 block height to fit through all gaps
                noSave(). // don't save this entity to disk.
                clientTrackingRange(10)));

    private final @NotNull GhostGame ghostGame;
    private final @NotNull NMSGhostEntity parentMob;
    private final @NotNull AttributeMap attributeMap = new AttributeMap(createAttributes().build()); // replacement since we can't change supers private one
    private volatile @Nullable CraftUnderWorldGhostEntity bukkitEntity;

    public NMSUnderWorldGhostEntity(final @NotNull NMSGhostEntity parentMob, final @NotNull GhostGame ghostGame) {
        super(UNDERWORLD_GHOST_TYPE, parentMob.level());

        setCanPickUpLoot(false);

        this.ghostGame = ghostGame;
        this.parentMob = parentMob;

        this.setPos(parentMob.position());
        this.setYRot(parentMob.getXRot());
        this.setYRot(parentMob.getYRot());

        final GroundPathNavigation groundPathNavigation = new GroundPathNavigation(this, parentMob.level());
        groundPathNavigation.setCanFloat(true); // can swim. not like floating in the air
        groundPathNavigation.setAvoidSun(false);
        navigation = groundPathNavigation;
        this.setPersistenceRequired();
    }

    @SuppressWarnings("unchecked")
    // has to be called while the server is bootstrapping, or else the registry will be frozen!
    private static <T extends Entity> @NotNull EntityType<T> registerEntityType(final @NotNull EntityType.Builder<Entity> type) {
        return (EntityType<T>) Registry.register(BuiltInRegistries.ENTITY_TYPE, "underworld_ghost",
            type.build(ResourceKey.create(Registries.ENTITY_TYPE, ResourceLocation.withDefaultNamespace("underworld_ghost"))));
    }

    public static @NotNull AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().
            add(Attributes.MAX_HEALTH, 500.0D).
            add(Attributes.MOVEMENT_SPEED, 0.30000001192092896D).
            add(Attributes.KNOCKBACK_RESISTANCE, 1.0D).add(Attributes.ATTACK_KNOCKBACK, 1.5D).
            add(Attributes.ATTACK_DAMAGE, 30.0D);
    }

    @Override
    public void tick() {
        super.tick();

        final Material material = CraftBlockType.minecraftToBukkit(getBlockStateOn().getBlock());
        double followRangeAt = ghostGame.getConfig().getFollowRangeAt(material);

        if (followRangeAt > 0) {
            getAttributes().getInstance(Attributes.FOLLOW_RANGE).setBaseValue(followRangeAt);
        }

        double followVelocityAt = ghostGame.getConfig().getFollowVelocityAt(material);
        if (followVelocityAt > 0) {
            getAttributes().getInstance(Attributes.MOVEMENT_SPEED).setBaseValue(followVelocityAt);
        }
    }

    @Override
    public @NotNull Packet<ClientGamePacketListener> getAddEntityPacket(@NotNull ServerEntity entityTrackerEntry) { // overwrite with allay type
        return new ClientboundAddEntityPacket(getId(), getUUID(),
            getX(), getY(), getZ(),
            entityTrackerEntry.getLastSentXRot(), entityTrackerEntry.getLastSentYRot(),
            EntityType.ALLAY, 0,
            entityTrackerEntry.getLastSentMovement(),
            entityTrackerEntry.getLastSentYHeadRot());
    }

    @Override
    public @NotNull CraftLivingEntity getBukkitLivingEntity() {
        return getBukkitEntity();
    }

    @SuppressWarnings("resource") // ignore level being auto closeable
    @Override
    public @NotNull CraftUnderWorldGhostEntity getBukkitEntity() {
        if (this.bukkitEntity == null) {
            synchronized (this) {
                if (this.bukkitEntity == null) {
                    return this.bukkitEntity = new CraftUnderWorldGhostEntity(this.level().getCraftServer(), this);
                }
            }
        }
        return this.bukkitEntity;
    }

    @Override
    protected @NotNull Vec3 getPassengerAttachmentPoint(final @NotNull Entity passenger, final @NotNull EntityDimensions dimensions, final float scaleFactor) {
        float walkAnimationSpeed = Math.min(0.25F, this.walkAnimation.speed());
        float walkAnimationPos = this.walkAnimation.position();
        float bumpOffset = 0.4F * Mth.cos(walkAnimationPos * 0.5F) * 2.0F * walkAnimationSpeed;

        return super.getPassengerAttachmentPoint(passenger, dimensions, scaleFactor).add(0.0D, (float) ghostGame.getConfig().getPathfindOffset() + 2.0f + bumpOffset * scaleFactor, 0.0D); // hardcoded offset to float 2 blocks above ground.
    }

    @Override
    public void aiStep() {
        super.aiStep();

        if (this.isAlive()) {
            parentMob.setPos(this.getPassengerRidingPosition(parentMob));
            parentMob.setXRot(this.getXRot());
            parentMob.setYRot(this.getYRot());
        }
    }

    @Override
    public @Nullable LivingEntity getControllingPassenger() {
        return parentMob;
    }

    @Override
    public boolean hasLineOfSight(final @NotNull Entity entity) {
        return parentMob.hasLineOfSight(entity);
    }

    @Override
    protected void defineSynchedData(@NotNull SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
    }

    @Override
    public void readAdditionalSaveData(@NotNull CompoundTag nbt) {
    }

    @Override
    public void addAdditionalSaveData(@NotNull CompoundTag nbt) {
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    public @Nullable ItemStack getPickResult() {
        return this.parentMob.getPickResult();
    }

    @Override
    public boolean hurtServer(final @NotNull ServerLevel world, final @NotNull DamageSource source, float amount) {
        return !this.isInvulnerableTo(world, source);
    }

    @Override
    public boolean is(final @NotNull Entity entity) {
        return this == entity || this.parentMob == entity;
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    public boolean isSilent() { // don't make any sound
        return true;
    }

    @Override
    public boolean hasIndirectPassenger(final @NotNull Entity passenger) {
        if (passenger == parentMob) {
            return true;
        } else if (passenger instanceof NMSGhostEntity ghostNMS) {
            NMSUnderWorldGhostEntity underWorldGhost = ghostNMS.underWorldGhost;

            if (underWorldGhost.isPassenger()) {
                Entity vehicle = underWorldGhost.getVehicle();

                return vehicle == parentMob || this.hasIndirectPassenger(vehicle);
            } else {
                return false;
            }
        } else if (!passenger.isPassenger()) {
            return false;
        } else {
            Entity vehicle = passenger.getVehicle();

            return vehicle == this || this.hasIndirectPassenger(vehicle);
        }
    }

    @Override
    protected void addPassenger(final @NotNull Entity passenger) {
        parentMob.addPassenger(passenger); // here because of protected access
    }

    @Override
    protected boolean removePassenger(final @NotNull Entity entity, final boolean suppressCancellation) {
        return parentMob.removePassenger(entity, suppressCancellation);
    }

    @Override
    public void remove(final @NotNull Entity.RemovalReason entity_removalreason, final @NotNull EntityRemoveEvent.Cause cause) {
        super.remove(entity_removalreason, cause);

        if (!parentMob.isRemoved()) {
            parentMob.remove(entity_removalreason, cause);
        }
    }
}
