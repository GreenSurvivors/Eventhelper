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
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

// todo rework this once Bridgadier is a thing
public class GhostCmd extends ASubCommand { // todo check sub permissions!
    private final @NotNull Permission EDIT_PERM = new Permission("eventhelper.ghost.command.edit", PermissionDefault.OP);
    // args pos2
    private static final @NotNull String
        REMOVE = "remove", REMOVE_NEAR = "removenear", REMOVE_ALL = "removeall";
    // args
    protected static final @NotNull String
        GHOST_SPAWN_LOCATION = "ghostspawnlocation",
        GHOST_IDLE_POSTION = "ghostidleposition",
        VEX_SPAWN_LOCATION = "vexspawnlocation";
    private final @NotNull GhostModul ghostModul;

    private final QuitSubCmd quitSubCmd; // quit is special as it the shortest cmd!
    private final @NotNull Map<String, ASubCommand> subCommands = new TreeMap<>();
    private final @NotNull Map<String, AGameSubCmd> gameSubcommands = new TreeMap<>();

    public GhostCmd(final @NotNull EventHelper plugin, final @Nullable Iterable<@NotNull Permission> parentPermissions, @NotNull GhostModul ghostModul) {
        super(plugin, null); // no permission needed, since quit sub cmd does not require a permission itself
        this.ghostModul = ghostModul;

        plugin.getServer().getCommandMap().register(plugin.getName(), new TopLevelGhostCmd(getAliases().iterator().next(), getAliases(), null));
        plugin.getServer().getPluginManager().addPermission(EDIT_PERM);

        final @NotNull Permission adminPerm = new Permission("eventhelper.ghost.command.*", PermissionDefault.OP, Map.of(
            EDIT_PERM.getName(), Boolean.TRUE
        )); // other children get added by the sub commands
        if (parentPermissions != null) {
            for (Permission parentPermission : parentPermissions) {
                adminPerm.addParent(parentPermission, true);
            }
        }
        plugin.getServer().getPluginManager().addPermission(adminPerm);

        quitSubCmd = new QuitSubCmd(plugin, ghostModul, adminPerm);
        registerSubCmd(quitSubCmd);
        registerSubCmd(new CreateSubCmd(plugin, ghostModul, EDIT_PERM));
        registerSubCmd(new ListSubCmd(plugin, ghostModul, adminPerm));
        registerGameSubCmd(new JoinSubCmd(plugin, ghostModul, adminPerm));
        registerGameSubCmd(new SpectateSubCmd(plugin, ghostModul, adminPerm));
        registerGameSubCmd(new StartGameSubCmd(plugin, adminPerm));
        registerGameSubCmd(new EndGameSubCmd(plugin, adminPerm));
        registerGameSubCmd(new SetSubCmd(plugin, EDIT_PERM));
        registerGameSubCmd(new AddSubCmd(plugin, EDIT_PERM));
        registerGameSubCmd(new InfoSubCmd(plugin, adminPerm));
    }

    protected void registerSubCmd(final @NotNull ASubCommand subCommand) {
        for (@NotNull String alias : subCommand.getAliases()) {
            subCommands.put(alias, subCommand);
        }
    }

    protected void registerGameSubCmd(final @NotNull AGameSubCmd gameSubCmd) {
        for (@NotNull String alias : gameSubCmd.getAliases()) {
            gameSubcommands.put(alias, gameSubCmd);
        }
    }

    @Override
    public @NotNull Set<@NotNull String> getAliases() {
        return Set.of("ghostgame", "ghost", "gg");
    }

    @Override
    public @NotNull LangPath getHelpTextPath(@NotNull List<String> arguments) {
        return null;
    }

