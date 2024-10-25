package de.greensurvivors.eventhelper.modules.ghost.vex;

import com.mojang.serialization.Dynamic;
import de.greensurvivors.eventhelper.modules.ghost.GhostGame;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.DebugPackets;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.GameEventTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Unit;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.monster.Vex;
import net.minecraft.world.entity.monster.warden.AngerLevel;
import net.minecraft.world.entity.monster.warden.AngerManagement;
import net.minecraft.world.entity.player.Player;
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
import org.bukkit.craftbukkit.v1_20_R3.util.CraftLocation;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Optional;
import java.util.function.BiConsumer;

public class VexNMSEntity extends Vex implements VibrationSystem { // todo system to stay in region
    private static final int VIBRATION_COOLDOWN_TICKS = 40;
    private static final int ANGERMANAGEMENT_TICK_DELAY = 20;
    private static final int DEFAULT_ANGER = 35;
    private static final int TOUCH_COOLDOWN_TICKS = 20;
    private final DynamicGameEventListener<VibrationSystem.Listener> dynamicGameEventListener = new DynamicGameEventListener<>(new VibrationSystem.Listener(this));
    private final VibrationSystem.User vibrationUser = new VibrationUser();
    private final VibrationSystem.Data vibrationData = new VibrationSystem.Data();
    AngerManagement angerManagement = new AngerManagement(this::canTargetEntity, Collections.emptyList());
    private volatile @Nullable VexCraftEntity bukkitEntity;
    private final @NotNull GhostGame ghostGame;

    public VexNMSEntity(final @NotNull Level world, final @NotNull GhostGame ghostGame) {
        super(EntityType.VEX, world);

        this.ghostGame = ghostGame;

        hasLimitedLife = false;
        this.setPathfindingMalus(BlockPathTypes.DAMAGE_OTHER, 8.0F);
        this.setPathfindingMalus(BlockPathTypes.POWDER_SNOW, 8.0F);
        this.setPathfindingMalus(BlockPathTypes.LAVA, 8.0F);
        this.setPathfindingMalus(BlockPathTypes.DAMAGE_FIRE, 0.0F);
        this.setPathfindingMalus(BlockPathTypes.DANGER_FIRE, 0.0F);
    }

    @Override
    public @NotNull VexCraftEntity getBukkitEntity() {
        if (this.bukkitEntity == null) {
            synchronized (this) {
                if (this.bukkitEntity == null) {
                    return this.bukkitEntity = new VexCraftEntity(this.level().getCraftServer(), this);
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

        if (this.tickCount % ANGERMANAGEMENT_TICK_DELAY == 0) {
            this.angerManagement.tick(worldserver, this::canTargetEntity);
        }

        VexAI.updateActivity(this);
    }

    @Override
    protected @NotNull Brain<?> makeBrain(@NotNull Dynamic<?> dynamic) {
        return VexAI.makeBrain(this, dynamic);
    }

    @Override
    public @NotNull Brain<VexNMSEntity> getBrain() {
        return (Brain<VexNMSEntity>) super.getBrain();
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
                ghostGame.getGhostModul().isInValidArea(CraftLocation.toBukkit(entityliving.position(), entityliving.level().getWorld())) &&
                EntitySelector.NO_CREATIVE_OR_SPECTATOR.test(entity) &&
                !this.isAlliedTo(entity) &&
                entityliving.getType() != EntityType.ARMOR_STAND &&
                entityliving.getType() != EntityType.VEX &&
                !entityliving.isInvulnerable() &&
                !entityliving.isDeadOrDying() &&
                this.level().getWorldBorder().isWithinBounds(entityliving.getBoundingBox());
        }

        return false;
    }

    public @NotNull AngerLevel getAngerLevel() {
        return AngerLevel.byAnger(this.getActiveAnger());
    }

    private int getActiveAnger() {
        return this.angerManagement.getActiveAnger(this.getTarget());
    }

    public void clearAnger(final @NotNull Entity entity) {
        this.angerManagement.clearAnger(entity);
    }

    public void increaseAngerAt(final @Nullable Entity entity) {
        this.increaseAngerAt(entity, DEFAULT_ANGER);
    }

    protected void increaseAngerAt(final @Nullable Entity entity, final int amount) {
        if (!this.isNoAi() && this.canTargetEntity(entity)) {
            boolean attackTargetNotPlayer = !(this.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET).orElse(null) instanceof Player); // CraftBukkit - decompile error
            int increasedAnger = this.angerManagement.increaseAnger(entity, amount);

            if (entity instanceof Player && attackTargetNotPlayer && AngerLevel.byAnger(increasedAnger).isAngry()) {
                this.getBrain().eraseMemory(MemoryModuleType.ATTACK_TARGET);
            }
        }
    }

    public Optional<LivingEntity> getEntityAngryAt() {
        return this.getAngerLevel().isAngry() ? this.angerManagement.getActiveEntity() : Optional.empty();
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

            this.increaseAngerAt(entity, AngerLevel.ANGRY.getMinimumAnger() + 20);
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
            this.increaseAngerAt(entity);
            if (this.isWithinRestriction(entity.blockPosition())) {
                VexAI.setDisturbanceLocation(this, entity.blockPosition());
            }
        }

        super.doPush(entity);
    }

