package de.greensurvivors.eventhelper.modules.ghost.command.set;

import de.greensurvivors.eventhelper.EventHelper;
import de.greensurvivors.eventhelper.messages.LangPath;
import de.greensurvivors.eventhelper.messages.MessageManager;
import de.greensurvivors.eventhelper.messages.SharedLangPath;
import de.greensurvivors.eventhelper.messages.SharedPlaceHolder;
import de.greensurvivors.eventhelper.modules.ghost.GhostGame;
import de.greensurvivors.eventhelper.modules.ghost.GhostLangPath;
import de.greensurvivors.eventhelper.modules.ghost.command.AGameSubCmd;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.command.CommandSender;
import org.bukkit.permissions.Permissible;
import org.bukkit.permissions.Permission;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class GameDuration extends AGameSubCmd {

    public GameDuration(@NotNull EventHelper plugin, @Nullable Permission permission) {
        super(plugin, permission);
    }

    @Override
    public @NotNull Set<@NotNull String> getAliases() {
        return Set.of("gameduration");
    }

    @Override
    public @NotNull LangPath getHelpTextPath(@NotNull Permissible permissible, @NotNull LinkedList<String> arguments) {
        return null;
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull GhostGame game, @NotNull LinkedList<@NotNull String> args) {
        if (!args.isEmpty()) {

            final @Nullable Duration newGameDuration = plugin.getMessageManager().parseDuration(args.getFirst());

            if (newGameDuration != null) {
                game.getConfig().setGameDuration(newGameDuration);

                plugin.getMessageManager().sendPrefixedLang(sender,
                    GhostLangPath.MESSAGE_PREFIX, GhostLangPath.COMMAND_SET_GAME_DURATION_SUCCESS,
                    Placeholder.component(SharedPlaceHolder.TIME.getKey(), MessageManager.formatTime(newGameDuration)));
            } else {
                plugin.getMessageManager().sendPrefixedLang(sender,
                    GhostLangPath.MESSAGE_PREFIX, SharedLangPath.ARG_NOT_A_TIME,
                    Placeholder.unparsed(SharedPlaceHolder.ARGUMENT.getKey(), args.getFirst()));

                plugin.getComponentLogger().debug("could not decode duration argument {} in command to set game duration!", args.getFirst());

                return false;
            }
        } else {
            plugin.getMessageManager().sendPrefixedLang(sender,
                GhostLangPath.MESSAGE_PREFIX, SharedLangPath.NOT_ENOUGH_ARGS);
            return false;
        }

        return true;
    }

    @Override // todo
    public @NotNull List<@NotNull String> tabComplete(@NotNull CommandSender sender, @NotNull GhostGame game, @NotNull LinkedList<@NotNull String> args) {
        return List.of();
    }
}
