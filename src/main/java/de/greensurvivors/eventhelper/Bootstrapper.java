package de.greensurvivors.eventhelper;

import de.greensurvivors.eventhelper.modules.ghost.ghostEntity.GhostNMSEntity;
import io.papermc.paper.plugin.bootstrap.BootstrapContext;
import io.papermc.paper.plugin.bootstrap.PluginBootstrap;
import net.minecraft.world.entity.EntityType;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("UnstableApiUsage") // paper plugins
public class Bootstrapper implements PluginBootstrap {

    @Override
    public void bootstrap(@NotNull BootstrapContext context) { // todo use RegistryAccess on 1.21+
        // register type before registries are frozen
        EntityType<GhostNMSEntity> type = GhostNMSEntity.GHOST_TYPE; // call to static

        context.getLogger().debug("Successfully registered new entity type!");
    }
}
