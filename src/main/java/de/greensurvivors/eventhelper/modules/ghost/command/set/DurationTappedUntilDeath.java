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

public class DurationTappedUntilDeath extends AGameSubCmd {

    public DurationTappedUntilDeath(@NotNull EventHelper plugin, @Nullable Permission permission) {
        super(plugin, permission);
    }

    @Override
    public @NotNull Set<@NotNull String> getAliases() {
        return Set.of("durationtrappeduntildeath", "trappedtime");
    }

    @Override
    public @NotNull LangPath getHelpTextPath(@NotNull Permissible permissible, @NotNull LinkedList<String> arguments) {
        return null;
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull GhostGame game, @NotNull LinkedList<@NotNull String> args) {
        if (hasPermission(sender)) {
            if (!args.isEmpty()) {
                @Nullable Duration duration = plugin.getMessageManager().parseDuration(args.getFirst());

                if (duration != null) {
                    game.getConfig().setDurationTrappedUntilDeath(duration);

                    plugin.getMessageManager().sendPrefixedLang(sender, GhostLangPath.MESSAGE_PREFIX, GhostLangPath.COMMAND_SET_DURATION_TAPPED_UNTIL_DEATH_SUCCESS,
                        Placeholder.component(SharedPlaceHolder.TIME.getKey(), MessageManager.formatTime(duration)));
                } else {
                    plugin.getMessageManager().sendPrefixedLang(sender, GhostLangPath.MESSAGE_PREFIX, SharedLangPath.ARG_NOT_A_TIME,
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
