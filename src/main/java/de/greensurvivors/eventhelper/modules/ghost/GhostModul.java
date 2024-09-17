package de.greensurvivors.eventhelper.modules.ghost;

import de.greensurvivors.eventhelper.EventHelper;
import de.greensurvivors.eventhelper.command.MainCmd;
import de.greensurvivors.eventhelper.modules.AModul;
import de.greensurvivors.eventhelper.modules.ghost.command.GhostGame;
import org.bukkit.Bukkit;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public class GhostModul extends AModul<GeneralGhostConfig> {
    private final @NotNull Map<@NotNull String, GhostGame> games = new HashMap<>();

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
        for (GhostGame ghostGame : games.values()) {
            Bukkit.getPluginManager().registerEvents(ghostGame, plugin);
        }
    }

    @Override
    public void onDisable() {
        for (GhostGame ghostGame : games.values()) {
            ghostGame.resetGame();
            HandlerList.unregisterAll(ghostGame);
        }
    }

    public @Nullable GhostGame getGameByName(final @NotNull String gameName) {
        return games.get(gameName);
    }
}
