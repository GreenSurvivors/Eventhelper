package de.greensurvivors.eventhelper.modules.ghost.command;

import de.greensurvivors.eventhelper.EventHelper;
import de.greensurvivors.eventhelper.command.ASubCommand;
import de.greensurvivors.eventhelper.messages.LangPath;
import de.greensurvivors.eventhelper.messages.SharedLangPath;
import de.greensurvivors.eventhelper.messages.SharedPlaceHolder;
import de.greensurvivors.eventhelper.modules.ghost.GhostGame;
import de.greensurvivors.eventhelper.modules.ghost.GhostLangPath;
import de.greensurvivors.eventhelper.modules.ghost.GhostModul;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.command.CommandSender;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class ListSubCmd extends ASubCommand { // todo locations, players
    private final @NotNull GhostModul ghostModul;

    public ListSubCmd(@NotNull EventHelper plugin, final @NotNull GhostModul ghostModul, @NotNull Permission parentPermission) {
        super(plugin, new Permission("eventhelper.ghost.command.list", PermissionDefault.OP));
        permission.addParent(parentPermission, true);

        this.ghostModul = ghostModul;
    }

    @Override
    public @NotNull Set<@NotNull String> getAliases() {
        return Set.of();
    }

    @Override
    public @NotNull LangPath getHelpTextPath(@NotNull List<String> arguments) {
        return null;
    }

    private NamedTextColor getColorOfGame(final @NotNull GhostGame game) {
        if (game.getConfig().isEnabled()) {
            return switch (game.getGameState()) {
                case IDLE -> NamedTextColor.DARK_GREEN;
                case RUNNING -> NamedTextColor.YELLOW;
                case STARTING, COUNTDOWN, RESETTING, RELOADING_CONFIG -> NamedTextColor.GOLD;
            };
        } else {
            return NamedTextColor.DARK_GRAY;
        }
    }

    private @NotNull Component makeGameList() {
        final Set<String> gameNameIds = ghostModul.getGameNameIds();
        final List<Component> games = new ArrayList<>(gameNameIds.size());

        for (String gameId : gameNameIds) {
            final GhostGame game = ghostModul.getGameByName(gameId);

            games.add(Component.text(gameId, getColorOfGame(game)).
                clickEvent(ClickEvent.runCommand("eventhelper gg info " + gameId)));
        }

        return Component.join(JoinConfiguration.commas(true), games);
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull LinkedList<@NotNull String> args) {
        if (hasPermission(sender)) {
            plugin.getMessageManager().sendPrefixedLang(sender, GhostLangPath.MESSAGE_PREFIX, GhostLangPath.COMMAND_LIST_GAMES,
                Placeholder.component(SharedPlaceHolder.TEXT.getKey(), makeGameList()));
        } else {
            plugin.getMessageManager().sendPrefixedLang(sender, GhostLangPath.MESSAGE_PREFIX, SharedLangPath.NO_PERMISSION);
        }

        return true;
    }

    @Override
    public @NotNull List<@NotNull String> tabComplete(@NotNull CommandSender sender, @NotNull LinkedList<@NotNull String> args) {
        return List.of();
    }
}
