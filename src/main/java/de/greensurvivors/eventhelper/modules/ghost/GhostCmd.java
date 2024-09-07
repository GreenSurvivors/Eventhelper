package de.greensurvivors.eventhelper.modules.ghost;

import de.greensurvivors.eventhelper.EventHelper;
import de.greensurvivors.eventhelper.command.ASubCommand;
import org.bukkit.command.CommandSender;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Set;

public class GhostCmd extends ASubCommand {

    protected GhostCmd(final @NotNull EventHelper plugin, final @Nullable Permission parentpermission) {
        super(plugin, new Permission("eventhelper.command.ghost", "GhostCmd Command", PermissionDefault.OP));

        if (parentpermission != null) {
            super.permission.addParent(parentpermission, true);
        }
    }

    @Override
    public @NotNull Set<@NotNull String> getAliases() {
        return Set.of();
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull List<String> args) {
        return false;
    }

    @Override
    public List<String> tabComplete(@NotNull CommandSender sender, @NotNull List<String> args) {
        return List.of();
    }
}
