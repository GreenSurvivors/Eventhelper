package de.greensurvivors.eventhelper.command;

import de.greensurvivors.eventhelper.EventHelper;
import de.greensurvivors.eventhelper.messages.LangPath;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.permissions.Permissible;
import org.bukkit.permissions.Permission;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public abstract class ASubCommand {
    protected final @NotNull EventHelper plugin;
    protected final @Nullable Permission permission;

    protected ASubCommand(final @NotNull EventHelper plugin, final @Nullable Permission permission) {
        this.plugin = plugin;
        this.permission = permission;

        if (permission != null) {
            Bukkit.getPluginManager().addPermission(permission);
        }
    }

    public abstract @NotNull Set<@NotNull String> getAliases(); // SequencedSet

    public abstract @NotNull LangPath getHelpTextPath();

    public boolean hasPermission(final @NotNull Permissible permissible) {
        return permission == null || permissible.hasPermission(permission);
    }

    public abstract boolean execute(@NotNull CommandSender sender, @NotNull LinkedList<@NotNull String> args);

    public abstract @NotNull List<@NotNull String> tabComplete(@NotNull CommandSender sender, @NotNull LinkedList<@NotNull String> args);
}