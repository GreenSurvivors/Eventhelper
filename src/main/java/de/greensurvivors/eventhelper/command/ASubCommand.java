package de.greensurvivors.eventhelper.command;

import de.greensurvivors.eventhelper.EventHelper;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.permissions.Permissible;
import org.bukkit.permissions.Permission;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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

    public boolean hasPermission(final @NotNull Permissible permissible) {
        return permission == null || permissible.hasPermission(permission);
    }

    public abstract boolean execute(@NotNull CommandSender sender, @NotNull List<String> args);

    public abstract List<String> tabComplete(@NotNull CommandSender sender, @NotNull List<String> args);
}