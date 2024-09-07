package de.greensurvivors.eventhelper.config;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

/**
 * represents a configurable option, with its path pointing to
 * please note: While fallback values are defined here, these are in fact NOT the default options.
 * They are just used in the unfortunate case loading them goes wrong.
 *
 * @param <T> Type of the config value
 */
public class ConfigOption<T> {
    private final @NotNull String path;
    private final @NotNull T fallbackValue;
    private final @NotNull AtomicReference<@Nullable T> value = new AtomicReference<>(null);

    public ConfigOption(@NotNull String path, @NotNull T fallbackValue) {
        this.path = path;
        this.fallbackValue = fallbackValue;
    }

    public @NotNull String getPath() {
        return path;
    }

    public @NotNull T getFallback() {
        return fallbackValue;
    }

    /**
     * returns the value if not null, else the fallback
     */
    public @NotNull T getValueOrFallback() {
        return Objects.requireNonNullElse(this.value.get(), fallbackValue);
    }

    public void setValue(@Nullable T value) {
        this.value.set(value);
    }
}
