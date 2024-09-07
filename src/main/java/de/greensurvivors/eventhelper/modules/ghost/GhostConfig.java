package de.greensurvivors.eventhelper.modules.ghost;

import de.greensurvivors.eventhelper.EventHelper;
import de.greensurvivors.eventhelper.modules.AModulConfig;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

public class GhostConfig extends AModulConfig<GhostModul> {
    public GhostConfig(final @NotNull EventHelper plugin) {
        super(plugin);
    }

    @Override
    public CompletableFuture<Boolean> reload() {
        final CompletableFuture<Boolean> runAfter = new CompletableFuture<>();

        //todo
        runAfter.complete(isEnabled.getValueOrFallback());

        return runAfter;
    }
}
