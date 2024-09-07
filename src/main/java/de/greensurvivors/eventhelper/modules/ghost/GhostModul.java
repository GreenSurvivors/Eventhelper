package de.greensurvivors.eventhelper.modules.ghost;

import de.greensurvivors.eventhelper.EventHelper;
import de.greensurvivors.eventhelper.command.MainCmd;
import de.greensurvivors.eventhelper.modules.AModul;
import org.jetbrains.annotations.NotNull;

public class GhostModul extends AModul<GhostConfig> {
    public GhostModul(final @NotNull EventHelper plugin) {
        super(plugin, new GhostConfig(plugin));
        this.getConfig().setModul(this);

        plugin.getMainCmd().registerSubCommand(new GhostCmd(plugin, MainCmd.getParentPermission()));
    }

    @Override
    public @NotNull String getName() {
        return "Ghost";
    }

    @Override
    public void onEnable() {

    }

    @Override
    public void onDisable() {

    }
}
