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

    /**
     * @return an ordered set of all aliases (including the main one!) this subcommand can get called by
     */
    public abstract @NotNull Set<@NotNull String> getAliases(); // todo SequencedSet

    /**
     * @return a help text, to be displayed by /eventhelper help
     */
    public abstract @NotNull LangPath getHelpTextPath(@NotNull List<String> arguments);

    /**
     * checks if the Permissible is allowed to run this command
     */
    public boolean hasPermission(final @NotNull Permissible permissible) {
        return permission == null || permissible.hasPermission(permission);
    }

    /**
     * Executes the subcommand, returning its success
     *
     * @param args the list of arguments, not including the subcommand or anything before
     * @return true if the command was successful, otherwise false
     */
    public abstract boolean execute(@NotNull CommandSender sender, @NotNull LinkedList<@NotNull String> args);

    /**
     * Executed on tab completion for this subcommand, returning a list of
     * options the player can tab through.
     *
     * @param sender Source object which is executing this command
     * @param args   the list of arguments, not including the subcommand or anything before
     * @return a list of tab-completions for the specified arguments. This
     * will never be null. List may be immutable.
     */
    public abstract @NotNull List<@NotNull String> tabComplete(@NotNull CommandSender sender, @NotNull LinkedList<@NotNull String> args);
}