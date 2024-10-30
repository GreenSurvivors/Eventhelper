package de.greensurvivors.eventhelper.modules.ghost.vex;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.behavior.OneShot;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.ai.util.GoalUtils;
import net.minecraft.world.entity.ai.util.RandomPos;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.Optional;

// the big difference to vanilla code here is that this does not depend on the restriction where an entity can move to being a sphere.
// also this is a bit more compact since we always can assume the same constants for our vexes being used.
public class RandomStrollInRegionBehavior {
    private static final int MAX_XZ_DIST = 10;
    private static final int MAX_Y_DIST = 7;

    public static OneShot<NMSVexEntity> fly(float speed) {
        final @NotNull StuckDetectionHolder stuckDetectionHolder = new StuckDetectionHolder();

        return BehaviorBuilder.create(
            context -> context.group(context.absent(MemoryModuleType.WALK_TARGET)).apply(context, walkTarget -> (world, entity, time) -> {
                final @NotNull Vec3 vec3 = entity.getViewVector(0.0F);
                Optional<Vec3> optional = Optional.ofNullable(
                    RandomPos.generateRandomPos(
                        entity, () -> randomPosFactory(entity, vec3.x, vec3.z, stuckDetectionHolder)));
                walkTarget.setOrErase(optional.map(pos -> new WalkTarget(pos, speed, 0)));
                return true;
            })
        );
    }

    @SuppressWarnings("resource") // ignore level being auto closeable
    protected static @Nullable BlockPos randomPosFactory(
        final @NotNull NMSVexEntity entity,
        final double directionX,
        final double directionZ,
        final @NotNull StuckDetectionHolder stuckDetectionHolder) {
        final @Nullable BlockPos blockPos = RandomPos.generateRandomDirectionWithinRadians(
            entity.getRandom(), MAX_XZ_DIST, MAX_Y_DIST, -2, directionX, directionZ, (Math.PI / 2.0D));
        if (blockPos == null) {
            return null;
        } else {
            BlockPos blockPos2 = RandomPos.generateRandomPosTowardDirection(entity, MAX_XZ_DIST, entity.getRandom(), blockPos);

            if (!GoalUtils.isOutsideLimits(blockPos2, entity) && entity.isWithinRestriction(blockPos2)) {
                blockPos2 = RandomPos.moveUpOutOfSolid(blockPos2, entity.level().getMaxBuildHeight(), pos -> GoalUtils.isSolid(entity, pos));

                if (GoalUtils.hasMalus(entity, blockPos2)) {
                    return null;
                } else {

                    stuckDetectionHolder.reset();
                    return blockPos2;
                }
            } else {
                if (stuckDetectionHolder.doStuckDetection()) {
                    return RandomPos.moveUpOutOfSolid(entity.getUnstuckPos(), entity.level().getMaxBuildHeight(), pos -> GoalUtils.isSolid(entity, pos));
                }

                return null;
            }
        }
    }

    protected static class StuckDetectionHolder {
        protected int lastStuckCheck = 0;

        protected void reset() {
            this.lastStuckCheck = 0;
        }

        protected boolean doStuckDetection() {
            this.lastStuckCheck++;

            if (this.lastStuckCheck > 40) {
                this.lastStuckCheck = 0;
                return true;
            }

            return false;
        }
    }
}
