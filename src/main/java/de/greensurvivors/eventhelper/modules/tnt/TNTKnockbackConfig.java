package de.greensurvivors.eventhelper.modules.tnt;

import de.greensurvivors.eventhelper.EventHelper;
import de.greensurvivors.eventhelper.modules.AModulConfig;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.util.concurrent.CompletableFuture;


public class TNTKnockbackConfig extends AModulConfig<TNTKnockbackModul> {

    public TNTKnockbackConfig(@NotNull EventHelper plugin) {
        super(plugin);
    }

    @Override
    public @NotNull CompletableFuture<@NotNull Boolean> reload() {
        final CompletableFuture<Boolean> runAfter = new CompletableFuture<>();

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            synchronized (this) {
                if (this.modul != null) {
                    plugin.saveResource(modul.getName() + "/" + configPath.getFileName().toString(), false);

                    try (BufferedReader bufferedReader = Files.newBufferedReader(configPath)) {
                        @NotNull YamlConfiguration config = YamlConfiguration.loadConfiguration(bufferedReader);

                        isEnabled.setValue(config.getBoolean(isEnabled.getPath()));
                        runAfter.complete(isEnabled.getValueOrFallback());
                    } catch (IOException e) {
                        plugin.getComponentLogger().error("Could not load modul config for {} from file!", modul.getName(), e);

                        isEnabled.setValue(Boolean.FALSE);
                        runAfter.complete(Boolean.FALSE);
                    }
                } else {
                    plugin.getComponentLogger().error("Could not load modul config, since the module of {} was not set!", this.getClass().getName());

                    isEnabled.setValue(Boolean.FALSE);
                    runAfter.complete(Boolean.FALSE);
                }
            }
        });

        return runAfter;
    }
}
