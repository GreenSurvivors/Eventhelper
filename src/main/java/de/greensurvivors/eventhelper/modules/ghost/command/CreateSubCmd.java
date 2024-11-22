package de.greensurvivors.eventhelper.modules.ghost.command;

import de.greensurvivors.eventhelper.EventHelper;
import de.greensurvivors.eventhelper.command.ASubCommand;
import de.greensurvivors.eventhelper.messages.LangPath;
import de.greensurvivors.eventhelper.messages.SharedPlaceHolder;
import de.greensurvivors.eventhelper.modules.ghost.GhostGame;
import de.greensurvivors.eventhelper.modules.ghost.GhostLangPath;
import de.greensurvivors.eventhelper.modules.ghost.GhostModul;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.command.CommandSender;
import org.bukkit.permissions.Permission;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class CreateSubCmd extends ASubCommand {
    protected final @NotNull GhostModul ghostModul;

    public CreateSubCmd(@NotNull EventHelper plugin, final @NotNull GhostModul ghostModul, @Nullable Permission permission) {
        super(plugin, permission);

        this.ghostModul = ghostModul;
    }

    @Override
    public @NotNull Set<@NotNull String> getAliases() {
        return Set.of("create");
    }

    @Override
    public @NotNull LangPath getHelpTextPath(@NotNull List<String> arguments) {
        return null;
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull LinkedList<@NotNull String> args) {
        if (!args.isEmpty()) {
            if (GhostModul.isValidGhostGameName(args.getFirst())) {
                GhostGame newGame = ghostModul.createNewGame(args.getFirst());

                if (newGame != null) {
                    plugin.getMessageManager().sendPrefixedLang(sender,
                        GhostLangPath.MESSAGE_PREFIX, GhostLangPath.COMMAND_CREATE_SUCCESS,
                        Placeholder.parsed(SharedPlaceHolder.TEXT.getKey(), newGame.getNameID()));
                } else {
                    plugin.getMessageManager().sendPrefixedLang(sender,
                        GhostLangPath.MESSAGE_PREFIX, GhostLangPath.COMMAND_CREATE_ERROR_EXISTS,
                        Placeholder.unparsed(SharedPlaceHolder.ARGUMENT.getKey(), args.getFirst()));
                    return true;
                }
            } else {
                plugin.getMessageManager().sendPrefixedLang(sender,
                    GhostLangPath.MESSAGE_PREFIX, GhostLangPath.COMMAND_CREATE_ERROR_INVALID_NAME,
                    Placeholder.unparsed(SharedPlaceHolder.ARGUMENT.getKey(), args.getFirst()));
                return true;
            }
        }

        return false;
    }

    @Override
    public @NotNull List<@NotNull String> tabComplete(@NotNull CommandSender sender, @NotNull LinkedList<@NotNull String> args) {
        return List.of();
    }
}
