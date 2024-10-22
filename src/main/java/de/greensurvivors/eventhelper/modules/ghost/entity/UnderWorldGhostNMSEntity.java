package de.greensurvivors.eventhelper.modules.ghost.entity;

import de.greensurvivors.eventhelper.modules.ghost.GhostGame;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.ItemStack;
import org.bukkit.craftbukkit.v1_20_R3.attribute.CraftAttributeMap;
import org.bukkit.craftbukkit.v1_20_R3.entity.CraftLivingEntity;
import org.bukkit.event.entity.EntityRemoveEvent;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;

import javax.annotation.Nullable;
import java.lang.reflect.Field;

public class UnderWorldGhostNMSEntity extends Monster {
    /**
     * do NOT - I repeat - do NOT call UNDERWORLD_GHOST_TYPE.create!
     * There is no way to add the important game parameter there!
     */
    public static final EntityType<UnderWorldGhostNMSEntity> UNDERWORLD_GHOST_TYPE = registerEntityType(
        (EntityType.Builder.
            of(null, MobCategory.MISC).
            sized(0.6F, 1F). // 1 block height to fit through all gaps
                noSave(). // don't save this entity to disk.
                clientTrackingRange(10)));

    private final @NotNull GhostGame ghostGame;
    private final @NotNull GhostNMSEntity parentMob;
    private volatile @Nullable UnderWorldGhostCraftEntity bukkitEntity;

    public UnderWorldGhostNMSEntity(final @NotNull GhostNMSEntity parentMob, final @NotNull GhostGame ghostGame) {
        super(UNDERWORLD_GHOST_TYPE, parentMob.level());

        setCanPickUpLoot(false);

        this.ghostGame = ghostGame;
        this.parentMob = parentMob;

        this.setPos(parentMob.position());
        this.setYRot(parentMob.getXRot());
        this.setYRot(parentMob.getYRot());

        final GhostPathNavigation ghostPathNavigation = new GhostPathNavigation(this, parentMob, parentMob.level());
        ghostPathNavigation.setCanFloat(true); // can swim. not like floating in the air
        navigation = ghostPathNavigation;
        this.setPersistenceRequired();
    }

    @SuppressWarnings("unchecked")
    // has to be called while the server is bootstrapping, or else the registry will be frozen!
    private static <T extends Entity> EntityType<T> registerEntityType(EntityType.Builder<Entity> type) {
        final EntityType<T> registered = (EntityType<T>) Registry.register(BuiltInRegistries.ENTITY_TYPE, "underworld_ghost",
            type.build("underworld_ghost"));
        return registered;
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
                Field privateAttributesField = livingEntityClass.getDeclaredField("bN"/*"attributes"*/); // todo change one update to moj mapped server vers.

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
    public @NotNull Packet<ClientGamePacketListener> getAddEntityPacket() { // overwrite with skeleton type
        return new ClientboundAddEntityPacket(getId(), getUUID(), getX(), getY(), getZ(), getXRot(), getYRot(), EntityType.ALLAY, 0, getDeltaMovement(), getYHeadRot());
    }

    @Override
    public @NotNull CraftLivingEntity getBukkitLivingEntity() {
        return getBukkitEntity();
    }

    @SuppressWarnings("resource")
    @Override
    public @NotNull UnderWorldGhostCraftEntity getBukkitEntity() {
        if (this.bukkitEntity == null) {
            synchronized (this) {
                if (this.bukkitEntity == null) {
                    return this.bukkitEntity = new UnderWorldGhostCraftEntity(this.level().getCraftServer(), this);
                }
            }
        }
        return this.bukkitEntity;
    }

    @Override
    protected @NotNull Vector3f getPassengerAttachmentPoint(@NotNull Entity passenger, @NotNull EntityDimensions dimensions, float scaleFactor) {
        float walkAnimationSpeed = Math.min(0.25F, this.walkAnimation.speed());
        float walkAnimationPos = this.walkAnimation.position();
        float bumpOffset = 0.12F * Mth.cos(walkAnimationPos * 0.5F) * 2.0F * walkAnimationSpeed;

        return new Vector3f(0.0F, (float) ghostGame.getConfig().getPathfindOffset() + bumpOffset * scaleFactor, 0.0F);
    }

    @Override
    protected void registerGoals() {
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
    public boolean hasLineOfSight(@NotNull Entity entity) {
        return parentMob.hasLineOfSight(entity);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
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
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    public boolean isSilent() { // don't make any sound
        return true;
    }

    public boolean hasIndirectPassenger(Entity passenger) {
        if (passenger == parentMob) {
            return true;
        } else if (passenger instanceof GhostNMSEntity ghostNMS) {
            UnderWorldGhostNMSEntity underWorldGhost = ghostNMS.underWorldGhost;

            if (!underWorldGhost.isPassenger()) {
                return false;
            } else {
                Entity entity1 = underWorldGhost.getVehicle();

                return entity1 == parentMob || this.hasIndirectPassenger(entity1);
            }
        } else if (!passenger.isPassenger()) {
            return false;
        } else {
            Entity entity1 = passenger.getVehicle();

            return entity1 == this ? true : this.hasIndirectPassenger(entity1);
        }
    }

    @Override
    protected void addPassenger(Entity passenger) {
        parentMob.addPassenger(passenger); // here because of protected access
    }

    @Override
    protected boolean removePassenger(Entity entity, boolean suppressCancellation) {
        return parentMob.removePassenger(entity, suppressCancellation);
    }

    @Override
    public void remove(Entity.RemovalReason entity_removalreason, EntityRemoveEvent.Cause cause) {
        super.remove(entity_removalreason, cause);

        if (!parentMob.isRemoved()) {
            parentMob.remove(entity_removalreason, cause);
        }
    }
}
