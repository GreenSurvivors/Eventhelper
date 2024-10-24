package de.greensurvivors.eventhelper.modules.ghost.ghostentity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.Path;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class GhostPathNavigation extends GroundPathNavigation {
    private final @NotNull GhostNMSEntity ghostNMS;

    public GhostPathNavigation(UnderWorldGhostNMSEntity underWorldGhostNMS, @NotNull GhostNMSEntity ghostNMS, Level world) {
        super(underWorldGhostNMS, world);
        this.ghostNMS = ghostNMS;
    }

    public Path createPath(BlockPos target, @Nullable Entity entity, int distance) {
        return super.createPath(target.atY((int) (target.getY() - ghostNMS.ghostGame.getConfig().getPathfindOffset())), entity, distance);
    }
}
