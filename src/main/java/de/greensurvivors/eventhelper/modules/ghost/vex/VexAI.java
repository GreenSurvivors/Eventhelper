package de.greensurvivors.eventhelper.modules.ghost.vex;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Dynamic;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.util.Unit;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.behavior.*;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.sensing.Sensor;
import net.minecraft.world.entity.ai.sensing.SensorType;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;

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

    public static void updateActivity(VexNMSEntity nmsVex) {
        nmsVex.getBrain()
            .setActiveActivityToFirstValid(
                ImmutableList.of(Activity.FIGHT, Activity.INVESTIGATE, Activity.IDLE)
            );
    }

    protected static Brain<?> makeBrain(VexNMSEntity nmsVex, Dynamic<?> dynamic) {
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

    private static void initCoreActivity(Brain<VexNMSEntity> brain) {
        brain.addActivity(
            Activity.CORE, 0, ImmutableList.of(new Swim(0.8F), SetVexLookTarget.create(), new LookAtTargetSink(45, 90), new MoveToTargetSink())
        );
    }

    private static void initIdleActivity(Brain<VexNMSEntity> brain) {
        brain.addActivity(
            Activity.IDLE,
            10,
            ImmutableList.of(
                new RunOne<>(
                    ImmutableList.of(Pair.of(RandomStroll.fly(0.5F), 2), Pair.of(new DoNothing(30, 60), 1))
                )
            )
        );
    }

    private static void initInvestigateActivity(Brain<VexNMSEntity> brain) {
        brain.addActivityAndRemoveMemoryWhenStopped(
            Activity.INVESTIGATE,
            5,
            ImmutableList.of(GoToTargetLocation.create(MemoryModuleType.DISTURBANCE_LOCATION, 2, 0.7F)),
            MemoryModuleType.DISTURBANCE_LOCATION
        );
    }

    private static void initFightActivity(VexNMSEntity nmsVex, Brain<VexNMSEntity> brain) {
        brain.addActivityAndRemoveMemoryWhenStopped(
            Activity.FIGHT,
            10,
            ImmutableList.of(
                new VexChargeAttackBehavior(),
                StopAttackingIfTargetInvalid.create(
                    entity -> !nmsVex.getAngerLevel().isAngry() || !nmsVex.canTargetEntity(entity), VexAI::onTargetInvalid, false
                ),
                SetEntityLookTarget.create(entity -> isTarget(nmsVex, entity), (float) nmsVex.getAttributeValue(Attributes.FOLLOW_RANGE)),
                SetWalkTargetFromAttackTargetIfTargetOutOfReach.create(1.2F),
                MeleeAttack.create(10)
            ),
            MemoryModuleType.ATTACK_TARGET
        );
    }

    private static boolean isTarget(@NotNull VexNMSEntity nmsVex, LivingEntity entity) {
        return nmsVex.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET).filter(entityx -> entityx == entity).isPresent();
    }

    private static void onTargetInvalid(VexNMSEntity nmsVex, LivingEntity suspect) {
        if (!nmsVex.canTargetEntity(suspect)) {
            nmsVex.clearAnger(suspect);
        }
    }

    public static void setDisturbanceLocation(VexNMSEntity nmsVex, BlockPos pos) {
        if (nmsVex.level().getWorldBorder().isWithinBounds(pos)
            && !nmsVex.getEntityAngryAt().isPresent()
            && !nmsVex.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET).isPresent()) {
            nmsVex.getBrain().setMemoryWithExpiry(MemoryModuleType.SNIFF_COOLDOWN, Unit.INSTANCE, 100L);
            nmsVex.getBrain().setMemoryWithExpiry(MemoryModuleType.LOOK_TARGET, new BlockPosTracker(pos), 100L);
            nmsVex.getBrain().setMemoryWithExpiry(MemoryModuleType.DISTURBANCE_LOCATION, pos, DISTURBANCE_LOCATION_EXPIRY_TIME);
            nmsVex.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
        }
    }

    protected static class VexChargeAttackBehavior extends Behavior<VexNMSEntity> {
        public VexChargeAttackBehavior() {
            super(ImmutableMap.of(
                MemoryModuleType.ATTACK_TARGET,
                MemoryStatus.VALUE_PRESENT));
        }

        @Override
        protected boolean checkExtraStartConditions(@NotNull ServerLevel world, @NotNull VexNMSEntity nmsVex) {
            LivingEntity entityliving = nmsVex.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET).get();

            return entityliving.isAlive() && !nmsVex.getMoveControl().hasWanted() && nmsVex.random.nextInt(Mth.positiveCeilDiv(7, 2)) == 0 && nmsVex.distanceToSqr(entityliving) > 4.0D;
        }

        @Override
        protected boolean canStillUse(@NotNull ServerLevel world, @NotNull VexNMSEntity nmsVex, long time) {
            Optional<LivingEntity> optionalTarget = nmsVex.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET);

            return nmsVex.getMoveControl().hasWanted() && nmsVex.isCharging() &&
                optionalTarget.isPresent() && optionalTarget.get().isAlive();
        }

        @Override
        protected void start(@NotNull ServerLevel world, @NotNull VexNMSEntity nmsVex, long time) {
            Optional<LivingEntity> optionalTarget = nmsVex.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET);

            if (optionalTarget.isPresent()) {
                Vec3 vec3d = optionalTarget.get().getEyePosition();

                nmsVex.getMoveControl().setWantedPosition(vec3d.x, vec3d.y, vec3d.z, 1.0D);
            }

            nmsVex.setIsCharging(true);
            nmsVex.playSound(SoundEvents.VEX_CHARGE, 1.0F, 1.0F);
        }

        @Override
        protected void stop(@NotNull ServerLevel serverLevel, @NotNull VexNMSEntity nmsVex, long l) {
            nmsVex.setIsCharging(false);
        }

        @Override
        protected void tick(@NotNull ServerLevel world, @NotNull VexNMSEntity nmsVex, long time) {
            Optional<LivingEntity> optionalTarget = nmsVex.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET);

            if (optionalTarget.isPresent()) {
                final LivingEntity target = optionalTarget.get();

                if (nmsVex.getBoundingBox().intersects(target.getBoundingBox())) {
                    nmsVex.doHurtTarget(target);
                    nmsVex.setIsCharging(false);
                } else {
                    double d0 = nmsVex.distanceToSqr(target);

                    if (d0 < 9.0D) {
                        Vec3 vec3d = target.getEyePosition();

                        nmsVex.getMoveControl().setWantedPosition(vec3d.x, vec3d.y, vec3d.z, 1.0D);
                    }
                }
            }
        }
    }

    protected static class SetVexLookTarget {
        public static BehaviorControl<LivingEntity> create() {
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
}

