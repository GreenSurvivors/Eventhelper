package de.greensurvivors.eventhelper;

import de.greensurvivors.eventhelper.modules.ghost.ghostentity.NMSGhostEntity;
import de.greensurvivors.eventhelper.modules.ghost.ghostentity.NMSUnderWorldGhostEntity;
import de.greensurvivors.eventhelper.modules.ghost.ghostentity.NearestAlivePlayerSensor;
import de.greensurvivors.eventhelper.modules.ghost.vex.NMSVexEntity;
import de.greensurvivors.eventhelper.modules.ghost.vex.VexEntitySensor;
import io.papermc.paper.plugin.bootstrap.BootstrapContext;
import io.papermc.paper.plugin.bootstrap.PluginBootstrap;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.sensing.SensorType;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings({"UnstableApiUsage", "unused"}) // paper plugins
public class Bootstrapper implements PluginBootstrap {

    @Override
    public void bootstrap(@NotNull BootstrapContext context) { // todo use RegistryAccess on 1.21+
        // register types before registries are frozen
        EntityType<NMSGhostEntity> ghostType = NMSGhostEntity.GHOST_TYPE; // call to static
        EntityType<NMSUnderWorldGhostEntity> underWorldType = NMSUnderWorldGhostEntity.UNDERWORLD_GHOST_TYPE; // call to static
        SensorType<NearestAlivePlayerSensor> someSensor = NearestAlivePlayerSensor.NEAREST_ALIVE_PLAYER_SENSOR;
        EntityType<NMSVexEntity> vexType = NMSVexEntity.VEX_TYPE; // call to static
        SensorType<VexEntitySensor> vexEntitySensorType = VexEntitySensor.VEX_ENTITY_SENSOR_TYPE;

        context.getLogger().debug("Successfully registered new entity and sensor types!");
        context.getLogger().info("You can safely ignore the following message, about the ghost entity missing attributes. " +
            "It does in fact has attributes, we just can't register a supplier the way mojang does.");
    }
}
