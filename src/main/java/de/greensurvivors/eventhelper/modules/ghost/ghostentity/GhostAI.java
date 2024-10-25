package de.greensurvivors.eventhelper.modules.ghost.ghostentity;

import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.util.Pair;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.entity.LivingEntity;
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
    protected static final List<SensorType<? extends Sensor<? super NMSGhostEntity>>> SENSOR_TYPES =
        ImmutableList.of(
            SensorType.NEAREST_LIVING_ENTITIES,
            NearestAlivePlayerSensor.NEAREST_ALIVE_PLAYER_SENSOR
        );
    protected static final List<MemoryModuleType<?>> MEMORY_TYPES = ImmutableList.of(
        MemoryModuleType.ATTACK_TARGET, // check for near players to set target to the nearest path findable
        MemoryModuleType.LOOK_TARGET,
        MemoryModuleType.WALK_TARGET,
        MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE,
        MemoryModuleType.PATH,
        MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES,
        MemoryModuleType.ATTACK_COOLING_DOWN,

        MemoryModuleType.NEAREST_LIVING_ENTITIES,
        MemoryModuleType.NEAREST_ATTACKABLE
    );

    protected static Brain<?> makeBrain(final @NotNull NMSGhostEntity ghost, final @NotNull Brain<NMSGhostEntity> brain) {
        initCoreActivity(brain);
        initIdleActivity(ghost, brain);
        initFightActivity(ghost, brain);
        brain.setCoreActivities(Set.of(Activity.CORE));
        brain.setDefaultActivity(Activity.FIGHT);
        brain.useDefaultActivity();
        return brain;
    }

    private static void initCoreActivity(final @NotNull Brain<NMSGhostEntity> brain) {
        brain.addActivity(Activity.CORE, 0, ImmutableList.of(new GhostSwimBehavior(0.8F), new LookAtTargetSink(45, 90), new MoveToTargetSink()));
    }

    private static void initIdleActivity(final @NotNull NMSGhostEntity ghost, final @NotNull Brain<NMSGhostEntity> brain) {
        brain.addActivity(
            Activity.IDLE,
            10,
            ImmutableList.of(
                StartAttacking.create(ignored -> brain.getMemory(MemoryModuleType.ATTACK_TARGET)),
                SetEntityLookTargetSometimes.create(8.0F, UniformInt.of(30, 60)),
                new RunOne<>(
                    ImmutableList.of(
                        Pair.of(MoveToIdlePosBehavior.moveToIdlePos(), 2), // todo replace with pathfind to middle

                        Pair.of(SetWalkTargetFromLookTarget.create((float) ghost.getIdleVelocity(), 3), 2),
                        Pair.of(new DoNothing(30, 60), 1)
                    )
                )
            )
        );
    }

    private static void initFightActivity(final @NotNull NMSGhostEntity ghost, final @NotNull Brain<NMSGhostEntity> brain) {
        brain.addActivityAndRemoveMemoryWhenStopped(
            Activity.FIGHT,
            10,
            ImmutableList.of(
                StopAttackingIfTargetInvalid.create(
                    entity -> !ghost.canTargetEntity(entity), GhostAI::onTargetInvalid, false),
                MeleeAttack.create(8),
                StopAttackingIfTargetInvalid.create()
            ),
            MemoryModuleType.ATTACK_TARGET
        );
    }

    private static void onTargetInvalid(NMSGhostEntity entity, LivingEntity entity1) {
    }

    public static void updateActivity(final @NotNull NMSGhostEntity ghost) {
        Activity activity = ghost.getBrain().getActiveNonCoreActivity().orElse(null);
        ghost.getBrain().setActiveActivityToFirstValid(ImmutableList.of(Activity.FIGHT, Activity.IDLE));
        Activity activity2 = ghost.getBrain().getActiveNonCoreActivity().orElse(null);
        if (activity2 == Activity.FIGHT && activity != Activity.FIGHT) {
            ghost.playAngrySound();
        }

        ghost.setAggressive(ghost.getBrain().hasMemoryValue(MemoryModuleType.ATTACK_TARGET));

        ghost.getBrain().setActiveActivityToFirstValid(
            ImmutableList.of(Activity.FIGHT, Activity.IDLE));
    }
}
