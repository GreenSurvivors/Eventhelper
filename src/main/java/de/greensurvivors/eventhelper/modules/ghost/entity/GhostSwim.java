package de.greensurvivors.eventhelper.modules.ghost.entity;

import com.google.common.collect.ImmutableMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.ai.behavior.Behavior;
import org.jetbrains.annotations.NotNull;

public class GhostSwim extends Behavior<GhostNMSEntity> {
    private final float chance;

    public GhostSwim(float chance) {
        super(ImmutableMap.of());
        this.chance = chance;
    }

    @Override
    protected boolean checkExtraStartConditions(@NotNull ServerLevel world, GhostNMSEntity ghost) {
        return ghost.underWorldGhost.isInWater() && ghost.underWorldGhost.getFluidHeight(FluidTags.WATER) > ghost.getFluidJumpThreshold() || ghost.underWorldGhost.isInLava();
    }

    @Override
    protected boolean canStillUse(@NotNull ServerLevel world, @NotNull GhostNMSEntity ghost, long time) {
        return this.checkExtraStartConditions(world, ghost);
    }

    @Override
    protected void tick(@NotNull ServerLevel serverLevel, GhostNMSEntity ghost, long ignored) {
        if (ghost.getRandom().nextFloat() < this.chance) {
            ghost.getJumpControl().jump();
        }
    }
}
