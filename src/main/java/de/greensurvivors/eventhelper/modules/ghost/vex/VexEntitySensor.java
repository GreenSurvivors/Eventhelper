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
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class VexEntitySensor extends NearestLivingEntitySensor<NMSVexEntity> {
    public static final @NotNull SensorType<VexEntitySensor> VEX_ENTITY_SENSOR_TYPE = register();

    private static <U extends Sensor<?>> @NotNull SensorType<U> register() {
        Constructor<SensorType> constructor;
        try {
            constructor = SensorType.class.getDeclaredConstructor(Supplier.class);
            constructor.setAccessible(true);
            SensorType<U> sensorType = constructor.newInstance((Supplier<VexEntitySensor>) VexEntitySensor::new);

            return Registry.register(BuiltInRegistries.SENSOR_TYPE, ResourceLocation.withDefaultNamespace("vex_entity_sensor"), sensorType);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException |
                 InstantiationException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public @NotNull Set<MemoryModuleType<?>> requires() {
        return ImmutableSet.copyOf(Iterables.concat(super.requires(), List.of(MemoryModuleType.NEAREST_ATTACKABLE)));
    }

    @Override
    protected void doTick(final @NotNull ServerLevel world, final @NotNull NMSVexEntity entity) {
        super.doTick(world, entity);
        getClosest(entity, entityx -> entityx.getType() == EntityType.PLAYER)
            .or(() -> getClosest(entity, entityx -> entityx.getType() != EntityType.PLAYER))
            .ifPresentOrElse(
                entityx -> entity.getBrain().setMemory(MemoryModuleType.NEAREST_ATTACKABLE, entityx),
                () -> entity.getBrain().eraseMemory(MemoryModuleType.NEAREST_ATTACKABLE)
            );
    }

    private static @NotNull Optional<LivingEntity> getClosest(final @NotNull NMSVexEntity nmsVex, final @NotNull Predicate<LivingEntity> targetPredicate) {
        return nmsVex.getBrain()
            .getMemory(MemoryModuleType.NEAREST_LIVING_ENTITIES)
            .stream()
            .flatMap(Collection::stream)
            .filter(nmsVex::canTargetEntity)
            .filter(targetPredicate)
            .findFirst();
    }
}
