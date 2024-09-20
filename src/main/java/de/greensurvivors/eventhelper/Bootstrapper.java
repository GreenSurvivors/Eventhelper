package de.greensurvivors.eventhelper;

import de.greensurvivors.eventhelper.modules.ghost.ghostEntity.GhostNMSEntity;
import de.greensurvivors.eventhelper.modules.ghost.ghostEntity.UnderWorldGhostNMSEntity;
import io.papermc.paper.plugin.bootstrap.BootstrapContext;
import io.papermc.paper.plugin.bootstrap.PluginBootstrap;
import net.minecraft.world.entity.EntityType;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings({"UnstableApiUsage", "unused"}) // paper plugins
public class Bootstrapper implements PluginBootstrap {

    @Override
    public void bootstrap(@NotNull BootstrapContext context) { // todo use RegistryAccess on 1.21+
        // register type before registries are frozen
        EntityType<GhostNMSEntity> type = GhostNMSEntity.GHOST_TYPE; // call to static
        EntityType<UnderWorldGhostNMSEntity> anotherType = UnderWorldGhostNMSEntity.UNDERWORLD_GHOST_TYPE; // call to static

        context.getLogger().debug("Successfully registered new entity type!");
        context.getLogger().info("You can safely ignore the following message, about the ghost entity missing attributes. " +
            "It does in fact has attributes, We just can't register a supplier the way mojang does.");
    }
}
