package de.greensurvivors.eventhelper.modules.ghost.ghostentity;

import com.google.common.collect.ImmutableMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.ai.behavior.Behavior;
import org.jetbrains.annotations.NotNull;

public class GhostSwimBehavior extends Behavior<NMSGhostEntity> {
    private final float chance;

    public GhostSwimBehavior(float chance) {
        super(ImmutableMap.of());
        this.chance = chance;
    }

    @Override
    protected boolean checkExtraStartConditions(final @NotNull ServerLevel world, final @NotNull NMSGhostEntity ghost) {
        return ghost.underWorldGhost.isInWater() && ghost.underWorldGhost.getFluidHeight(FluidTags.WATER) > ghost.underWorldGhost.getFluidJumpThreshold() || ghost.underWorldGhost.isInLava();
    }

    @Override
    protected boolean canStillUse(final @NotNull ServerLevel world, final @NotNull NMSGhostEntity ghost, long ignored) {
        return this.checkExtraStartConditions(world, ghost);
    }

    @Override
    protected void tick(final @NotNull ServerLevel serverLevel, final @NotNull NMSGhostEntity ghost, long ignored) {
        if (ghost.getRandom().nextFloat() < this.chance) {
            ghost.getJumpControl().jump();
        }
    }
}
