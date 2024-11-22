package de.greensurvivors.eventhelper.modules.ghost.command;

import de.greensurvivors.eventhelper.EventHelper;
import de.greensurvivors.eventhelper.messages.LangPath;
import de.greensurvivors.eventhelper.messages.SharedLangPath;
import de.greensurvivors.eventhelper.messages.SharedPlaceHolder;
import de.greensurvivors.eventhelper.modules.ghost.GhostGame;
import de.greensurvivors.eventhelper.modules.ghost.GhostLangPath;
import de.greensurvivors.eventhelper.modules.ghost.command.set.*;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.command.CommandSender;
import org.bukkit.permissions.Permissible;
import org.bukkit.permissions.Permission;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class SetSubCmd extends AGameSubCmd {
    protected final @NotNull Map<String, AGameSubCmd> registeredCommandMap = new TreeMap<>(); // SequencedMap

    protected SetSubCmd(@NotNull EventHelper plugin, @NotNull Permission permission) {
        super(plugin, permission);

        registerSubCommand(new AmountOfGhosts(plugin, permission));
        registerSubCommand(new EndLocation(plugin, permission));
        registerSubCommand(new EndPlayerTime(plugin, permission));
        registerSubCommand(new FeedingAmount(plugin, permission));
        registerSubCommand(new FeedingDuration(plugin, permission));
        registerSubCommand(new FeedingRegion(plugin, permission));
        registerSubCommand(new GameDuration(plugin, permission));
        registerSubCommand(new GhostOffset(plugin, permission));
        registerSubCommand(new IsLateJoinAllowed(plugin, permission));
        registerSubCommand(new IsRejoinAllowed(plugin, permission));
        registerSubCommand(new LobbyLocation(plugin, permission));
        registerSubCommand(new SpectatorLocation(plugin, permission));
        registerSubCommand(new MaxAmountPlayers(plugin, permission));
        registerSubCommand(new MaxFeedingAmount(plugin, permission));
        registerSubCommand(new MinAmountPlayers(plugin, permission));
        registerSubCommand(new StartHealth(plugin, permission));
        registerSubCommand(new StartingFood(plugin, permission));
        registerSubCommand(new StartingSaturation(plugin, permission));
        registerSubCommand(new StartLocation(plugin, permission));
        registerSubCommand(new StartPlayerTime(plugin, permission));
        registerSubCommand(new PerishedTaskAmount(plugin, permission));
        registerSubCommand(new DurationTappedUntilDeath(plugin, permission));
        registerSubCommand(new PointGoal(plugin, permission));
        registerSubCommand(new AmountOfVexes(plugin, permission));
    }

    public void registerSubCommand(final @NotNull AGameSubCmd subCommand) {
        for (final @NotNull String alias : subCommand.getAliases()) {
            this.registeredCommandMap.put(alias, subCommand);
        }
    }

    /**
     * @return an ordered set of all aliases (including the main one!) this subcommand can get called by
     */
    @Override
    public @NotNull Set<@NotNull String> getAliases() {
        return Set.of("set");
    }

    /**
     * @return a help text, to be displayed by /eventhelper help
     */
    @Override
    public @NotNull LangPath getHelpTextPath(@NotNull Permissible permissible, @NotNull LinkedList<String> args) {
        if (!args.isEmpty()) {
            AGameSubCmd subCommand = registeredCommandMap.get(args.getFirst());

            if (subCommand != null && subCommand.hasPermission(permissible)) {
                args.removeFirst();

                return subCommand.getHelpTextPath(permissible, args);
            }
        }

        return null;
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull GhostGame game, @NotNull LinkedList<@NotNull String> args) {
        if (hasPermission(sender)) {
            if (!args.isEmpty()) {
                AGameSubCmd subCommand = registeredCommandMap.get(args.getFirst());

                if (subCommand != null) {
                    if (subCommand.hasPermission(sender)) {
                        args.removeFirst();

                        return subCommand.execute(sender, game, args);
                    } else {
                        plugin.getMessageManager().sendLang(sender, SharedLangPath.NO_PERMISSION);
                        return true;
                    }
                } else {
                    plugin.getMessageManager().sendLang(sender, SharedLangPath.ARG_NOT_A_SUBCMD,
                        Placeholder.unparsed(SharedPlaceHolder.ARGUMENT.getKey(), args.getFirst()));
                }
            } else {
                plugin.getMessageManager().sendPrefixedLang(sender,
                    GhostLangPath.MESSAGE_PREFIX, SharedLangPath.NOT_ENOUGH_ARGS);
                return false;
            }
        } else {
            plugin.getMessageManager().sendLang(sender, SharedLangPath.NO_PERMISSION);
        }
        return true;
    }

    @Override
    public @NotNull List<@NotNull String> tabComplete(@NotNull CommandSender sender, @NotNull GhostGame game, @NotNull LinkedList<@NotNull String> args) {
        if (args.isEmpty()) {
            List<String> result = new ArrayList<>(registeredCommandMap.size());

            for (AGameSubCmd subCommand : registeredCommandMap.values()) {
                if (subCommand.hasPermission(sender)) {
                    result.addAll(subCommand.getAliases());
                }
            }

            return result;
        } else if (args.size() == 1) {
            List<String> result = new ArrayList<>(registeredCommandMap.size());

            for (AGameSubCmd subCommand : registeredCommandMap.values()) {
                if (subCommand.hasPermission(sender)) {
                    for (String subAlias : subCommand.getAliases()) {
                        if (subAlias.startsWith(args.getFirst())) {
                            result.add(subAlias);
                        }
                    }
                }
            }

            return result;
        } else {
            AGameSubCmd subCommand = registeredCommandMap.get(args.getFirst());

            if (subCommand != null && subCommand.hasPermission(sender)) {
                LinkedList<String> shortenedArgs = new LinkedList<>(args);
                shortenedArgs.removeFirst();

                return subCommand.tabComplete(sender, game, shortenedArgs);
            }
        }

        return List.of();
    }
}
