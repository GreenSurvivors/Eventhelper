package de.greensurvivors.eventhelper.modules.ghost.vex;

import com.google.common.collect.ImmutableMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

class VexChargeAttackBehavior extends Behavior<NMSVexEntity> {
    public VexChargeAttackBehavior() {
        super(ImmutableMap.of(
            MemoryModuleType.ATTACK_TARGET,
            MemoryStatus.VALUE_PRESENT));
    }

    @Override
    protected boolean checkExtraStartConditions(final @NotNull ServerLevel world, final @NotNull NMSVexEntity nmsVex) {
        LivingEntity entityliving = nmsVex.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET).get();

        return entityliving.isAlive() && !nmsVex.getMoveControl().hasWanted() && nmsVex.random.nextInt(Mth.positiveCeilDiv(7, 2)) == 0 && nmsVex.distanceToSqr(entityliving) > 4.0D;
    }

    @Override
    protected boolean canStillUse(final @NotNull ServerLevel world, final @NotNull NMSVexEntity nmsVex, final long time) {
        Optional<LivingEntity> optionalTarget = nmsVex.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET);

        return nmsVex.getMoveControl().hasWanted() && nmsVex.isCharging() &&
            optionalTarget.isPresent() && optionalTarget.get().isAlive();
    }

    @Override
    protected void start(final @NotNull ServerLevel world, final @NotNull NMSVexEntity nmsVex, final long time) {
        Optional<LivingEntity> optionalTarget = nmsVex.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET);

        if (optionalTarget.isPresent()) {
            Vec3 vec3d = optionalTarget.get().getEyePosition();

            nmsVex.getMoveControl().setWantedPosition(vec3d.x, vec3d.y, vec3d.z, 1.0D);
        }

        nmsVex.setIsCharging(true);
        nmsVex.playSound(SoundEvents.VEX_CHARGE, 1.0F, 1.0F);
    }

    @Override
    protected void stop(final @NotNull ServerLevel serverLevel, final @NotNull NMSVexEntity nmsVex, final long time) {
        nmsVex.setIsCharging(false);
    }

    @Override
    protected void tick(final @NotNull ServerLevel world, final @NotNull NMSVexEntity nmsVex, final long time) {
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
