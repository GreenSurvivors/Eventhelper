package de.greensurvivors.eventhelper.modules.ghost;

import de.greensurvivors.eventhelper.EventHelper;
import de.greensurvivors.eventhelper.command.MainCmd;
import de.greensurvivors.eventhelper.modules.AModul;
import de.greensurvivors.eventhelper.modules.ghost.command.GhostCmd;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.PluginManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

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
        GhostLangPath.moduleName = getName(); // set here, not in constructor, so this class can expanded by another one if ever needed in the future
        games.clear();

        try (Stream<Path> stream = Files.list(plugin.getDataFolder().toPath().resolve(getName()))) { // stream need to get closed
            PluginManager pluginManager = Bukkit.getPluginManager();

            stream.filter(Files::isDirectory).
                map(path -> new GhostGame(plugin, this, path.getFileName().toString())). // create a new game instance
                forEach(game -> {
                // register game
                games.put(game.getName_id().toLowerCase(Locale.ENGLISH), game);
                pluginManager.registerEvents(game, plugin);

                // reload game
                game.getConfig().reload();
            });
        } catch (IOException e) {
            plugin.getComponentLogger().error("could open ghost game directories.", e);
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

    public @NotNull Set<String> getGameNameIds() {
        return games.keySet();
    }

    public @Nullable GhostGame getGameOfPlayer(@NotNull Player player) {
        for (GhostGame game : games.values()) {
            if (game.isPlaying(player)) {
                return game;
            }
        }

        return null;
    }
}
