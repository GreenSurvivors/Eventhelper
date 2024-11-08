package de.greensurvivors.eventhelper.modules.ghost.ghostentity;

import com.google.common.collect.ImmutableMap;
import de.greensurvivors.eventhelper.modules.ghost.player.AGhostGamePlayer;
import de.greensurvivors.eventhelper.modules.ghost.player.AlivePlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.behavior.EntityTracker;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class GhostMoveToTargetSinkBehavior extends Behavior<NMSGhostEntity> {
    private static final int MAX_COOLDOWN_BEFORE_RETRYING = 40;
    private int remainingCooldown;
    private @Nullable Path path;
    private @Nullable BlockPos lastTargetPos;
    private float speedModifier;

    public GhostMoveToTargetSinkBehavior() {
        this(150, 250);
    }

    public GhostMoveToTargetSinkBehavior(final int minRunTime, final int maxRunTime) {
        super(
            ImmutableMap.of(
                MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE,
                MemoryStatus.REGISTERED,
                MemoryModuleType.PATH,
                MemoryStatus.VALUE_ABSENT,
                MemoryModuleType.WALK_TARGET,
                MemoryStatus.VALUE_PRESENT
            ),
            minRunTime,
            maxRunTime
        );
    }

    @Override
    protected boolean checkExtraStartConditions(final @NotNull ServerLevel world, final @NotNull NMSGhostEntity ghostEntity) {
        if (this.remainingCooldown > 0) {
            this.remainingCooldown--;
            return false;
        } else {
            Brain<?> brain = ghostEntity.getBrain();
            WalkTarget walkTarget = brain.getMemory(MemoryModuleType.WALK_TARGET).get();
            boolean reachedTarget = this.reachedTarget(ghostEntity, walkTarget);
            if (!reachedTarget && this.tryComputePath(ghostEntity, walkTarget, world.getGameTime())) {
                this.lastTargetPos = walkTarget.getTarget().currentBlockPosition();
                return true;
            } else {
                brain.eraseMemory(MemoryModuleType.WALK_TARGET);
                if (reachedTarget) {
                    brain.eraseMemory(MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE);
                }

                return false;
            }
        }
    }

    @Override
    protected boolean canStillUse(final @NotNull ServerLevel world, final @NotNull NMSGhostEntity ghostEntity, final long time) {
        if (this.path != null && this.lastTargetPos != null) {
            Optional<WalkTarget> optionalWalkTarget = ghostEntity.getBrain().getMemory(MemoryModuleType.WALK_TARGET);
            boolean isSpectator = optionalWalkTarget.map(GhostMoveToTargetSinkBehavior::isWalkTargetSpectator).orElse(false);
            boolean isAlive = optionalWalkTarget.map(walkTarget -> GhostMoveToTargetSinkBehavior.isWalkTargetGhostGameAlive(ghostEntity, walkTarget)).orElse(false);
            return !ghostEntity.getNavigation().isDone() && optionalWalkTarget.isPresent() && !this.reachedTarget(ghostEntity, optionalWalkTarget.get()) && !isSpectator && !isAlive;
        } else {
            return false;
        }
    }

    @Override
    protected void stop(final @NotNull ServerLevel world, final @NotNull NMSGhostEntity entity, final long time) {
        if (entity.getBrain().hasMemoryValue(MemoryModuleType.WALK_TARGET)
            && !this.reachedTarget(entity, entity.getBrain().getMemory(MemoryModuleType.WALK_TARGET).get())
            && entity.getNavigation().isStuck()) {
            this.remainingCooldown = world.getRandom().nextInt(MAX_COOLDOWN_BEFORE_RETRYING);
        }

        entity.getNavigation().stop();
        entity.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
        entity.getBrain().eraseMemory(MemoryModuleType.PATH);
        this.path = null;
    }

    @Override
    protected void start(final @NotNull ServerLevel serverLevel, final @NotNull NMSGhostEntity nmsGhostEntity, final long ignored) {
        nmsGhostEntity.getBrain().setMemory(MemoryModuleType.PATH, this.path);
        nmsGhostEntity.getNavigation().moveTo(this.path, this.speedModifier);
    }

    @Override
    protected void tick(final @NotNull ServerLevel serverLevel, final @NotNull NMSGhostEntity nmsGhostEntity, final long time) {
        Path path = nmsGhostEntity.getNavigation().getPath();
        Brain<?> brain = nmsGhostEntity.getBrain();
        if (this.path != path) {
            this.path = path;
            brain.setMemory(MemoryModuleType.PATH, path);
        }

        if (path != null && this.lastTargetPos != null) {
            WalkTarget walkTarget = brain.getMemory(MemoryModuleType.WALK_TARGET).get();
            if (walkTarget.getTarget().currentBlockPosition().distSqr(this.lastTargetPos) > 4.0
                && this.tryComputePath(nmsGhostEntity, walkTarget, serverLevel.getGameTime())) {
                this.lastTargetPos = walkTarget.getTarget().currentBlockPosition();
                this.start(serverLevel, nmsGhostEntity, time);
            }
        }
    }

    protected boolean tryComputePath(final @NotNull NMSGhostEntity entity, final @NotNull WalkTarget walkTarget, final long time) {
        BlockPos blockPos = walkTarget.getTarget().currentBlockPosition();

        this.path = entity.getNavigation().createPath(blockPos.below((int) entity.ghostGame.getConfig().getPathfindOffset()), 0);
        this.speedModifier = walkTarget.getSpeedModifier();
        Brain<?> brain = entity.getBrain();
        if (this.reachedTarget(entity, walkTarget)) {
            brain.eraseMemory(MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE);
        } else {
            boolean canReach = this.path != null && this.path.canReach();
            if (canReach) {
                brain.eraseMemory(MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE);
            } else if (!brain.hasMemoryValue(MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE)) {
                brain.setMemory(MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE, time);
            }

            if (this.path != null) {
                return true;
            }

            Vec3 vec3 = DefaultRandomPos.getPosTowards(entity, entity.getFollowRangeAt(), (int) (entity.getFollowRangeAt() * 0.7D), Vec3.atBottomCenterOf(blockPos), (float) (Math.PI / 2));
            if (vec3 != null) {
                this.path = entity.getNavigation().createPath(vec3.x, vec3.y - entity.ghostGame.getConfig().getPathfindOffset(), vec3.z, 0);
                return this.path != null;
            }
        }

        return false;
    }

    protected boolean reachedTarget(final @NotNull NMSGhostEntity entity, final @NotNull WalkTarget walkTarget) {
        return walkTarget.getTarget().currentBlockPosition().distManhattan(entity.blockPosition()) <= walkTarget.getCloseEnoughDist();
    }

    protected static boolean isWalkTargetSpectator(final @NotNull WalkTarget target) {
        return target.getTarget() instanceof EntityTracker entityTracker && entityTracker.getEntity().isSpectator();
    }

    protected static boolean isWalkTargetGhostGameAlive(final @NotNull NMSGhostEntity nmsGhostEntity, final @NotNull WalkTarget target) {
        if (target.getTarget() instanceof EntityTracker entityTracker) {
            if (entityTracker.getEntity() instanceof ServerPlayer serverPlayer) {
                AGhostGamePlayer ghostGamePlayer = nmsGhostEntity.ghostGame.getGhostGamePlayer(serverPlayer.getUUID());
                if (ghostGamePlayer instanceof AlivePlayer alivePlayer) {
                    return alivePlayer.getMouseTrapTrappedIn() != null;
                }
            }
        }

        return false;
    }
}
