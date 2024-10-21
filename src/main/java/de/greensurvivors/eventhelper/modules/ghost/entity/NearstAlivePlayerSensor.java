package de.greensurvivors.eventhelper.modules.ghost.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.NearestVisibleLivingEntities;
import net.minecraft.world.entity.ai.sensing.Sensor;
import net.minecraft.world.entity.ai.sensing.SensorType;
import net.minecraft.world.level.PathNavigationRegion;
import net.minecraft.world.level.pathfinder.Path;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_20_R3.block.CraftBlockType;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

public class NearstAlivePlayerSensor extends Sensor<GhostNMSEntity> {
    public static final SensorType<NearstAlivePlayerSensor> NEAREST_ALIVE_PLAYER_SENSOR = register();

    private static <U extends Sensor<?>> SensorType<U> register() {
        Constructor<SensorType> constructor;
        try {
            constructor = SensorType.class.getDeclaredConstructor(Supplier.class);
            constructor.setAccessible(true);
            SensorType<U> sensorType = constructor.newInstance((Supplier<NearstAlivePlayerSensor>) NearstAlivePlayerSensor::new);

            return Registry.register(BuiltInRegistries.SENSOR_TYPE, new ResourceLocation("piglin_specific_sensor"), sensorType);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException |
                 InstantiationException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public @NotNull Set<MemoryModuleType<?>> requires() {
        return Set.of(
            MemoryModuleType.NEAREST_ATTACKABLE,
            MemoryModuleType.NEAREST_LIVING_ENTITIES
        );
    }

    @Override
    protected void doTick(@NotNull ServerLevel world, @NotNull GhostNMSEntity entity) {
        Brain<GhostNMSEntity> brain = entity.getBrain();
        NearestVisibleLivingEntities nearestVisibleLivingEntities = brain.getMemory(MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES)
            .orElse(NearestVisibleLivingEntities.empty());

        final Material material = CraftBlockType.minecraftToBukkit(entity.underWorldGhost.getFeetBlockState().getBlock());
        final int followRangeAt = entity.ghostGame.getConfig().getFollowRangeAt(material);
        final int offsetVal = 2 * followRangeAt;
        PathNavigationRegion pathNavigationRegion = new PathNavigationRegion(world, entity.blockPosition().offset(-offsetVal, -offsetVal, -offsetVal), entity.blockPosition().offset(offsetVal, offsetVal, offsetVal));

        Map<BlockPos, LivingEntity> nearEntityLocations = new HashMap<>();
        for (LivingEntity livingEntity : nearestVisibleLivingEntities.findAll(livingEntityx -> true)) {
            nearEntityLocations.put(livingEntity.blockPosition(), livingEntity);
        }

        // I believe distance 0 means we are at the beginning and range multiple is almost always 1, except for bees for some reason
        Path path = entity.getNavigation().pathFinder.findPath(pathNavigationRegion, entity, nearEntityLocations.keySet(), followRangeAt, 0, 1.0f);

        if (path != null) {
            brain.setMemory(MemoryModuleType.NEAREST_ATTACKABLE, nearEntityLocations.get(path.getTarget()));
        } else {
            // todo no player was found
        }
    }
}
