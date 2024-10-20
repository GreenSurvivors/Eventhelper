package de.greensurvivors.eventhelper.modules.ghost.entity;

import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.util.Pair;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.behavior.*;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.sensing.Sensor;
import net.minecraft.world.entity.ai.sensing.SensorType;
import net.minecraft.world.entity.schedule.Activity;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Set;

public class GhostAI {
    static final List<SensorType<? extends Sensor<? super GhostNMSEntity>>> SENSOR_TYPES =
        ImmutableList.of(
            NearstAlivePlayerSensor.NEAREST_ALIVE_PLAYER_SENSOR,
            SensorType.NEAREST_PLAYERS
        );
    static final List<MemoryModuleType<?>> MEMORY_TYPES = ImmutableList.of(
        MemoryModuleType.LOOK_TARGET,
        MemoryModuleType.NEAREST_PLAYERS, //
        MemoryModuleType.NEAREST_VISIBLE_PLAYER, //
        MemoryModuleType.NEAREST_VISIBLE_ATTACKABLE_PLAYER,// ??
        MemoryModuleType.LOOK_TARGET,
        MemoryModuleType.WALK_TARGET,
        MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE,
        MemoryModuleType.ATTACK_TARGET, // needed?
        MemoryModuleType.NEAREST_ATTACKABLE, //
        MemoryModuleType.WALK_TARGET,
        MemoryModuleType.PATH
    );

    protected static Brain<?> makeBrain(final @NotNull Brain<GhostNMSEntity> brain) {
        initCoreActivity(brain);
        //initIdleActivity(brain);
        initFightActivity(brain);
        brain.setCoreActivities(Set.of(Activity.CORE));
        brain.setDefaultActivity(Activity.FIGHT);
        brain.useDefaultActivity();
        return brain;
    }

    private static void initCoreActivity(Brain<GhostNMSEntity> brain) {
        brain.addActivity(Activity.CORE, 0, ImmutableList.of(new Swim(0.8F), new LookAtTargetSink(45, 90), new MoveToTargetSink()));
    }

    private static void initFightActivity(Brain<GhostNMSEntity> brain) {
        brain.addActivityWithConditions(
            Activity.FIGHT,
            ImmutableList.of(
                Pair.of(0, StartAttacking.create(ghost -> ghost.getBrain().getMemory(MemoryModuleType.NEAREST_ATTACKABLE))),
                Pair.of(1, StopAttackingIfTargetInvalid.create()),
                /*Pair.of(2, new Shoot()),
                Pair.of(3, new ShootWhenStuck()),
                Pair.of(4, new LongJump()),
                Pair.of(5, new Slide()),*/
                Pair.of(2, new RunOne<>(ImmutableList.of(Pair.of(new DoNothing(20, 100), 1), Pair.of(RandomStroll.stroll(0.6F), 2))))
            ),
            Set.of()
        );
    }

    public static void updateActivity(final @NotNull GhostNMSEntity ghost) {
        ghost.getBrain().setActiveActivityToFirstValid(
            ImmutableList.of(Activity.FIGHT, Activity.INVESTIGATE, Activity.SNIFF, Activity.IDLE));
    }
}
