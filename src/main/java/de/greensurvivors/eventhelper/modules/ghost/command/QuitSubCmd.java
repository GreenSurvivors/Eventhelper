package de.greensurvivors.eventhelper.modules.ghost.command;

import de.greensurvivors.eventhelper.EventHelper;
import de.greensurvivors.eventhelper.command.ASubCommand;
import de.greensurvivors.eventhelper.messages.LangPath;
import de.greensurvivors.eventhelper.messages.SharedLangPath;
import de.greensurvivors.eventhelper.messages.SharedPlaceHolder;
import de.greensurvivors.eventhelper.modules.ghost.GhostGame;
import de.greensurvivors.eventhelper.modules.ghost.GhostLangPath;
import de.greensurvivors.eventhelper.modules.ghost.GhostModul;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class QuitSubCmd extends ASubCommand {
    private final @NotNull Permission quitOthersPerm = new Permission("eventhelper.ghost.command.quit_others", "", PermissionDefault.OP);
    protected final @NotNull GhostModul ghostModul;

    protected QuitSubCmd(@NotNull EventHelper plugin, final @NotNull GhostModul ghostModul, @Nullable Permission parent) {
        super(plugin, null);

        quitOthersPerm.addParent(parent, true);
        this.ghostModul = ghostModul;
    }

    @Override
    public @NotNull Set<@NotNull String> getAliases() {
        return Set.of("quit");
    }

    @Override
    public @NotNull LangPath getHelpTextPath(@NotNull List<String> arguments) {
        return null;
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull LinkedList<@NotNull String> args) {
        if (!args.isEmpty() && sender.hasPermission(quitOthersPerm)) {
            final @Nullable Player playerToQuit = plugin.getServer().getPlayer(args.getFirst());

            if (playerToQuit != null) {
                GhostGame game = ghostModul.getGameParticipatingIn(playerToQuit);

                if (game != null) {
                    game.playerQuit(playerToQuit, true);
                    plugin.getMessageManager().sendPrefixedLang(sender, GhostLangPath.MESSAGE_PREFIX, GhostLangPath.QUIT_OTHER_GAME,
                        Placeholder.component(SharedPlaceHolder.PLAYER.getKey(), playerToQuit.displayName()));
                } else {
                    plugin.getMessageManager().sendLang(sender, GhostLangPath.ERROR_NOT_PLAYING_OTHER,
                        Placeholder.component(SharedPlaceHolder.PLAYER.getKey(), playerToQuit.displayName()));
                }
            } else {
                plugin.getMessageManager().sendPrefixedLang(sender, GhostLangPath.MESSAGE_PREFIX, SharedLangPath.ARG_NOT_A_PLAYER,
                    Placeholder.unparsed(SharedPlaceHolder.TEXT.getKey(), args.getFirst()));
            }
        } else {
            if (sender instanceof Player player) {
                GhostGame game = ghostModul.getGameParticipatingIn(player);

                if (game != null) {
                    game.playerQuit(player, true);
                } else {
                    plugin.getMessageManager().sendLang(player, GhostLangPath.ERROR_NOT_PLAYING_SELF);
                }
            } else {
                plugin.getMessageManager().sendLang(sender, SharedLangPath.CMD_ERROR_SENDER_NOT_A_PLAYER);
            }
        }

        return true;
    }

    @Override
    public @NotNull List<@NotNull String> tabComplete(@NotNull CommandSender sender, @NotNull LinkedList<@NotNull String> args) {
        if (args.isEmpty() && sender.hasPermission(quitOthersPerm)) {
            return plugin.getServer().getOnlinePlayers().stream().map(Player::getName).toList();
        } else if (args.size() == 1 && sender.hasPermission(quitOthersPerm)) {
            return plugin.getServer().getOnlinePlayers().stream().map(Player::getName).filter(name -> StringUtils.startsWithIgnoreCase(name, args.getFirst())).toList();
        }

        return List.of();
    }
}
