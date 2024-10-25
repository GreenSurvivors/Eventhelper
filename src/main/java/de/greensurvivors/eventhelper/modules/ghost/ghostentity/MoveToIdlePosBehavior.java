package de.greensurvivors.eventhelper.modules.ghost.ghostentity;

import io.papermc.paper.math.Position;
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
import java.util.List;
import java.util.Optional;

@SuppressWarnings("UnstableApiUsage") // Position
public class MoveToIdlePosBehavior {
    public static @NotNull OneShot<NMSGhostEntity> moveToIdlePos() {
        return BehaviorBuilder.create(
            context -> context.group(context.absent(MemoryModuleType.WALK_TARGET)).apply(context, walkTarget -> (world, entity, time) -> {
                Optional<Vec3> optional = Optional.ofNullable(getPos(entity));
                walkTarget.setOrErase(optional.map(pos -> new WalkTarget(pos, (float) entity.getIdleVelocity(), 0)));
                return true;
            })
        );
    }

    @Nullable
    public static Vec3 getPos(final @NotNull NMSGhostEntity entity) {
        return RandomPos.generateRandomPos(() -> {
            final @NotNull List<@NotNull Position> idlePositions = entity.ghostGame.getConfig().getIdlePositions();

            if (!idlePositions.isEmpty()) {
                Position position = idlePositions.get(entity.getRandom().nextInt(idlePositions.size()));
                BlockPos blockPos = new BlockPos(position.blockX(), position.blockY(), position.blockZ());

                if (!GoalUtils.isOutsideLimits(blockPos, entity) &&
                    !GoalUtils.isNotStable(entity.getNavigation(), blockPos) &&
                    !GoalUtils.isWater(entity, blockPos) &&
                    !GoalUtils.hasMalus(entity, blockPos)) {
                    return blockPos;
                }
            }
            return null;

        }, entity::getWalkTargetValue);
    }
}
