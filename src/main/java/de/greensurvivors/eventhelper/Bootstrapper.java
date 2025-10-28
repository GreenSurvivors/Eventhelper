package de.greensurvivors.eventhelper;

import de.greensurvivors.eventhelper.modules.ghost.ghostentity.NMSGhostEntity;
import de.greensurvivors.eventhelper.modules.ghost.ghostentity.NMSUnderWorldGhostEntity;
import de.greensurvivors.eventhelper.modules.ghost.ghostentity.NearestAlivePlayerSensor;
import de.greensurvivors.eventhelper.modules.ghost.vex.NMSVexEntity;
import de.greensurvivors.eventhelper.modules.ghost.vex.VexEntitySensor;
import io.papermc.paper.plugin.bootstrap.BootstrapContext;
import io.papermc.paper.plugin.bootstrap.PluginBootstrap;
import io.papermc.paper.registry.event.RegistryEvents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.sensing.SensorType;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings({"UnstableApiUsage", "unused"}) // paper plugins
public class Bootstrapper implements PluginBootstrap {

    @Override
    public void bootstrap(final @NotNull BootstrapContext context) {
        // Register a new handler for the freeze lifecycle event on the game_event registry.
        // game_event happens slightly before entity_type and way before sensor_type
        context.getLifecycleManager().registerEventHandler(RegistryEvents.GAME_EVENT.freeze().newHandler(event -> { // todo make use of compose() in a future version. Also look for an RegistryEvents.Entity_TYPE. At time of writing it isn't implemented yet!

            // call to static for the entities to register themselves as Entity_Type's
            EntityType<NMSGhostEntity> ghostType = NMSGhostEntity.GHOST_TYPE;
            EntityType<NMSUnderWorldGhostEntity> underWorldType = NMSUnderWorldGhostEntity.UNDERWORLD_GHOST_TYPE;
            EntityType<NMSVexEntity> vexType = NMSVexEntity.VEX_TYPE;

            SensorType<NearestAlivePlayerSensor> someSensor = NearestAlivePlayerSensor.NEAREST_ALIVE_PLAYER_SENSOR;
            SensorType<VexEntitySensor> vexEntitySensorType = VexEntitySensor.VEX_ENTITY_SENSOR_TYPE;

            context.getLogger().debug("Successfully registered new entity and sensor types!");
        }));

        context.getLogger().info("You can safely ignore the following message, about the ghost entity missing attributes. " +
            "It does in fact has attributes, we just can't register a supplier the way mojang does.");
    }
}
