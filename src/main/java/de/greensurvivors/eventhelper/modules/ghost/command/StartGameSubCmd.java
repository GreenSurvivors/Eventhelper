package de.greensurvivors.eventhelper.modules.ghost.command;

import de.greensurvivors.eventhelper.EventHelper;
import de.greensurvivors.eventhelper.messages.LangPath;
import de.greensurvivors.eventhelper.messages.SharedLangPath;
import de.greensurvivors.eventhelper.messages.SharedPlaceHolder;
import de.greensurvivors.eventhelper.modules.ghost.GhostGame;
import de.greensurvivors.eventhelper.modules.ghost.GhostLangPath;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.command.CommandSender;
import org.bukkit.permissions.Permissible;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class StartGameSubCmd extends AGameSubCmd {
    public StartGameSubCmd(@NotNull EventHelper plugin, @NotNull Permission parentPermission) {
        super(plugin, new Permission("eventhelper.ghost.command.startgame", PermissionDefault.OP));

        permission.addParent(parentPermission, true);
    }

    @Override
    public @NotNull Set<@NotNull String> getAliases() {
        return Set.of("startgame");
    }

    @Override
    public @NotNull LangPath getHelpTextPath(@NotNull Permissible permissible, @NotNull LinkedList<String> arguments) {
        return null;
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull GhostGame game, @NotNull LinkedList<@NotNull String> args) {
        if (hasPermission(sender)) {
            if (game.getGameState() == GhostGame.GameState.IDLE) {
                game.startStartingCountdown();

                plugin.getMessageManager().sendPrefixedLang(sender,
                    GhostLangPath.MESSAGE_PREFIX, GhostLangPath.COMMAND_START_SUCCESS,
                    Placeholder.unparsed(SharedPlaceHolder.TEXT.getKey(), game.getNameID()));
            } else {
                plugin.getMessageManager().sendPrefixedLang(sender,
                    GhostLangPath.MESSAGE_PREFIX,
                    GhostLangPath.COMMAND_START_ERROR_ALREADY_RUNNING, // todo display current state
                    Placeholder.unparsed(SharedPlaceHolder.TEXT.getKey(), game.getNameID()));
            }
        } else {
            plugin.getMessageManager().sendLang(sender, SharedLangPath.NO_PERMISSION);
        }

        return true;
    }

    @Override
    public @NotNull List<@NotNull String> tabComplete(@NotNull CommandSender sender, @NotNull GhostGame game, @NotNull LinkedList<@NotNull String> args) {
        return List.of();
    }
}
