package de.greensurvivors.eventhelper.modules.ghost;

import de.greensurvivors.eventhelper.EventHelper;
import de.greensurvivors.eventhelper.command.MainCmd;
import de.greensurvivors.eventhelper.modules.AModul;
import org.jetbrains.annotations.NotNull;

public class GhostModul extends AModul<GeneralGhostConfig> {
    public GhostModul(final @NotNull EventHelper plugin) {
        super(plugin, new GeneralGhostConfig(plugin));
        this.getConfig().setModul(this);

        plugin.getMainCmd().registerSubCommand(new GhostCmd(plugin, MainCmd.getParentPermission()));
    }

    @Override
    public @NotNull String getName() {
        return "ghost";
    }

    @Override
    public void onEnable() {

    }

    @Override
    public void onDisable() {

    }
}
