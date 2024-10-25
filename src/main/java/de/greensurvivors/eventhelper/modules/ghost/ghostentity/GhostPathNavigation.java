package de.greensurvivors.eventhelper.modules.ghost.ghostentity;

import de.greensurvivors.eventhelper.EventHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.Path;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class GhostPathNavigation extends GroundPathNavigation {
    private final @NotNull NMSGhostEntity ghostNMS;

    public GhostPathNavigation(final @NotNull NMSUnderWorldGhostEntity underWorldGhostNMS,
                               final @NotNull NMSGhostEntity ghostNMS,
                               final @NotNull Level world) {
        super(underWorldGhostNMS, world);
        this.ghostNMS = ghostNMS;
    }

    public Path createPath(final @NotNull BlockPos target, final @Nullable Entity entity, final int distance) {
        EventHelper.getPlugin().getComponentLogger().info("pathfinding to {}, entity: {}, distance: {}", target, entity, distance);

        return super.createPath(target.atY((int) (target.getY() - ghostNMS.ghostGame.getConfig().getPathfindOffset())), entity, distance);
    }
}
