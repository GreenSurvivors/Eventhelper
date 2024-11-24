package de.greensurvivors.eventhelper.modules.ghost.vex;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Dynamic;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.behavior.*;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.sensing.Sensor;
import net.minecraft.world.entity.ai.sensing.SensorType;
import net.minecraft.world.entity.schedule.Activity;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class VexAI {
    private static final float SPEED_MULTIPLIER_WHEN_IDLING = 0.5F;
    private static final float SPEED_MULTIPLIER_WHEN_INVESTIGATING = 0.7F;
    private static final float SPEED_MULTIPLIER_WHEN_FIGHTING = 1.2F;
    private static final int MELEE_ATTACK_COOLDOWN = 18;
    private static final int DISTURBANCE_LOCATION_EXPIRY_TIME = 100;
    private static final List<SensorType<? extends Sensor<? super NMSVexEntity>>> SENSOR_TYPES = List.of(SensorType.NEAREST_PLAYERS, VexEntitySensor.VEX_ENTITY_SENSOR_TYPE);
    private static final List<MemoryModuleType<?>> MEMORY_TYPES = List.of(
        MemoryModuleType.NEAREST_LIVING_ENTITIES,
        MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES,
        MemoryModuleType.NEAREST_VISIBLE_PLAYER,
        MemoryModuleType.NEAREST_VISIBLE_ATTACKABLE_PLAYER,
        MemoryModuleType.NEAREST_VISIBLE_NEMESIS,
        MemoryModuleType.LOOK_TARGET,
        MemoryModuleType.WALK_TARGET,
        MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE,
        MemoryModuleType.PATH,
        MemoryModuleType.ATTACK_TARGET,
        MemoryModuleType.ATTACK_COOLING_DOWN,
        MemoryModuleType.NEAREST_ATTACKABLE,
        MemoryModuleType.DISTURBANCE_LOCATION,
        MemoryModuleType.TOUCH_COOLDOWN,
        MemoryModuleType.VIBRATION_COOLDOWN
    );

    public static void updateActivity(final @NotNull NMSVexEntity nmsVex) {
        nmsVex.getBrain()
            .setActiveActivityToFirstValid(
                ImmutableList.of(Activity.FIGHT, Activity.INVESTIGATE, Activity.IDLE)
            );
    }

    protected static Brain<?> makeBrain(final @NotNull NMSVexEntity nmsVex, final @NotNull Dynamic<?> dynamic) {
        Brain.Provider<NMSVexEntity> provider = Brain.provider(MEMORY_TYPES, SENSOR_TYPES);
        Brain<NMSVexEntity> brain = provider.makeBrain(dynamic);
        initCoreActivity(brain);
        initIdleActivity(brain);
        initFightActivity(nmsVex, brain);
        initInvestigateActivity(brain);
        brain.setCoreActivities(ImmutableSet.of(Activity.CORE));
        brain.setDefaultActivity(Activity.IDLE);
        brain.useDefaultActivity();
        return brain;
    }

    private static void initCoreActivity(final @NotNull Brain<NMSVexEntity> brain) {
        brain.addActivity(
            Activity.CORE, 0, ImmutableList.of(new Swim<>(0.8F), SetVexLookTargetBehavior.create(), new LookAtTargetSink(45, 90), new MoveToTargetSink())
        );
    }

    private static void initIdleActivity(final @NotNull Brain<NMSVexEntity> brain) {
        brain.addActivity(
            Activity.IDLE,
            10,
            ImmutableList.of(
                new RunOne<>(
                    ImmutableList.of(Pair.of(RandomStrollInRegionBehavior.fly(SPEED_MULTIPLIER_WHEN_IDLING), 2), Pair.of(new DoNothing(30, 60), 1))
                )
            )
        );
    }

    private static void initInvestigateActivity(final @NotNull Brain<NMSVexEntity> brain) {
        brain.addActivityAndRemoveMemoryWhenStopped(
            Activity.INVESTIGATE,
            5,
            ImmutableList.of(GoToTargetLocation.create(MemoryModuleType.DISTURBANCE_LOCATION, 2, SPEED_MULTIPLIER_WHEN_INVESTIGATING)),
            MemoryModuleType.DISTURBANCE_LOCATION
        );
    }

    private static void initFightActivity(final @NotNull NMSVexEntity nmsVex, final @NotNull Brain<NMSVexEntity> brain) {
        brain.addActivityAndRemoveMemoryWhenStopped(
            Activity.FIGHT,
            10,
            ImmutableList.of(
                new VexChargeAttackBehavior(),
                StopAttackingIfTargetInvalid.create(
                    (world, entity) -> !nmsVex.canTargetEntity(entity), (world, entity, target) -> {
                    }, false
                ),
                SetEntityLookTarget.create(entity -> isTarget(nmsVex, entity), (float) nmsVex.getAttributeValue(Attributes.FOLLOW_RANGE)),
                SetWalkTargetFromAttackTargetIfTargetOutOfReach.create(SPEED_MULTIPLIER_WHEN_FIGHTING),
                MeleeAttack.create(10)
            ),
            MemoryModuleType.ATTACK_TARGET
        );
    }

    private static boolean isTarget(@NotNull NMSVexEntity nmsVex, LivingEntity entity) {
        return nmsVex.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET).filter(entityx -> entityx == entity).isPresent();
    }
}

