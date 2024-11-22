package de.greensurvivors.eventhelper.modules.ghost.command;

import de.greensurvivors.eventhelper.EventHelper;
import de.greensurvivors.eventhelper.messages.LangPath;
import de.greensurvivors.eventhelper.messages.MessageManager;
import de.greensurvivors.eventhelper.messages.SharedLangPath;
import de.greensurvivors.eventhelper.messages.SharedPlaceHolder;
import de.greensurvivors.eventhelper.modules.ghost.GhostGame;
import de.greensurvivors.eventhelper.modules.ghost.GhostLangPath;
import de.greensurvivors.eventhelper.modules.ghost.GhostPlaceHolder;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.command.CommandSender;
import org.bukkit.permissions.Permissible;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class InfoSubCmd extends AGameSubCmd {

    public InfoSubCmd(@NotNull EventHelper plugin, @NotNull Permission parentPermission) {
        super(plugin, new Permission("eventhelper.ghost.command.info", PermissionDefault.OP));
        permission.addParent(parentPermission, true);
    }

    @Override
    public @NotNull Set<@NotNull String> getAliases() {
        return Set.of("info");
    }

    @Override
    public @NotNull LangPath getHelpTextPath(@NotNull Permissible permissible, @NotNull LinkedList<String> arguments) {
        return null;
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull GhostGame game, @NotNull LinkedList<@NotNull String> args) {
        if (hasPermission(sender)) {
            final @Nullable Duration remainingDuration = game.getRemainingDuration();

            plugin.getMessageManager().sendLang(sender, GhostLangPath.COMMAND_INFO_SUCCESS,
                Placeholder.unparsed(GhostPlaceHolder.GAME_STATUS.getKey(), game.getGameState().name()),
                Placeholder.component(SharedPlaceHolder.TIME.getKey(), remainingDuration == null ? Component.text("-") : MessageManager.formatTime(remainingDuration)),
                Placeholder.unparsed(GhostPlaceHolder.POINTS_CURRENT.getKey(), String.format("%.2f", game.getGainedPointAmount())),
                Placeholder.unparsed(GhostPlaceHolder.POINTS_MAX.getKey(), String.format("%d", game.getConfig().getPointGoal())),
                Placeholder.unparsed(GhostPlaceHolder.AMOUNT_PLAYERS_ALIVE.getKey(), String.format("%d", game.getAliveFreePlayerAmount())),
                Placeholder.unparsed(GhostPlaceHolder.AMOUNT_PLAYERS_TRAPPED.getKey(), String.format("%d", game.getTrappedPlayerAmount())),
                Placeholder.unparsed(GhostPlaceHolder.AMOUNT_PLAYERS_DEAD.getKey(), String.format("%d", game.getPerishedPlayersAmount())));
        } else {
            plugin.getMessageManager().sendPrefixedLang(sender, GhostLangPath.MESSAGE_PREFIX, SharedLangPath.NO_PERMISSION);
        }

        return true;
    }

    @Override
    public @NotNull List<@NotNull String> tabComplete(@NotNull CommandSender sender, @NotNull GhostGame game, @NotNull LinkedList<@NotNull String> args) {
        return List.of();
    }
}
