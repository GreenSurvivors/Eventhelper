package de.greensurvivors.eventhelper.modules.ghost;

import de.greensurvivors.eventhelper.EventHelper;
import de.greensurvivors.eventhelper.command.MainCmd;
import de.greensurvivors.eventhelper.modules.AModul;
import de.greensurvivors.eventhelper.modules.ghost.command.GhostCmd;
import org.bukkit.Bukkit;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class GhostModul extends AModul<GeneralGhostConfig> {
    private final @NotNull Map<@NotNull String, GhostGame> games = new HashMap<>(); // todo remember to init with lower case names

    public GhostModul(final @NotNull EventHelper plugin) {
        super(plugin, new GeneralGhostConfig(plugin));
        this.getConfig().setModul(this);

        plugin.getMainCmd().registerSubCommand(new GhostCmd(plugin, MainCmd.getParentPermission(), this));
    }

    @Override
    public @NotNull String getName() {
        return "ghost";
    }

    @Override
    public void onEnable() {
        for (GhostGame ghostGame : games.values()) {
            Bukkit.getPluginManager().registerEvents(ghostGame, plugin);
            ghostGame.getConfig().reload();
        }
    }

    @Override
    public void onDisable() {
        for (GhostGame ghostGame : games.values()) {
            ghostGame.resetGame();
            HandlerList.unregisterAll(ghostGame);
        }
    }

    public @Nullable GhostGame createNewGame(final @NotNull String gameName) {
        if (!games.containsKey(gameName)) {
            GhostGame newGame = new GhostGame(plugin, this, gameName);

            if (config.isEnabled()) {
                Bukkit.getPluginManager().registerEvents(newGame, plugin);
                newGame.getConfig().save().thenRun(() -> newGame.getConfig().reload()); // safe to disk
            } else {
                newGame.getConfig().save(); // safe to disk
            }

            games.put(gameName.toLowerCase(Locale.ENGLISH), newGame);

            return newGame;
        } else {
            return null;
        }
    }

    public @Nullable GhostGame getGameByName(final @NotNull String gameName) {
        return games.get(gameName.toLowerCase(Locale.ENGLISH)); // todo locale??
    }

    public @NotNull Set<String> getGameNames() {
        return games.keySet();
    }
}
