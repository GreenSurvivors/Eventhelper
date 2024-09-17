package de.greensurvivors.eventhelper.command;

import de.greensurvivors.eventhelper.EventHelper;
import de.greensurvivors.eventhelper.messages.StandardLangPath;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class MainCmd extends Command {
    private final static @NotNull String description = "eventhelper MainCmd command branches out to subcommands";
    private final static @NotNull Permission permission = new Permission("eventhelper.command.*", description, PermissionDefault.OP);
    private final static @NotNull String label = "eventhelper";
    private final @NotNull EventHelper plugin;
    private final @NotNull Set<ASubCommand> registeredCommands = new LinkedHashSet<>(); // SequenzedSet
    private final @NotNull Map<String, ASubCommand> registeredCommandMap = new LinkedHashMap<>(); // SequencedMap

    public MainCmd(@NotNull EventHelper plugin) {
        super(label, description, "use /evh help", List.of("evhelper", "evh"));

        this.plugin = plugin;
        plugin.getServer().getCommandMap().register(plugin.getName().toLowerCase(Locale.ENGLISH), MainCmd.label, this);
    }

    public static @NotNull Permission getParentPermission() {
        return permission;
    }

    public void registerSubCommand(final @NotNull ASubCommand subCommand) {
        if (registeredCommands.add(subCommand)) {

            for (final @NotNull String alias : subCommand.getAliases()) {
                this.registeredCommandMap.put(alias, subCommand);
            }
        }
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String commandLabel, @NotNull String[] args) {
        if (args.length > 0) {

            ASubCommand subCommand = registeredCommandMap.get(args[0]);

            if (subCommand != null) {
                if (subCommand.hasPermission(sender)) {
                    List<String> shortenedArgs = new ArrayList<>(Arrays.asList(args));
                    shortenedArgs.remove(0);

                    return subCommand.execute(sender, shortenedArgs);
                } else {
                    plugin.getMessageManager().sendLang(sender, StandardLangPath.NO_PERMISSION);
                    return true;
                }
            }
        }

        return false;
    }

    @Override
    public @NotNull List<String> tabComplete(@NotNull CommandSender sender, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 0) {
            List<String> result = new ArrayList<>(registeredCommandMap.size());

            for (ASubCommand subCommand : registeredCommands) {
                if (subCommand.hasPermission(sender)) {
                    result.addAll(subCommand.getAliases());
                }
            }

            return result;
        } else if (args.length == 1) {
            List<String> result = new ArrayList<>(registeredCommandMap.size());

            for (ASubCommand subCommand : registeredCommands) {
                if (subCommand.hasPermission(sender)) {
                    for (String subAlias : subCommand.getAliases()) {
                        if (subAlias.startsWith(args[0])) {
                            result.add(subAlias);
                        }
                    }
                }
            }

            return result;
        } else {
            ASubCommand subCommand = registeredCommandMap.get(args[0]);

            if (subCommand != null && subCommand.hasPermission(sender)) {
                List<String> shortenedArgs = new ArrayList<>(Arrays.asList(args));
                shortenedArgs.remove(0);

                return subCommand.tabComplete(sender, shortenedArgs);
            }
        }

        return List.of();
    }
}
