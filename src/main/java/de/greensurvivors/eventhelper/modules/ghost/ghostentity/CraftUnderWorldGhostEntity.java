package de.greensurvivors.eventhelper.modules.ghost.ghostentity;

import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.craftbukkit.entity.CraftMob;
import org.jetbrains.annotations.NotNull;


public class CraftUnderWorldGhostEntity extends CraftMob {
    public CraftUnderWorldGhostEntity(final @NotNull CraftServer server, final @NotNull NMSUnderWorldGhostEntity entity) {
        super(server, entity);
    }

    @Override
    public NMSUnderWorldGhostEntity getHandle() {
        return (NMSUnderWorldGhostEntity) this.entity;
    }

    @Override
    public String toString() {
        return "CraftUnderworldGhost";
    }
}
