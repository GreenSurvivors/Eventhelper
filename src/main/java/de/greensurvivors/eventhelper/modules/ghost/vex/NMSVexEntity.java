package de.greensurvivors.eventhelper.modules.ghost.vex;

import com.mojang.serialization.Dynamic;
import de.greensurvivors.eventhelper.modules.ghost.GhostGame;
import de.greensurvivors.eventhelper.modules.ghost.ghostentity.NMSGhostEntity;
import de.greensurvivors.eventhelper.modules.ghost.ghostentity.NMSUnderWorldGhostEntity;
import de.greensurvivors.eventhelper.modules.ghost.player.AlivePlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.DebugPackets;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.GameEventTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Unit;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.Vex;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.DynamicGameEventListener;
import net.minecraft.world.level.gameevent.EntityPositionSource;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gameevent.PositionSource;
import net.minecraft.world.level.gameevent.vibrations.VibrationSystem;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.level.pathfinder.FlyNodeEvaluator;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.PathFinder;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_20_R3.attribute.CraftAttributeMap;
import org.bukkit.craftbukkit.v1_20_R3.util.CraftLocation;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.util.function.BiConsumer;

public class NMSVexEntity extends Vex implements VibrationSystem {
    /**
     * do NOT - I repeat - do NOT call GHOST_TYPE.create!
     * There is no way to add the important game parameter there!
     */
    public static final EntityType<NMSVexEntity> VEX_TYPE = registerEntityType(
        (EntityType.Builder.
            of((ignored, world) -> EntityType.VEX.create(world), MobCategory.MONSTER).
            sized(EntityType.VEX.getDimensions().width, EntityType.VEX.getDimensions().height).
            fireImmune().
            noSave(). // don't save this entity to disk.
                clientTrackingRange(EntityType.VEX.clientTrackingRange())));

    private static final int VIBRATION_COOLDOWN_TICKS = 40;
    private static final int ANGERMANAGEMENT_TICK_DELAY = 20;
    private static final int DEFAULT_ANGER = 35;
    private static final int TOUCH_COOLDOWN_TICKS = 20;
    private final DynamicGameEventListener<VibrationSystem.Listener> dynamicGameEventListener = new DynamicGameEventListener<>(new VibrationSystem.Listener(this));
    private final VibrationSystem.User vibrationUser = new VibrationUser();
    private final VibrationSystem.Data vibrationData = new VibrationSystem.Data();
    private volatile @Nullable CraftVexEntity bukkitEntity;
    private final @NotNull GhostGame ghostGame;
    private final @NotNull BlockPos spawnPos;

    @SuppressWarnings("unchecked")
    // has to be called while the server is bootstrapping, or else the registry will be frozen!
    private static <T extends Entity> @NotNull EntityType<T> registerEntityType(final @NotNull EntityType.Builder<Entity> type) {
        return (EntityType<T>) Registry.register(BuiltInRegistries.ENTITY_TYPE, "ghost_vex",
            type.build("ghost_vex"));
    }

    public NMSVexEntity(final @NotNull Level world, final @NotNull GhostGame ghostGame, Location spawnLocation) {
        super(VEX_TYPE, world);
        this.ghostGame = ghostGame;

        hasLimitedLife = false;
        this.setPathfindingMalus(BlockPathTypes.DAMAGE_OTHER, 8.0F);
        this.setPathfindingMalus(BlockPathTypes.POWDER_SNOW, 8.0F);
        this.setPathfindingMalus(BlockPathTypes.LAVA, 8.0F);
        this.setPathfindingMalus(BlockPathTypes.DAMAGE_FIRE, 0.0F);
        this.setPathfindingMalus(BlockPathTypes.DANGER_FIRE, 0.0F);

        absMoveTo(spawnLocation.x(), spawnLocation.y(), spawnLocation.z(), spawnLocation.getYaw(), spawnLocation.getPitch());

        spawnPos = CraftLocation.toBlockPosition(spawnLocation);
    }

    @Override
    protected boolean shouldDespawnInPeaceful() {
        return false;
    }

