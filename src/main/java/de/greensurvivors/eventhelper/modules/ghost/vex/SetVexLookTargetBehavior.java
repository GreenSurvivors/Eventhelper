package de.greensurvivors.eventhelper.modules.ghost.vex;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.entity.ai.behavior.BlockPosTracker;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

class SetVexLookTargetBehavior {
    public static @NotNull BehaviorControl<LivingEntity> create() {
        return BehaviorBuilder.create(
            context -> context.group(
                    context.registered(MemoryModuleType.LOOK_TARGET),
                    context.registered(MemoryModuleType.DISTURBANCE_LOCATION),
                    context.absent(MemoryModuleType.ATTACK_TARGET)
                )
                .apply(
                    context,
                    (lookTarget, disturbanceLocation, attackTarget) -> (world, entity, time) -> {
                        Optional<BlockPos> optional = context.tryGet(disturbanceLocation);
                        if (optional.isEmpty()) {
                            return false;
                        } else {
                            lookTarget.set(new BlockPosTracker(optional.get()));
                            return true;
                        }
                    }
                )
        );
    }
}
