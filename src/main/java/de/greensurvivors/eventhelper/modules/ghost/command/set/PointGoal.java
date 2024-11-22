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

public class PointGoal extends AGameSubCmd {

    public PointGoal(@NotNull EventHelper plugin, @Nullable Permission permission) {
        super(plugin, permission);
    }

    @Override
    public @NotNull Set<@NotNull String> getAliases() {
        return Set.of("pointgoal", "pointstowin");
    }

    @Override
    public @NotNull LangPath getHelpTextPath(@NotNull Permissible permissible, @NotNull LinkedList<String> arguments) {
        return null;
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull GhostGame game, @NotNull LinkedList<@NotNull String> args) {
        if (hasPermission(sender)) {
            if (!args.isEmpty()) {
                try {
                    int pointGoal = Integer.parseUnsignedInt(args.getFirst(), 1, Integer.MAX_VALUE, 10);
                    game.getConfig().setPointGoal(pointGoal);

                    plugin.getMessageManager().sendPrefixedLang(sender, GhostLangPath.MESSAGE_PREFIX, GhostLangPath.COMMAND_SET_POINT_GOAL_SUCCESS,
                        Placeholder.unparsed(SharedPlaceHolder.NUMBER.getKey(), String.valueOf(pointGoal)));
                } catch (NumberFormatException ignored) {
                    plugin.getMessageManager().sendPrefixedLang(sender, GhostLangPath.MESSAGE_PREFIX, SharedLangPath.ARG_NOT_A_NUMBER,
                        Placeholder.unparsed(SharedPlaceHolder.TEXT.getKey(), args.getFirst()));
                    return false;
                }
            } else {
                plugin.getMessageManager().sendPrefixedLang(sender, GhostLangPath.MESSAGE_PREFIX, SharedLangPath.NOT_ENOUGH_ARGS);

                return false;
            }
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
