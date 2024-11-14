package de.greensurvivors.eventhelper.command;

import de.greensurvivors.eventhelper.EventHelper;
import de.greensurvivors.eventhelper.messages.LangPath;
import de.greensurvivors.eventhelper.messages.SharedLangPath;
import org.bukkit.command.CommandSender;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class ReloadCommand extends ASubCommand {
    private final static @NotNull Permission permission = new Permission("eventhelper.command.reload", PermissionDefault.OP);

    protected ReloadCommand(@NotNull EventHelper plugin, @NotNull Permission parentPermission) {
        super(plugin, permission);

        permission.addParent(parentPermission, true);
    }

    @Override
    public @NotNull Set<@NotNull String> getAliases() {
        return Set.of("reloadall");
    }

    @Override
    public @NotNull LangPath getHelpTextPath() {
        return SharedLangPath.RELOAD_HELP_TEXT;
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull LinkedList<@NotNull String> args) {
        plugin.reloadAll();
        return true;
    }

    @Override
    public @NotNull List<@NotNull String> tabComplete(@NotNull CommandSender sender, @NotNull LinkedList<@NotNull String> args) {
        return List.of();
    }
}
