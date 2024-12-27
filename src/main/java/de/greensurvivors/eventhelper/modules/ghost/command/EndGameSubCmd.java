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
import org.jetbrains.annotations.Nullable;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class EndGameSubCmd extends AGameSubCmd {

    public EndGameSubCmd(@NotNull EventHelper plugin, @Nullable Permission parentPermission) {
        super(plugin, new Permission("eventhelper.ghost.command.endgame", PermissionDefault.OP));

        permission.addParent(parentPermission, true);
    }

    @Override
    public @NotNull Set<@NotNull String> getAliases() {
        return Set.of("endgame");
    }

    @Override
    public @NotNull LangPath getHelpTextPath(@NotNull Permissible permissible, @NotNull LinkedList<String> arguments) {
        return null;
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull GhostGame game, @NotNull LinkedList<@NotNull String> args) {
        if (hasPermission(sender)) {
            game.endGame(GhostGame.EndReason.EXTERN);

            plugin.getMessageManager().sendPrefixedLang(sender,
                GhostLangPath.MESSAGE_PREFIX, GhostLangPath.COMMAND_END_SUCCESS,
                Placeholder.unparsed(SharedPlaceHolder.TEXT.getKey(), game.getNameID()));
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
