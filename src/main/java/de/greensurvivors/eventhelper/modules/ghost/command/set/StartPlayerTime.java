package de.greensurvivors.eventhelper.modules.ghost.command.set;

import de.greensurvivors.eventhelper.EventHelper;
import de.greensurvivors.eventhelper.messages.LangPath;
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

import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class StartPlayerTime extends AGameSubCmd {

    public StartPlayerTime(@NotNull EventHelper plugin, @Nullable Permission permission) {
        super(plugin, permission);
    }

    @Override
    public @NotNull Set<@NotNull String> getAliases() {
        return Set.of("startplayertime");
    }

    @Override
    public @NotNull LangPath getHelpTextPath(@NotNull Permissible permissible, @NotNull LinkedList<String> arguments) {
        return null;
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull GhostGame game, @NotNull LinkedList<@NotNull String> args) {
        if (!args.isEmpty()) {
            try {
                int newStartPlayerTime = Integer.decode(args.getFirst());

                if (0 <= newStartPlayerTime && newStartPlayerTime <= 24000) {
                    if (newStartPlayerTime <= game.getConfig().getEndPlayerTime()) {
                        game.getConfig().setStartPlayerTime(newStartPlayerTime);

                        plugin.getMessageManager().sendPrefixedLang(sender,
                            GhostLangPath.MESSAGE_PREFIX, GhostLangPath.COMMAND_SET_START_PLAYERTIME_SUCCESS,
                            Placeholder.unparsed(SharedPlaceHolder.NUMBER.getKey(), String.valueOf(newStartPlayerTime)));
                    } else {
                        plugin.getMessageManager().sendPrefixedLang(sender,
                            GhostLangPath.MESSAGE_PREFIX, GhostLangPath.COMMAND_SET_START_PLAYERTIME_ERROR_LARGER_THEN_END,
                            Placeholder.unparsed(SharedPlaceHolder.NUMBER.getKey(), String.valueOf(newStartPlayerTime)),
                            Placeholder.unparsed(SharedPlaceHolder.MAX.getKey(), String.valueOf(game.getConfig().getEndPlayerTime())));
                    }
                } else {
                    plugin.getMessageManager().sendPrefixedLang(sender,
                        GhostLangPath.MESSAGE_PREFIX, SharedLangPath.ARG_NUMBER_OUT_OF_BOUNDS,
                        Placeholder.unparsed(SharedPlaceHolder.NUMBER.getKey(), String.valueOf(newStartPlayerTime)),
                        Placeholder.unparsed(SharedPlaceHolder.MIN.getKey(), "0"),
                        Placeholder.unparsed(SharedPlaceHolder.MAX.getKey(), "24000"));
                }
            } catch (NumberFormatException e) {
                plugin.getMessageManager().sendPrefixedLang(sender,
                    GhostLangPath.MESSAGE_PREFIX, SharedLangPath.ARG_NOT_A_NUMBER,
                    Placeholder.unparsed(SharedPlaceHolder.ARGUMENT.getKey(), args.getFirst()));

                plugin.getComponentLogger().debug("could not decode Integer argument {} in command to set start player-time!", args.getFirst(), e);

                return false;
            }
        } else {
            plugin.getMessageManager().sendPrefixedLang(sender,
                GhostLangPath.MESSAGE_PREFIX, SharedLangPath.NOT_ENOUGH_ARGS);
            return false;
        }

        return true;
    }

    @Override
    public @NotNull List<@NotNull String> tabComplete(@NotNull CommandSender sender, @NotNull GhostGame game, @NotNull LinkedList<@NotNull String> args) {
        return List.of();
    }
}
