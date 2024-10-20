package de.greensurvivors.eventhelper.modules.ghost.vex;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.sensing.NearestLivingEntitySensor;
import net.minecraft.world.entity.ai.sensing.Sensor;
import net.minecraft.world.entity.ai.sensing.SensorType;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class VexEntitySensor extends NearestLivingEntitySensor<VexNMSEntity> {
    public static final SensorType<VexEntitySensor> VEX_ENTITY_SENSOR_TYPE = register();

    private static <U extends Sensor<?>> SensorType<U> register() {
        Constructor<SensorType> constructor;
        try {
            constructor = SensorType.class.getDeclaredConstructor(Supplier.class);
            constructor.setAccessible(true);
            SensorType<U> sensorType = constructor.newInstance((Supplier<VexEntitySensor>) VexEntitySensor::new);

            return Registry.register(BuiltInRegistries.SENSOR_TYPE, new ResourceLocation("piglin_specific_sensor"), sensorType);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException |
                 InstantiationException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Set<MemoryModuleType<?>> requires() {
        return ImmutableSet.copyOf(Iterables.concat(super.requires(), List.of(MemoryModuleType.NEAREST_ATTACKABLE)));
    }

    @Override
    protected void doTick(ServerLevel world, VexNMSEntity entity) {
        super.doTick(world, entity);
        getClosest(entity, entityx -> entityx.getType() == EntityType.PLAYER)
            .or(() -> getClosest(entity, entityx -> entityx.getType() != EntityType.PLAYER))
            .ifPresentOrElse(
                entityx -> entity.getBrain().setMemory(MemoryModuleType.NEAREST_ATTACKABLE, entityx),
                () -> entity.getBrain().eraseMemory(MemoryModuleType.NEAREST_ATTACKABLE)
            );
    }

    private static Optional<LivingEntity> getClosest(VexNMSEntity warden, Predicate<LivingEntity> targetPredicate) {
        return warden.getBrain()
            .getMemory(MemoryModuleType.NEAREST_LIVING_ENTITIES)
            .stream()
            .flatMap(Collection::stream)
            .filter(warden::canTargetEntity)
            .filter(targetPredicate)
            .findFirst();
    }

    @Override
    protected int radiusXZ() {
        return 24;
    }

    @Override
    protected int radiusY() {
        return 24;
    }
}