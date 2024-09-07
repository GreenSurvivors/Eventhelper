package de.greensurvivors.eventhelper.modules.tnt;

import de.greensurvivors.eventhelper.EventHelper;
import de.greensurvivors.eventhelper.modules.AModulConfig;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

public class TNTKnockbackConfig extends AModulConfig<TNTKnockbackModul> {

    public TNTKnockbackConfig(@NotNull EventHelper plugin) {
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