    @Override
    public boolean execute(final @NotNull CommandSender sender, final @NotNull LinkedList<@NotNull String> args) {
        if (ghostModul.getConfig().isEnabled()) {
            if (args.size() >= 2) {
                for (Map.Entry<String, ASubCommand> entry : subCommands.entrySet()) {
                    if (StringUtils.equalsIgnoreCase(entry.getKey(), args.getFirst())) {
                        args.removeFirst();
                        return entry.getValue().execute(sender, args);
                    }
                }

                final @Nullable GhostGame game = ghostModul.getGameByName(args.getFirst());

                if (game != null) {
                    switch (args.get(1).toLowerCase(Locale.ENGLISH)) {
                        case REMOVE -> {
                            if (sender.hasPermission(EDIT_PERM)) {
                                if (args.size() >= 3) {
                                    // todo

                                } else {
                                    plugin.getMessageManager().sendPrefixedLang(sender,
                                        GhostLangPath.MESSAGE_PREFIX, SharedLangPath.NOT_ENOUGH_ARGS);
                                    return false;
                                }
                            } else {
                                plugin.getMessageManager().sendLang(sender, SharedLangPath.NO_PERMISSION);
                            }
                        }
                        case REMOVE_NEAR -> {
                            if (sender.hasPermission(EDIT_PERM)) {
                                if (args.size() >= 3) {
                                    // todo

                                } else {
                                    plugin.getMessageManager().sendPrefixedLang(sender,
                                        GhostLangPath.MESSAGE_PREFIX, SharedLangPath.NOT_ENOUGH_ARGS);
                                    return false;
                                }
                            } else {
                                plugin.getMessageManager().sendLang(sender, SharedLangPath.NO_PERMISSION);
                            }
                        }
                        case REMOVE_ALL -> {
                            if (sender.hasPermission(EDIT_PERM)) {
                                if (args.size() >= 3) {
                                    switch (args.get(2).toLowerCase(Locale.ENGLISH)) {
                                        case GHOST_SPAWN_LOCATION -> {
                                            game.getConfig().removeAllGhostSpawnLocations();

                                            plugin.getMessageManager().sendPrefixedLang(sender,
                                                GhostLangPath.MESSAGE_PREFIX, GhostLangPath.COMMAND_REMOVEALL_GHOST_SPAWNPOINT_SUCCESS);
                                        }
                                        case GHOST_IDLE_POSTION -> {
                                            game.getConfig().removeAllGhostIdlePositions();

                                            plugin.getMessageManager().sendPrefixedLang(sender,
                                                GhostLangPath.MESSAGE_PREFIX, GhostLangPath.COMMAND_REMOVEALL_GHOST_IDLEPOS_SUCCESS);
                                        }
                                        case VEX_SPAWN_LOCATION -> {
                                            game.getConfig().removeAllVexSpawnLocations();

                                            plugin.getMessageManager().sendPrefixedLang(sender,
                                                GhostLangPath.MESSAGE_PREFIX, GhostLangPath.COMMAND_REMOVEALL_VEX_SPAWNPOINT_SUCCESS);
                                        }
                                    }
                                } else {
                                    plugin.getMessageManager().sendPrefixedLang(sender,
                                        GhostLangPath.MESSAGE_PREFIX, SharedLangPath.NOT_ENOUGH_ARGS);
                                    return false;
                                }
                            } else {
                                plugin.getMessageManager().sendLang(sender, SharedLangPath.NO_PERMISSION);
                            }
                        }
                        default -> {
                            for (Map.Entry<String, AGameSubCmd> entry : gameSubcommands.entrySet()) {
                                if (StringUtils.equalsIgnoreCase(entry.getKey(), args.getFirst())) {
                                    args.removeFirst();
                                    args.removeFirst();
                                    return entry.getValue().execute(sender, game, args);
                                }
                            }
                            plugin.getMessageManager().sendPrefixedLang(sender, GhostLangPath.MESSAGE_PREFIX, SharedLangPath.UNKNOWN_ARG,
                                Placeholder.unparsed(SharedPlaceHolder.TEXT.getKey(), args.get(1)));

                            return false;
                        }
                    }
                } else {
                    plugin.getMessageManager().sendPrefixedLang(sender,
                        GhostLangPath.MESSAGE_PREFIX, GhostLangPath.ARG_NOT_A_GAME,
                        Placeholder.unparsed(SharedPlaceHolder.ARGUMENT.getKey(), args.getFirst()));
                }
            } else if (!args.isEmpty()) {
                for (String quitAlias : quitSubCmd.getAliases()) {
                    if (StringUtils.equalsIgnoreCase(quitAlias, args.getFirst())) {
                        args.removeFirst();
                        return quitSubCmd.execute(sender, args);
                    }
                }
                plugin.getMessageManager().sendPrefixedLang(sender,
                    GhostLangPath.MESSAGE_PREFIX, SharedLangPath.UNKNOWN_ARG,
                    Placeholder.unparsed(SharedPlaceHolder.TEXT.getKey(), args.getFirst()));
                return false;
            } else {
                plugin.getMessageManager().sendPrefixedLang(sender,
                    GhostLangPath.MESSAGE_PREFIX, SharedLangPath.NOT_ENOUGH_ARGS);
                return false;
            }
        } else {
            plugin.getMessageManager().sendPrefixedLang(sender,
                GhostLangPath.MESSAGE_PREFIX, GhostLangPath.ERROR_MODUL_NOT_ENABLED);
        }

        return true;
    }