    public static @NotNull AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().
            add(Attributes.MAX_HEALTH, 500.0D).
            add(Attributes.MOVEMENT_SPEED, 0.7D).
            add(Attributes.ATTACK_DAMAGE, 4.0D);
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
    public @Nullable AttributeInstance getAttribute(final @NotNull Attribute attribute) {
        try {
            return super.getAttribute(attribute);
        } catch (NullPointerException ignored) {
            Class<?> livingEntityClass = LivingEntity.class;

            try {
                // Access the private field
                Field privateAttributesField = livingEntityClass.getDeclaredField("bN"/*"attributes"*/); // todo change when update to moj mappings

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
        return new ClientboundAddEntityPacket(getId(), getUUID(), getX(), getY(), getZ(), getXRot(), getYRot(), EntityType.VEX, 0, getDeltaMovement(), getYHeadRot());
    }

    @Override
    public @NotNull CraftVexEntity getBukkitEntity() {
        if (this.bukkitEntity == null) {
            synchronized (this) {
                if (this.bukkitEntity == null) {
                    return this.bukkitEntity = new CraftVexEntity(this.level().getCraftServer(), this);
                }
            }
        }
        return this.bukkitEntity;
    }

    @Override
    protected void registerGoals() {
    }

    @Override
    public boolean isInvulnerableTo(final @NotNull DamageSource damageSource) {
        return !damageSource.is(DamageTypeTags.BYPASSES_INVULNERABILITY) || super.isInvulnerableTo(damageSource);
    }

    @Override
    protected boolean canRide(final @NotNull Entity entity) {
        return false;
    }

    @Override
    public boolean dampensVibrations() {
        return true;
    }

    @Override
    public float getSoundVolume() {
        return 4.0F;
    }

    @SuppressWarnings("resource") // ignore level being auto closeable
    @Override
    public void tick() {
        if (this.level() instanceof ServerLevel worldserver) {
            VibrationSystem.Ticker.tick(worldserver, this.vibrationData, this.vibrationUser);
        }

        super.tick();
    }

    @SuppressWarnings("resource") // ignore level being auto closeable
    @Override
    protected void customServerAiStep() {
        ServerLevel worldserver = (ServerLevel) this.level();

        worldserver.getProfiler().push("ghostVexBrain");
        this.getBrain().tick(worldserver, this);
        this.level().getProfiler().pop();
        super.customServerAiStep();

        VexAI.updateActivity(this);
    }

    @Override
    protected @NotNull Brain<?> makeBrain(@NotNull Dynamic<?> dynamic) {
        return VexAI.makeBrain(this, dynamic);
    }

    @Override
    public @NotNull Brain<NMSVexEntity> getBrain() {
        return (Brain<NMSVexEntity>) super.getBrain();
    }

    @Override
    protected void sendDebugPackets() {
        super.sendDebugPackets();
        DebugPackets.sendEntityBrain(this);
    }

    @SuppressWarnings("resource") // ignore level being auto closeable
    @Override
    public void updateDynamicGameEventListener(final @NotNull BiConsumer<DynamicGameEventListener<?>, ServerLevel> callback) {
        if (this.level() instanceof ServerLevel worldserver) {
            callback.accept(this.dynamicGameEventListener, worldserver);
        }
    }

    @SuppressWarnings("resource") // ignore level being auto closeable
    @Contract("null->false")
    public boolean canTargetEntity(final @Nullable Entity entity) {
        if (entity instanceof LivingEntity entityliving) {
            return this.level() == entity.level() &&
                ghostGame.getGhostModul().isInValidVexArea(CraftLocation.toBukkit(entityliving.position(), entityliving.level().getWorld())) &&
                ghostGame.getGhostGamePlayer(entity.getUUID()) instanceof AlivePlayer alivePlayer &&
                alivePlayer.getMouseTrapTrappedIn() == null &&
                EntitySelector.NO_CREATIVE_OR_SPECTATOR.test(entity) &&
                !this.isAlliedTo(entity) &&
                entityliving.getType() != EntityType.ARMOR_STAND &&
                entityliving.getType() != VEX_TYPE &&
                entityliving.getType() != NMSGhostEntity.GHOST_TYPE &&
                entityliving.getType() != NMSUnderWorldGhostEntity.UNDERWORLD_GHOST_TYPE &&
                !entityliving.isInvulnerable() &&
                !entityliving.isDeadOrDying() &&
                this.level().getWorldBorder().isWithinBounds(entityliving.getBoundingBox());
        }

        return false;
    }

    @Override
    public @Nullable LivingEntity getTarget() {
        return this.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET).orElse(null);
    }

    @Override
    public boolean removeWhenFarAway(final double distanceSquared) {
        return false;
    }

    @Override
    public boolean hurt(final @NotNull DamageSource source, final float amount) {
        boolean superHurt = super.hurt(source, amount);

        if (!this.isNoAi()) {
            Entity entity = source.getEntity();
            if (this.brain.getMemory(MemoryModuleType.ATTACK_TARGET).isEmpty() && entity instanceof LivingEntity entityliving) {

                if (!source.isIndirect() || this.closerThan(entityliving, 5.0D)) {
                    this.setAttackTarget(entityliving);
                }
            }
        }

        return superHurt;
    }

    public void setAttackTarget(final @NotNull LivingEntity target) {
        this.getBrain().setMemory(MemoryModuleType.ATTACK_TARGET, target);
        this.getBrain().eraseMemory(MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE);
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected void doPush(final @NotNull Entity entity) {
        if (!this.isNoAi() && !this.getBrain().hasMemoryValue(MemoryModuleType.TOUCH_COOLDOWN)) {
            this.getBrain().setMemoryWithExpiry(MemoryModuleType.TOUCH_COOLDOWN, Unit.INSTANCE, TOUCH_COOLDOWN_TICKS);

            if (this.brain.getMemory(MemoryModuleType.ATTACK_TARGET).isEmpty() && entity instanceof LivingEntity entityliving) {
                this.setAttackTarget(entityliving);
            }
        }

        super.doPush(entity);
    }

    @Override
    protected @NotNull PathNavigation createNavigation(final @NotNull Level world) {
        return new FlyingPathNavigation(this, world) {
            @Override
            protected @NotNull PathFinder createPathFinder(int range) {
                this.nodeEvaluator = new FlyNodeEvaluator();
                this.nodeEvaluator.setCanPassDoors(true);
                return new PathFinder(this.nodeEvaluator, range) {
                    @Override
                    protected float distance(@NotNull Node a, @NotNull Node b) {
                        return a.distanceToXZ(b);
                    }
                };
            }
        };
    }

    @Override
    public @NotNull VibrationSystem.Data getVibrationData() {
        return this.vibrationData;
    }

    @Override
    public @NotNull VibrationSystem.User getVibrationUser() {
        return this.vibrationUser;
    }

    @Override
    public boolean isWithinRestriction(final @NotNull BlockPos pos) {
        return ghostGame.getGhostModul().isInValidVexArea(CraftLocation.toBukkit(pos, level()));
    }

    public @Nullable BlockPos getUnstuckPos() {
        return spawnPos;
    }

    @Override
    public boolean hasRestriction() {
        return true;
    }

    @Override
    public float getRestrictRadius() {
        return 0;
    }

    private class VibrationUser implements VibrationSystem.User {
        private static final int GAME_EVENT_LISTENER_RANGE = 24;
        private final PositionSource positionSource = new EntityPositionSource(NMSVexEntity.this, NMSVexEntity.this.getEyeHeight());

        VibrationUser() {
        }

        @Override
        public int getListenerRadius() {
            return GAME_EVENT_LISTENER_RANGE;
        }

        @Override
        public @NotNull PositionSource getPositionSource() {
            return this.positionSource;
        }

        @Override
        public @NotNull TagKey<GameEvent> getListenableEvents() {
            return GameEventTags.WARDEN_CAN_LISTEN;
        }

        @Override
        public boolean canTriggerAvoidVibration() {
            return true;
        }

        @Override
        public boolean canReceiveVibration(final @NotNull ServerLevel world,
                                           final @NotNull BlockPos pos,
                                           final @NotNull GameEvent event,
                                           final @NotNull GameEvent.Context emitter) {
            if (!NMSVexEntity.this.isNoAi() && !NMSVexEntity.this.isDeadOrDying() && !NMSVexEntity.this.getBrain().hasMemoryValue(MemoryModuleType.VIBRATION_COOLDOWN) && world.getWorldBorder().isWithinBounds(pos)) {
                Entity entity = emitter.sourceEntity();

                if (entity instanceof LivingEntity entityliving) {
                    return NMSVexEntity.this.canTargetEntity(entityliving);
                }

                return true;
            } else {
                return false;
            }
        }

        @Override
        public void onReceiveVibration(final @NotNull ServerLevel world,
                                       final @NotNull BlockPos pos,
                                       final @NotNull GameEvent event,
                                       final @Nullable Entity sourceEntity,
                                       final @Nullable Entity entity,
                                       final float distance) {
            if (!NMSVexEntity.this.isDeadOrDying()) {
                // ignore vibrations outside of region
                if (sourceEntity == null || !NMSVexEntity.this.isWithinRestriction(entity == null ? sourceEntity.blockPosition() : entity.blockPosition())) {
                    return;
                }

                NMSVexEntity.this.brain.setMemoryWithExpiry(MemoryModuleType.VIBRATION_COOLDOWN, Unit.INSTANCE, VIBRATION_COOLDOWN_TICKS);
                //world.broadcastEntityEvent(NMSVexEntity.this, (byte) 61);
                NMSVexEntity.this.playSound(SoundEvents.WARDEN_TENDRIL_CLICKS, 5.0F, NMSVexEntity.this.getVoicePitch());

                if (NMSVexEntity.this.canTargetEntity(sourceEntity)) {
                    setAttackTarget((LivingEntity) sourceEntity);
                }
            }
        }
    }
}
