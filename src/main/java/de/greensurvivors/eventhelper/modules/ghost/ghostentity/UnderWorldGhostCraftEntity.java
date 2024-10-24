package de.greensurvivors.eventhelper.modules.ghost.ghostentity;

import org.bukkit.craftbukkit.v1_20_R3.CraftServer;
import org.bukkit.craftbukkit.v1_20_R3.entity.CraftMob;
import org.jetbrains.annotations.NotNull;


public class UnderWorldGhostCraftEntity extends CraftMob {
    public UnderWorldGhostCraftEntity(final @NotNull CraftServer server, final @NotNull UnderWorldGhostNMSEntity entity) {
        super(server, entity);
    }

    @Override
    public UnderWorldGhostNMSEntity getHandle() {
        return (UnderWorldGhostNMSEntity) this.entity;
    }

    @Override
    public String toString() {
        return "CraftUnderworldGhost";
    }
}