    @Override
    public @NotNull List<String> tabComplete(@NotNull CommandSender sender, @NotNull LinkedList<@NotNull String> args) {
        switch (args.size()) {
            case 0 -> {
                List<String> result = new ArrayList<>(subCommands.size());

                for (Map.Entry<String, ASubCommand> entry : subCommands.entrySet()) {
                    if (entry.getValue().hasPermission(sender)) {
                        result.add(entry.getKey());
                    }
                }

                if (sender.hasPermission(EDIT_PERM)) {
                    Set<String> gameIds = ghostModul.getGameNameIds();
                    result.addAll(gameIds);
                }

                return result;
            }
            case 1 -> {
                final String arg_0 = args.getFirst();
                Set<String> gameIds = ghostModul.getGameNameIds();
                List<String> result = new ArrayList<>(gameIds.size());

                for (Map.Entry<String, ASubCommand> entry : subCommands.entrySet()) {
                    if (entry.getValue().hasPermission(sender) && StringUtils.startsWithIgnoreCase(entry.getKey(), arg_0)) {
                        result.add(entry.getKey());
                    }
                }

                if (sender.hasPermission(EDIT_PERM)) {
                    for (String gameNameId : gameIds) {
                        if (StringUtils.startsWithIgnoreCase(gameNameId, arg_0)) {
                            result.add(gameNameId);
                        }
                    }
                }

                return result;
            }
            case 2 -> {
                if (sender.hasPermission(EDIT_PERM)) {
                    final @Nullable GhostGame game = ghostModul.getGameByName(args.getFirst());

                    if (game != null) {
                        List<String> result = new ArrayList<>();

                        final String arg_1 = args.get(1);

                        for (String alias : gameSubcommands.keySet()) {
                            if (StringUtils.startsWithIgnoreCase(alias, arg_1)) {
                                result.add(alias);
                            }
                        }

                        if (StringUtils.startsWithIgnoreCase(REMOVE, arg_1)) {
                            result.add(REMOVE);
                        }

                        if (StringUtils.startsWithIgnoreCase(REMOVE_NEAR, arg_1)) {
                            result.add(REMOVE_NEAR);
                        }

                        if (StringUtils.startsWithIgnoreCase(REMOVE_ALL, arg_1)) {
                            result.add(REMOVE_ALL);
                        }

                        return result;
                    }
                }
            }
            case 3 -> {
                if (sender.hasPermission(EDIT_PERM)) {
                    final @Nullable GhostGame game = ghostModul.getGameByName(args.getFirst());

                    if (game != null) {
                        switch (args.get(1).toLowerCase(Locale.ENGLISH)) {
                            case REMOVE -> {
                                List<String> result = new ArrayList<>();
                                String arg_2 = args.get(2);

                                return result;
                            }
                            case REMOVE_NEAR -> {
                                List<String> result = new ArrayList<>();
                                String arg_2 = args.get(2);

                                return result;
                            }
                            case REMOVE_ALL -> {
                                List<String> result = new ArrayList<>();
                                String arg_2 = args.get(2);

                                if (StringUtils.startsWithIgnoreCase(GHOST_SPAWN_LOCATION, arg_2)) {
                                    result.add(GHOST_SPAWN_LOCATION);
                                }
                                if (StringUtils.startsWithIgnoreCase(GHOST_IDLE_POSTION, arg_2)) {
                                    result.add(GHOST_IDLE_POSTION);
                                }
                                if (StringUtils.startsWithIgnoreCase(VEX_SPAWN_LOCATION, arg_2)) {
                                    result.add(VEX_SPAWN_LOCATION);
                                }

                                return result;
                            }
                            default -> {
                                for (Map.Entry<String, AGameSubCmd> entry : gameSubcommands.entrySet()) {
                                    if (StringUtils.startsWithIgnoreCase(entry.getKey(), args.get(1))) {
                                        args.removeFirst();
                                        args.removeFirst();
                                        return entry.getValue().tabComplete(sender, game, args);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        return List.of();
    }

    /// Because a class can't extend two other classes at the same time, we need a little helper
    private final class TopLevelGhostCmd extends Command {

        private TopLevelGhostCmd(final @NotNull String name, final @NotNull Set<@NotNull String> aliases, final @Nullable Permission permission) {
            super(name);
            setAliases(List.copyOf(aliases));
            setPermission(permission == null ? null : permission.getName());
        }

        @Override
        public boolean execute(@NotNull CommandSender sender, @NotNull String commandLabel, @NotNull String[] args) {
            return GhostCmd.this.execute(sender, new LinkedList<>(Arrays.asList(args)));
        }

        @Override
        public @NotNull List<@NotNull String> tabComplete(@NotNull CommandSender sender, @NotNull String alias, @NotNull String[] args) {
            return GhostCmd.this.tabComplete(sender, new LinkedList<>(Arrays.asList(args)));
        }
    }
}