    public AngerManagement getAngerManagement() {
        return this.angerManagement;
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
        return ghostGame.getGhostModul().isInValidArea(CraftLocation.toBukkit(pos, level()));
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
        private final PositionSource positionSource = new EntityPositionSource(VexNMSEntity.this, VexNMSEntity.this.getEyeHeight());

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
            if (!VexNMSEntity.this.isNoAi() && !VexNMSEntity.this.isDeadOrDying() && !VexNMSEntity.this.getBrain().hasMemoryValue(MemoryModuleType.VIBRATION_COOLDOWN) && world.getWorldBorder().isWithinBounds(pos)) {
                Entity entity = emitter.sourceEntity();

                if (entity instanceof LivingEntity entityliving) {
                    return VexNMSEntity.this.canTargetEntity(entityliving);
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
            if (!VexNMSEntity.this.isDeadOrDying()) {
                // ignore vibrations outside of region
                if (!VexNMSEntity.this.isWithinRestriction(entity == null ? sourceEntity.blockPosition() : entity.blockPosition())) {
                    return;
                }

                VexNMSEntity.this.brain.setMemoryWithExpiry(MemoryModuleType.VIBRATION_COOLDOWN, Unit.INSTANCE, VIBRATION_COOLDOWN_TICKS);
                world.broadcastEntityEvent(VexNMSEntity.this, (byte) 61);
                VexNMSEntity.this.playSound(SoundEvents.WARDEN_TENDRIL_CLICKS, 5.0F, VexNMSEntity.this.getVoicePitch());
                BlockPos blockposition1 = pos;

                if (entity != null) {
                    if (VexNMSEntity.this.closerThan(entity, 30.0D)) {
                        if (VexNMSEntity.this.getBrain().hasMemoryValue(MemoryModuleType.RECENT_PROJECTILE)) {
                            if (VexNMSEntity.this.canTargetEntity(entity)) {
                                blockposition1 = entity.blockPosition();
                            }

                            VexNMSEntity.this.increaseAngerAt(entity);
                        } else {
                            VexNMSEntity.this.increaseAngerAt(entity, 10);
                        }
                    }

                    VexNMSEntity.this.getBrain().setMemoryWithExpiry(MemoryModuleType.RECENT_PROJECTILE, Unit.INSTANCE, 100L);
                } else {
                    VexNMSEntity.this.increaseAngerAt(sourceEntity);
                }

                if (!VexNMSEntity.this.getAngerLevel().isAngry()) {
                    Optional<LivingEntity> optional = VexNMSEntity.this.angerManagement.getActiveEntity();

                    if (entity != null || optional.isEmpty() || optional.get() == sourceEntity) {
                        VexAI.setDisturbanceLocation(VexNMSEntity.this, blockposition1);
                    }
                }
            }
        }
    }
}
