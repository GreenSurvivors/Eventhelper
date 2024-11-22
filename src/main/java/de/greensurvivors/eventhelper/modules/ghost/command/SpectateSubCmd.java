package de.greensurvivors.eventhelper.modules.ghost.command;

import de.greensurvivors.eventhelper.EventHelper;
import de.greensurvivors.eventhelper.messages.LangPath;
import de.greensurvivors.eventhelper.messages.SharedLangPath;
import de.greensurvivors.eventhelper.modules.ghost.GhostGame;
import de.greensurvivors.eventhelper.modules.ghost.GhostModul;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permissible;
import org.bukkit.permissions.Permission;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class SpectateSubCmd extends AGameSubCmd {
    protected final @NotNull GhostModul ghostModul;

    public SpectateSubCmd(@NotNull EventHelper plugin, final @NotNull GhostModul ghostModul, @Nullable Permission permission) {
        super(plugin, permission);

        this.ghostModul = ghostModul;
    }

    @Override
    public @NotNull Set<@NotNull String> getAliases() {
        return Set.of("spectate");
    }

    @Override
    public @NotNull LangPath getHelpTextPath(@NotNull Permissible permissible, @NotNull LinkedList<String> arguments) {
        return null;
    }

    @Override
    public boolean hasPermission(final @NotNull Permissible permissible) {
        return permissible.hasPermission(permission) || permissible.hasPermission(GhostModul.getSpectatePermission());
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull GhostGame game, @NotNull LinkedList<@NotNull String> args) {
        if (hasPermission(sender)) {
            if (sender instanceof Player player) {
                game.playerSpectate(player);
            } else {
                plugin.getMessageManager().sendLang(sender, SharedLangPath.CMD_ERROR_SENDER_NOT_A_PLAYER);
            }
        }

        return true;
    }

    @Override
    public @NotNull List<@NotNull String> tabComplete(@NotNull CommandSender sender, @NotNull GhostGame game, @NotNull LinkedList<@NotNull String> args) {
        return List.of();
    }
}
