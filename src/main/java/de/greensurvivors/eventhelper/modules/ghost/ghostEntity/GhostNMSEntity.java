package de.greensurvivors.eventhelper.modules.ghost.ghostEntity;

import com.mojang.serialization.Dynamic;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.DebugPackets;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import org.bukkit.craftbukkit.v1_20_R3.attribute.CraftAttributeMap;
import org.bukkit.craftbukkit.v1_20_R3.entity.CraftLivingEntity;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;

import javax.annotation.Nullable;
import java.lang.reflect.Field;

public class GhostNMSEntity extends Monster implements Enemy {
    public static final EntityType<GhostNMSEntity> GHOST_TYPE = registerEntityType(
        (EntityType.Builder.
            of(GhostNMSEntity::new, MobCategory.MONSTER).
            // sometimes you aren't aware of how big you are. And sometimes that for the better.
            // At least that's what I will to continue to tell myself, while avoiding every mirror
                sized(0.6F, 1.95F). // size of zombie, this just makes pathfinding easier
            //sized(4.0F, 4.0F). // size of ghast (alternative), hard for pathfinding
            noSave(). // don't save this entity to disk.
                clientTrackingRange(10)));
    private static final EntityDataAccessor<Boolean> DATA_IS_CHARGING = SynchedEntityData.defineId(GhostNMSEntity.class, EntityDataSerializers.BOOLEAN);
    private volatile GhostCraftEntity bukkitEntity;

    @SuppressWarnings("unchecked")
    // has to be called while the server is bootstrapping, or else the registry will be frozen!
    private static <T extends Entity> EntityType<T> registerEntityType(EntityType.Builder<Entity> type) {
        return (EntityType<T>) Registry.register(BuiltInRegistries.ENTITY_TYPE, "ghast", // register as ghast to display a ghast to the client
            type.build("ghost"));
    }

    public GhostNMSEntity(EntityType<? extends GhostNMSEntity> type, Level world) {
        super(type, world);

        navigation.setCanFloat(true); // can swim. not like floating in the air
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().
            add(Attributes.MAX_HEALTH, 500.0D).
            add(Attributes.MOVEMENT_SPEED, 0.30000001192092896D).
            add(Attributes.KNOCKBACK_RESISTANCE, 1.0D).add(Attributes.ATTACK_KNOCKBACK, 1.5D).
            add(Attributes.ATTACK_DAMAGE, 30.0D);
    }

    /*
    since the DefaultAttributes class builds the immutable map directly
    and the LivingEntity super constructor calls this method before we can set the attribute map correctly ourselves,
    we have to get creative.
    First we try to just let super fetch the attribute.
    If this fails, as the fist time called from the constructor will do,
    we set the private field ("attributes") of our super class LivingEntity with our defaults.
    While the craftAttributes don't get immediately get accessed in the super constructor,
    they don't get accessed via method. Since we use reflection to set the non craft field anyway,
     we may set the craft field at the same time as well.
    After that we try again calling the super method. fingers crossed it worked!

    This have to be done this way, since the super constructor is not done and therefor we can't access the fields of this class yet.
    We can't just relay to our own variable, nor can we just set the super one after the constructor.
    Also, since the field is private we have to use reflection to set it to a new value!
    If you have an idea how to solve this any mess better, please tell me!
    */
    public @Nullable AttributeInstance getAttribute(@NotNull Attribute attribute) {
        try {
            return super.getAttribute(attribute);
        } catch (NullPointerException ignored) {
            Class<?> livingEntityClass = LivingEntity.class;

            try {
                // Access the private field
                Field privateAttributesField = livingEntityClass.getDeclaredField("attributes");

                // Make the field accessible
                privateAttributesField.setAccessible(true);

                // Set the field value
                AttributeMap attributeMap = new AttributeMap(createAttributes().build());
                privateAttributesField.set(this, attributeMap);

                // same below
                Field finalCraftAttributesField = livingEntityClass.getDeclaredField("craftAttributes");

                finalCraftAttributesField.setAccessible(true);

                finalCraftAttributesField.set(this, new CraftAttributeMap(attributeMap));

            } catch (NoSuchFieldException |
                     IllegalAccessException e) { // should never happen since we set the field accessible
                throw new RuntimeException(e);
            }
        }

        return super.getAttribute(attribute);
    }

    @Override
    public @NotNull Packet<ClientGamePacketListener> getAddEntityPacket() { // overwrite with ghast type
        return new ClientboundAddEntityPacket(getId(), getUUID(), getX(), getY(), getZ(), getXRot(), getYRot(), EntityType.GHAST, 0, getDeltaMovement(), getYHeadRot());
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

    @Override
    public @NotNull CraftLivingEntity getBukkitLivingEntity() {
        return getBukkitEntity();
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

    public boolean isCharging() {
        return this.entityData.get(DATA_IS_CHARGING);
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
    protected void customServerAiStep() {
        ServerLevel worldserver = (ServerLevel) this.level();

        worldserver.getProfiler().push("ghostBrain");
        this.getBrain().tick(worldserver, this);
        this.level().getProfiler().pop();
        super.customServerAiStep();

        //GhostAi.updateActivity(this);
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
    public int getMaxHeadXRot() { // don't turn your head if you are all head
        return 1;
    }

    @Override
    public int getMaxHeadYRot() { // don't turn your head if you are all head
        return 1;
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
    public float getWalkTargetValue(@NotNull BlockPos pos, @NotNull LevelReader world) {
        return 0.0F;
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
