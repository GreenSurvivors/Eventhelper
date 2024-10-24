package de.greensurvivors.eventhelper.modules.ghost.vex;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Dynamic;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Unit;
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
    private static final List<SensorType<? extends Sensor<? super VexNMSEntity>>> SENSOR_TYPES = List.of(SensorType.NEAREST_PLAYERS, VexEntitySensor.VEX_ENTITY_SENSOR_TYPE);
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
        MemoryModuleType.RECENT_PROJECTILE,
        MemoryModuleType.TOUCH_COOLDOWN,
        MemoryModuleType.VIBRATION_COOLDOWN
    );

    public static void updateActivity(final @NotNull VexNMSEntity nmsVex) {
        nmsVex.getBrain()
            .setActiveActivityToFirstValid(
                ImmutableList.of(Activity.FIGHT, Activity.INVESTIGATE, Activity.IDLE)
            );
    }

    protected static Brain<?> makeBrain(final @NotNull VexNMSEntity nmsVex, final @NotNull Dynamic<?> dynamic) {
        Brain.Provider<VexNMSEntity> provider = Brain.provider(MEMORY_TYPES, SENSOR_TYPES);
        Brain<VexNMSEntity> brain = provider.makeBrain(dynamic);
        initCoreActivity(brain);
        initIdleActivity(brain);
        initFightActivity(nmsVex, brain);
        initInvestigateActivity(brain);
        brain.setCoreActivities(ImmutableSet.of(Activity.CORE));
        brain.setDefaultActivity(Activity.IDLE);
        brain.useDefaultActivity();
        return brain;
    }

    private static void initCoreActivity(final @NotNull Brain<VexNMSEntity> brain) {
        brain.addActivity(
            Activity.CORE, 0, ImmutableList.of(new Swim(0.8F), SetVexLookTargetBehavior.create(), new LookAtTargetSink(45, 90), new MoveToTargetSink())
        );
    }

    private static void initIdleActivity(final @NotNull Brain<VexNMSEntity> brain) {
        brain.addActivity(
            Activity.IDLE,
            10,
            ImmutableList.of(
                new RunOne<>(
                    ImmutableList.of(Pair.of(RandomStroll.fly(SPEED_MULTIPLIER_WHEN_IDLING), 2), Pair.of(new DoNothing(30, 60), 1))
                )
            )
        );
    }

    private static void initInvestigateActivity(final @NotNull Brain<VexNMSEntity> brain) {
        brain.addActivityAndRemoveMemoryWhenStopped(
            Activity.INVESTIGATE,
            5,
            ImmutableList.of(GoToTargetLocation.create(MemoryModuleType.DISTURBANCE_LOCATION, 2, SPEED_MULTIPLIER_WHEN_INVESTIGATING)),
            MemoryModuleType.DISTURBANCE_LOCATION
        );
    }

    private static void initFightActivity(final @NotNull VexNMSEntity nmsVex, final @NotNull Brain<VexNMSEntity> brain) {
        brain.addActivityAndRemoveMemoryWhenStopped(
            Activity.FIGHT,
            10,
            ImmutableList.of(
                new VexChargeAttackBehavior(),
                StopAttackingIfTargetInvalid.create(
                    entity -> !nmsVex.getAngerLevel().isAngry() || !nmsVex.canTargetEntity(entity), VexAI::onTargetInvalid, false
                ),
                SetEntityLookTarget.create(entity -> isTarget(nmsVex, entity), (float) nmsVex.getAttributeValue(Attributes.FOLLOW_RANGE)),
                SetWalkTargetFromAttackTargetIfTargetOutOfReach.create(SPEED_MULTIPLIER_WHEN_FIGHTING),
                MeleeAttack.create(10)
            ),
            MemoryModuleType.ATTACK_TARGET
        );
    }

    private static boolean isTarget(@NotNull VexNMSEntity nmsVex, LivingEntity entity) {
        return nmsVex.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET).filter(entityx -> entityx == entity).isPresent();
    }

    private static void onTargetInvalid(final @NotNull VexNMSEntity nmsVex, final @NotNull LivingEntity suspect) {
        if (!nmsVex.canTargetEntity(suspect)) {
            nmsVex.clearAnger(suspect);
        }
    }

    @SuppressWarnings("resource") // ignore level being auto closeable
    public static void setDisturbanceLocation(final @NotNull VexNMSEntity nmsVex, final @NotNull BlockPos pos) {
        if (nmsVex.level().getWorldBorder().isWithinBounds(pos)
            && nmsVex.getEntityAngryAt().isEmpty()
            && nmsVex.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET).isEmpty()) {
            nmsVex.getBrain().setMemoryWithExpiry(MemoryModuleType.SNIFF_COOLDOWN, Unit.INSTANCE, 100L);
            nmsVex.getBrain().setMemoryWithExpiry(MemoryModuleType.LOOK_TARGET, new BlockPosTracker(pos), 100L);
            nmsVex.getBrain().setMemoryWithExpiry(MemoryModuleType.DISTURBANCE_LOCATION, pos, DISTURBANCE_LOCATION_EXPIRY_TIME);
            nmsVex.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
        }
    }
}

