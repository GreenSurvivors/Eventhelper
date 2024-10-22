package de.greensurvivors.eventhelper.modules;

import de.greensurvivors.eventhelper.EventHelper;
import de.greensurvivors.eventhelper.config.ConfigOption;
import io.papermc.paper.math.Position;
import org.apache.maven.artifact.versioning.ComparableVersion;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public abstract class AModulConfig<ModulType extends AModul<?>> {
    protected final static @NotNull String VERSION_PATH = "dataVersion";
    protected final @NotNull ComparableVersion dataVersion;
    protected final @NotNull ConfigOption<Boolean> isEnabled = new ConfigOption<>("enabled", Boolean.TRUE);
    protected final @NotNull EventHelper plugin;
    protected @Nullable ModulType modul;
    protected @NotNull Path configPath;
    private @NotNull Path configSpecificPath;

    public AModulConfig(final @NotNull EventHelper plugin) {
        this(plugin, Path.of("config.yaml"));
    }

    public AModulConfig(final @NotNull EventHelper plugin, final @NotNull Path configSpecificPath) {
        this(plugin, configSpecificPath, new ComparableVersion("1.0.0"));
    }

    public AModulConfig(final @NotNull EventHelper plugin,
                        final @NotNull Path configSpecificPath,
                        final @NotNull ComparableVersion dataVersion) {
        this.plugin = plugin;
        this.configSpecificPath = configSpecificPath;
        this.dataVersion = dataVersion;
    }

    public @Nullable ModulType getModul() { // always have to be called!
        return modul;
    }

    public void setModul(final @NotNull ModulType modul) {
        this.modul = modul;
        configPath = plugin.getDataFolder().toPath().resolve(modul.getName()).resolve(configSpecificPath);
    }

    public abstract @NotNull CompletableFuture<@NotNull Boolean> reload();

    public abstract @NotNull CompletableFuture<@NotNull Boolean> save();

    public boolean isEnabled() {
        return isEnabled.getValueOrFallback();
    }

    public static @NotNull Map<String, Object> serializePosition(final @NotNull Position position) {
        return Map.of(
            VERSION_PATH, "1.0.0",
            "type", position.isBlock() ? "block" : position.isFine() ? "fine" : "unknown",
            "x", position.x(),
            "y", position.y(),
            "z", position.z()
        );
    }

    public static @NotNull Position deserializePosition(Map<String, ?> serialized) throws IllegalArgumentException {
        if (serialized.get("x") instanceof Number x) {
            if (serialized.get("y") instanceof Number y) {
                if (serialized.get("z") instanceof Number z) {
                    if (serialized.get("type") instanceof String type) {
                        if (type.equalsIgnoreCase("block")) {
                            return Position.block(x.intValue(), y.intValue(), z.intValue());
                        } else if (type.equalsIgnoreCase("fine")) {
                            return Position.fine(x.doubleValue(), y.doubleValue(), z.doubleValue());
                        } else {
                            throw new IllegalArgumentException("Argument " + type + " is not a position type.");
                        }
                    } else {
                        throw new IllegalArgumentException("Argument " + serialized.get(ConfigurationSerialization.SERIALIZED_TYPE_KEY) + " is not a position type.");
                    }
                } else {
                    throw new IllegalArgumentException("Argument " + serialized.get("z") + " is not a number.");
                }
            } else {
                throw new IllegalArgumentException("Argument " + serialized.get("y") + " is not a number.");
            }
        } else {
            throw new IllegalArgumentException("Argument " + serialized.get("x") + " is not a number.");
        }
    }
}
