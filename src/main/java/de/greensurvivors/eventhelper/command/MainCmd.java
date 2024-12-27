package de.greensurvivors.eventhelper.command;

import de.greensurvivors.eventhelper.EventHelper;
import de.greensurvivors.eventhelper.messages.SharedLangPath;
import de.greensurvivors.eventhelper.messages.SharedPlaceHolder;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
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
    protected final @NotNull Map<String, ASubCommand> registeredCommandMap = new LinkedHashMap<>(); // SequencedMap

    public MainCmd(@NotNull EventHelper plugin) {
        super(label, description, "use /evh help", List.of("evhelper", "evh"));

        this.plugin = plugin;
        plugin.getServer().getCommandMap().register(MainCmd.label, plugin.getName().toLowerCase(Locale.ENGLISH), this);

        //registerSubCommand(new HelpSubCommand(plugin, permission, this)); // todo
        registerSubCommand(new ReloadCommand(plugin, permission));
    }

    public static @NotNull Permission getParentPermission() {
        return permission;
    }

    public void registerSubCommand(final @NotNull ASubCommand subCommand) {
        for (final @NotNull String alias : subCommand.getAliases()) {
            this.registeredCommandMap.put(alias, subCommand);
        }
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String commandLabel, @NotNull String[] args) {
        if (args.length > 0) {
            ASubCommand subCommand = registeredCommandMap.get(args[0]);

            if (subCommand != null) {
                if (subCommand.hasPermission(sender)) {
                    LinkedList<String> shortenedArgs = new LinkedList<>(Arrays.asList(args));
                    shortenedArgs.removeFirst();

                    return subCommand.execute(sender, shortenedArgs);
                } else {
                    plugin.getMessageManager().sendLang(sender, SharedLangPath.NO_PERMISSION);
                    return true;
                }
            } else {
                plugin.getMessageManager().sendLang(sender, SharedLangPath.ARG_NOT_A_SUBCMD,
                    Placeholder.unparsed(SharedPlaceHolder.ARGUMENT.getKey(), args[0]));
            }
        }

        return false;
    }

    @Override
    public @NotNull List<String> tabComplete(@NotNull CommandSender sender, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 0) {
            List<String> result = new ArrayList<>(registeredCommandMap.size());

            for (ASubCommand subCommand : registeredCommandMap.values()) {
                if (subCommand.hasPermission(sender)) {
                    result.addAll(subCommand.getAliases());
                }
            }

            return result;
        } else if (args.length == 1) {
            List<String> result = new ArrayList<>(registeredCommandMap.size());

            for (ASubCommand subCommand : registeredCommandMap.values()) {
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
                LinkedList<String> shortenedArgs = new LinkedList<>(Arrays.asList(args));
                shortenedArgs.removeFirst();

                return subCommand.tabComplete(sender, shortenedArgs);
            }
        }

        return List.of();
    }
}
