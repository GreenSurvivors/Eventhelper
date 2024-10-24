package de.greensurvivors.eventhelper.modules.ghost.ghostentity;

import com.destroystokyo.paper.util.maplist.ReferenceList;
import de.greensurvivors.eventhelper.EventHelper;
import de.greensurvivors.eventhelper.modules.ghost.player.AlivePlayer;
import io.papermc.paper.util.player.NearbyPlayers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.ai.sensing.Sensor;
import net.minecraft.world.entity.ai.sensing.SensorType;
import net.minecraft.world.level.PathNavigationRegion;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_20_R3.block.CraftBlockType;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

public class NearestAlivePlayerSensor extends Sensor<GhostNMSEntity> {
    public static final @NotNull SensorType<NearestAlivePlayerSensor> NEAREST_ALIVE_PLAYER_SENSOR = register();

    private static <U extends Sensor<?>> @NotNull SensorType<U> register() {
        Constructor<SensorType> constructor;
        try {
            constructor = SensorType.class.getDeclaredConstructor(Supplier.class);
            constructor.setAccessible(true);
            SensorType<U> sensorType = constructor.newInstance((Supplier<NearestAlivePlayerSensor>) NearestAlivePlayerSensor::new);

            return Registry.register(BuiltInRegistries.SENSOR_TYPE, new ResourceLocation("nearest_alive_player_sensor"), sensorType);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException |
                 InstantiationException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public @NotNull Set<MemoryModuleType<?>> requires() {
        return Set.of(
            MemoryModuleType.ATTACK_TARGET,
            MemoryModuleType.WALK_TARGET // todo dirty fix
        );
    }

    @Override
    protected void doTick(final @NotNull ServerLevel world, final @NotNull GhostNMSEntity entity) {
        NearbyPlayers nearbyPlayers = world.chunkSource.chunkMap.getNearbyPlayers();
        Vec3 entityPos = entity.position();
        ReferenceList<ServerPlayer> nearby = nearbyPlayers.getPlayersByChunk(
            entity.chunkPosition().x,
            entity.chunkPosition().z,
            io.papermc.paper.util.player.NearbyPlayers.NearbyMapType.GENERAL_REALLY_SMALL
        );

        final Material material = CraftBlockType.minecraftToBukkit(entity.underWorldGhost.getFeetBlockState().getBlock());
        int followRangeAt = entity.ghostGame.getConfig().getFollowRangeAt(material);
        if (followRangeAt <= 0) {
            followRangeAt = 16;
        }
        double followVelocityAt = entity.ghostGame.getConfig().getFollowVelocityAt(material);
        if (followVelocityAt <= 0) {
            followVelocityAt = 0.39284D;
        }
        final int offsetVal = 2 * followRangeAt;
        PathNavigationRegion pathNavigationRegion = new PathNavigationRegion(world, entity.blockPosition().offset(-offsetVal, -offsetVal, -offsetVal), entity.blockPosition().offset(offsetVal, offsetVal, offsetVal));

        Map<BlockPos, LivingEntity> nearPlayerLocations = new HashMap<>(nearby == null ? 0 : nearby.size());
        if (nearby != null) {
            Object[] rawData = nearby.getRawData();
            for (int index = 0, len = nearby.size(); index < len; ++index) {
                ServerPlayer player = (ServerPlayer) rawData[index];

                if (!EntitySelector.NO_CREATIVE_OR_SPECTATOR.test(player)) {
                    continue;
                }

                if (player.distanceToSqr(entityPos.x, entityPos.y, entityPos.z) >= (followRangeAt * followRangeAt)) {
                    continue;
                }

                // ignore game spectators and perished players
                if (!(entity.ghostGame.getGhostGamePlayer(player.getUUID()) instanceof AlivePlayer alivePlayer)) {
                    continue;
                }

                // ignore trapped players
                if (alivePlayer.getMouseTrapTrappedIn() != null) {
                    continue;
                }

                nearPlayerLocations.put(player.blockPosition(), player);
            }
        }

        // I believe distance 0 means we are at the beginning and range multiple is almost always 1, except for bees for some reason
        Path path = entity.getNavigation().pathFinder.findPath(pathNavigationRegion, entity, nearPlayerLocations.keySet(), followRangeAt, 0, 1.0f);

        Brain<GhostNMSEntity> brain = entity.getBrain();
        if (path != null) {
            EventHelper.getPlugin().getComponentLogger().info("final path is to" + path.getTarget());

            brain.setMemory(MemoryModuleType.ATTACK_TARGET, Optional.of(nearPlayerLocations.get(path.getTarget())));
            brain.setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(nearPlayerLocations.get(path.getTarget()), (float) followVelocityAt, 1)); // todo dirty fix
        } else {
            brain.setMemory(MemoryModuleType.ATTACK_TARGET, Optional.empty());
        }
    }
}
