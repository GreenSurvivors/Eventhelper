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
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

// todo join, spectate command
// todo rework this once Bridgadier is a thing
public class GhostCmd extends ASubCommand { // todo make toplevel command; check sub permissions!; list --> lists all games; ghost locations; commands
    private final @NotNull Permission START_PERM = new Permission("eventhelper.ghost.command.startgame", PermissionDefault.OP, Map.of(super.permission.getName(), Boolean.TRUE));
    private final @NotNull Permission END_PERM = new Permission("eventhelper.ghost.command.endgame", PermissionDefault.OP, Map.of(super.permission.getName(), Boolean.TRUE));
    private final @NotNull Permission RELOAD_PERM = new Permission("eventhelper.ghost.command.reload", PermissionDefault.OP, Map.of(super.permission.getName(), Boolean.TRUE));
    private final @NotNull Permission EDIT_PERM = new Permission("eventhelper.ghost.command.edit", PermissionDefault.OP, Map.of(super.permission.getName(), Boolean.TRUE));
    // arg pos0
    private static final @NotNull String CREATE = "create";
    // args pos2
    private static final @NotNull String // todo kick player; force join player; info --> game status, amount players, (info ghosts), time left, amount trapped, items delivered / points
        START_GAME = "startgame", END_GAME = "endgame", RELOAD = "reload", SET = "set", ADD = "add", REMOVE = "remove", REMOVE_NEAR = "removenear", REMOVE_ALL = "removeall", QUIT = "quit";
    // args pos3
    private static final @NotNull String
        GHOST_OFFSET = "ghostoffset",
        GHOST_SPAWN_LOCATION = "ghostspawnlocation",
        AMONT_OF_GHOSTS = "ghostamount",
        LOBBY_LOCATION = "lobbylocation",
        START_LOCATION = "startlocation",
        END_LOCATION = "endlocation",
        GAME_DURATION = "gameduration",
        START_PLAYERTIME = "startplayertime",
        END_PLAYERTIME = "endplayertime",
        IS_LATE_JOIN_ALLOWED = "islatejoinallowed",
        MIN_AMOUNT_PLAYERS = "minamountplayers",
        MAX_AMOUNT_PLAYERS = "maxamountplayers",
        PLAYER_TELEPORT_SPREAD_DISTANCE = "playerteleportspreaddistance";
    private final @NotNull GhostModul ghostModul;

    public GhostCmd(final @NotNull EventHelper plugin, final @Nullable Permission parentpermission, @NotNull GhostModul ghostModul) {
        super(plugin, new Permission("eventhelper.ghost.command", "GhostCmd Command", PermissionDefault.OP));

        if (parentpermission != null) {
            super.permission.addParent(parentpermission, true);
        }

        this.ghostModul = ghostModul;

        plugin.getServer().getPluginManager().addPermission(START_PERM);
        plugin.getServer().getPluginManager().addPermission(END_PERM);
        plugin.getServer().getPluginManager().addPermission(RELOAD_PERM);
        plugin.getServer().getPluginManager().addPermission(EDIT_PERM);

        final @NotNull Permission ADMIN_PERM = new Permission("eventhelper.ghost.command.*", PermissionDefault.OP, Map.of(
            START_PERM.getName(), Boolean.TRUE,
            END_PERM.getName(), Boolean.TRUE,
            RELOAD_PERM.getName(), Boolean.TRUE,
            EDIT_PERM.getName(), Boolean.TRUE
        ));
        plugin.getServer().getPluginManager().addPermission(ADMIN_PERM);
    }

    @Override
    public @NotNull Set<@NotNull String> getAliases() {
        return Set.of("ghost", "ghostgame", "gg");
    }

    @Override
    public @NotNull LangPath getHelpTextPath() {
        return GhostLangPath.COMMAD_HELP_TEXT;
    }

    @Override
    public boolean execute(final @NotNull CommandSender sender, final @NotNull LinkedList<@NotNull String> args) {
        if (ghostModul.getConfig().isEnabled()) {
            if (args.size() >= 2) {
                if (args.get(0).equalsIgnoreCase(QUIT)) {
                    if (sender instanceof Player player) {
                        GhostGame game = ghostModul.getGameParticipatingIn(player);

                        if (game != null) {
                            game.playerQuit(player, true);
                            plugin.getMessageManager().sendLang(player, GhostLangPath.PLAYER_GAME_QUIT);
                        } else {
                            plugin.getMessageManager().sendLang(player, GhostLangPath.ERROR_NOT_PLAYING_SELF);
                        }
                    } else {
                        plugin.getMessageManager().sendLang(sender, SharedLangPath.CMD_ERROR_SENDER_NOT_A_PLAYER);
                    }
                } else if (args.get(0).equalsIgnoreCase(CREATE)) {
                    GhostGame newGame = ghostModul.createNewGame(args.get(1));

                    if (newGame != null) {
                        plugin.getMessageManager().sendPrefixedLang(sender,
                            GhostLangPath.MESSAGE_PREFIX, GhostLangPath.COMMAND_CREATE_SUCCESS,
                            Placeholder.parsed(SharedPlaceHolder.TEXT.getKey(), newGame.getName_id()));
                    } else {
                        plugin.getMessageManager().sendPrefixedLang(sender,
                            GhostLangPath.MESSAGE_PREFIX, GhostLangPath.COMMAND_CREATE_ERROR_EXISTS,
                            Placeholder.unparsed(SharedPlaceHolder.ARGUMENT.getKey(), args.get(1)));
                        return true;
                    }
                } else {
                    final @Nullable GhostGame game = ghostModul.getGameByName(args.get(0));

                    if (game != null) {
                        switch (args.get(1).toLowerCase(Locale.ENGLISH)) {
                            case START_GAME -> {
                                if (sender.hasPermission(START_PERM)) {
                                    game.startStartingCountdown();

                                    plugin.getMessageManager().sendPrefixedLang(sender,
                                        GhostLangPath.MESSAGE_PREFIX, GhostLangPath.COMMAND_START_SUCCESS,
                                        Placeholder.unparsed(SharedPlaceHolder.TEXT.getKey(), game.getName_id()));
                                } else {
                                    plugin.getMessageManager().sendLang(sender, SharedLangPath.NO_PERMISSION);
                                }
                            }
                            case END_GAME -> {
                                if (sender.hasPermission(END_PERM)) {
                                    game.endGame(GhostGame.EndReason.EXTERN);

                                    plugin.getMessageManager().sendPrefixedLang(sender,
                                        GhostLangPath.MESSAGE_PREFIX, GhostLangPath.COMMAND_END_SUCCESS,
                                        Placeholder.unparsed(SharedPlaceHolder.TEXT.getKey(), game.getName_id()));
                                } else {
                                    plugin.getMessageManager().sendLang(sender, SharedLangPath.NO_PERMISSION);
                                }
                            }
                            case RELOAD -> {
                                if (sender.hasPermission(RELOAD_PERM)) {
                                    game.reload();

                                    plugin.getMessageManager().sendPrefixedLang(sender,
                                        GhostLangPath.MESSAGE_PREFIX, GhostLangPath.COMMAND_RELOAD_GAME_SUCCESS,
                                        Placeholder.component(SharedPlaceHolder.TEXT.getKey(), game.getConfig().getDisplayName()));
                                } else {
                                    plugin.getMessageManager().sendLang(sender, SharedLangPath.NO_PERMISSION);
                                }
                            }
                            case SET -> {
                                if (sender.hasPermission(END_PERM)) {
                                    if (args.size() >= 3) {
                                        switch (args.get(2).toLowerCase(Locale.ENGLISH)) {
                                            case GHOST_OFFSET -> {
                                                if (args.size() >= 4) {
                                                    try {
                                                        double newGhostOffset = Double.parseDouble(args.get(3));

                                                        if (newGhostOffset >= 0) {
                                                            game.getConfig().setPathfindOffset(newGhostOffset);

                                                            plugin.getMessageManager().sendPrefixedLang(sender,
                                                                GhostLangPath.MESSAGE_PREFIX, GhostLangPath.COMMAND_SET_GHOST_OFFSET_SUCCESS,
                                                                Placeholder.unparsed(SharedPlaceHolder.NUMBER.getKey(), String.valueOf(newGhostOffset)));
                                                        } else {
                                                            plugin.getMessageManager().sendPrefixedLang(sender,
                                                                GhostLangPath.MESSAGE_PREFIX, SharedLangPath.ARG_NUMBER_OUT_OF_BOUNDS,
                                                                Placeholder.unparsed(SharedPlaceHolder.NUMBER.getKey(), String.valueOf(newGhostOffset)),
                                                                Placeholder.unparsed(SharedPlaceHolder.MIN.getKey(), String.valueOf(0)),
                                                                Placeholder.unparsed(SharedPlaceHolder.MAX.getKey(), String.valueOf(Double.MAX_VALUE)));
                                                        }
                                                    } catch (NumberFormatException e) {
                                                        plugin.getMessageManager().sendPrefixedLang(sender,
                                                            GhostLangPath.MESSAGE_PREFIX, SharedLangPath.ARG_NOT_A_NUMBER,
                                                            Placeholder.unparsed(SharedPlaceHolder.ARGUMENT.getKey(), args.get(3)));

                                                        plugin.getComponentLogger().debug("could not decode Integer argument {} in command to set max player amount!", args.get(3), e);

                                                        return false;
                                                    }
                                                } else {
                                                    plugin.getMessageManager().sendPrefixedLang(sender,
                                                        GhostLangPath.MESSAGE_PREFIX, SharedLangPath.NOT_ENOUGH_ARGS);
                                                    return false;
                                                }
                                            }
                                            case AMONT_OF_GHOSTS -> {
                                                if (args.size() >= 4) {
                                                    try {
                                                        int newGhostAmount = Integer.decode(args.get(3));

                                                        if (newGhostAmount >= 1) {
                                                            game.getConfig().setAmountOfGhosts(newGhostAmount);

                                                            plugin.getMessageManager().sendPrefixedLang(sender,
                                                                GhostLangPath.MESSAGE_PREFIX, GhostLangPath.COMMAND_SET_GHOST_AMOUNT_SUCCESS,
                                                                Placeholder.unparsed(SharedPlaceHolder.NUMBER.getKey(), String.valueOf(newGhostAmount)));
                                                        } else {
                                                            plugin.getMessageManager().sendPrefixedLang(sender,
                                                                GhostLangPath.MESSAGE_PREFIX, SharedLangPath.ARG_NUMBER_OUT_OF_BOUNDS,
                                                                Placeholder.unparsed(SharedPlaceHolder.NUMBER.getKey(), String.valueOf(newGhostAmount)),
                                                                Placeholder.unparsed(SharedPlaceHolder.MIN.getKey(), String.valueOf(1)),
                                                                Placeholder.unparsed(SharedPlaceHolder.MAX.getKey(), String.valueOf(Double.MAX_VALUE)));
                                                        }
                                                    } catch (NumberFormatException e) {
                                                        plugin.getMessageManager().sendPrefixedLang(sender,
                                                            GhostLangPath.MESSAGE_PREFIX, SharedLangPath.ARG_NOT_A_NUMBER,
                                                            Placeholder.unparsed(SharedPlaceHolder.ARGUMENT.getKey(), args.get(3)));

                                                        plugin.getComponentLogger().debug("could not decode Integer argument {} in command to set max player amount!", args.get(3), e);

                                                        return false;
                                                    }
                                                } else {
                                                    plugin.getMessageManager().sendPrefixedLang(sender,
                                                        GhostLangPath.MESSAGE_PREFIX, SharedLangPath.NOT_ENOUGH_ARGS);
                                                    return false;
                                                }
                                            }
                                            case LOBBY_LOCATION -> {
                                                if (sender instanceof Entity entity) {
                                                    game.getConfig().setLobbyLocation(entity.getLocation());

                                                    plugin.getMessageManager().sendPrefixedLang(sender,
                                                        GhostLangPath.MESSAGE_PREFIX, GhostLangPath.COMMAND_SET_LOBBY_LOCATION_SUCCESS);
                                                } else {
                                                    plugin.getMessageManager().sendPrefixedLang(sender,
                                                        GhostLangPath.MESSAGE_PREFIX, SharedLangPath.CMD_ERROR_SENDER_NOT_A_PLAYER);
                                                }
                                            }
                                            case START_LOCATION -> {
                                                if (sender instanceof Entity entity) {
                                                    game.getConfig().setPlayerStartLocation(entity.getLocation());

                                                    plugin.getMessageManager().sendPrefixedLang(sender,
                                                        GhostLangPath.MESSAGE_PREFIX, GhostLangPath.COMMAND_SET_START_LOCATION_SUCCESS);
                                                } else {
                                                    plugin.getMessageManager().sendPrefixedLang(sender,
                                                        GhostLangPath.MESSAGE_PREFIX, SharedLangPath.CMD_ERROR_SENDER_NOT_A_PLAYER);
                                                }
                                            }
                                            case END_LOCATION -> {
                                                if (sender instanceof Entity entity) {
                                                    game.getConfig().setEndLocation(entity.getLocation());

                                                    plugin.getMessageManager().sendPrefixedLang(sender,
                                                        GhostLangPath.MESSAGE_PREFIX, GhostLangPath.COMMAND_SET_END_LOCATION_SUCCESS);
                                                } else {
                                                    plugin.getMessageManager().sendPrefixedLang(sender,
                                                        GhostLangPath.MESSAGE_PREFIX, SharedLangPath.CMD_ERROR_SENDER_NOT_A_PLAYER);
                                                }
                                            }
                                            case GAME_DURATION -> {
                                                // todo
                                            }
                                            case START_PLAYERTIME -> {
                                                if (args.size() >= 4) {
                                                    try {
                                                        int newStartPlayerTime = Integer.decode(args.get(3));

                                                        if (0 <= newStartPlayerTime && newStartPlayerTime <= 24000) {
                                                            if (newStartPlayerTime <= game.getConfig().getEndPlayerTime()) {
                                                                game.getConfig().setStartPlayerTime(newStartPlayerTime);

                                                                plugin.getMessageManager().sendPrefixedLang(sender,
                                                                    GhostLangPath.MESSAGE_PREFIX, GhostLangPath.COMMAND_SET_START_PLAYERTIME_SUCCESS,
                                                                    Placeholder.unparsed(SharedPlaceHolder.NUMBER.getKey(), String.valueOf(newStartPlayerTime)));
                                                            } else {
                                                                plugin.getMessageManager().sendPrefixedLang(sender,
                                                                    GhostLangPath.MESSAGE_PREFIX, GhostLangPath.COMMAND_SET_START_PLAYERTIME_ERROR_LARGER_THEN_END,
                                                                    Placeholder.unparsed(SharedPlaceHolder.NUMBER.getKey(), String.valueOf(newStartPlayerTime)),
                                                                    Placeholder.unparsed(SharedPlaceHolder.MAX.getKey(), String.valueOf(game.getConfig().getEndPlayerTime())));
                                                            }
                                                        } else {
                                                            plugin.getMessageManager().sendPrefixedLang(sender,
                                                                GhostLangPath.MESSAGE_PREFIX, SharedLangPath.ARG_NUMBER_OUT_OF_BOUNDS,
                                                                Placeholder.unparsed(SharedPlaceHolder.NUMBER.getKey(), String.valueOf(newStartPlayerTime)),
                                                                Placeholder.unparsed(SharedPlaceHolder.MIN.getKey(), "0"),
                                                                Placeholder.unparsed(SharedPlaceHolder.MAX.getKey(), "24000"));
                                                        }
                                                    } catch (NumberFormatException e) {
                                                        plugin.getMessageManager().sendPrefixedLang(sender,
                                                            GhostLangPath.MESSAGE_PREFIX, SharedLangPath.ARG_NOT_A_NUMBER,
                                                            Placeholder.unparsed(SharedPlaceHolder.ARGUMENT.getKey(), args.get(3)));

                                                        plugin.getComponentLogger().debug("could not decode Integer argument {} in command to set max player amount!", args.get(3), e);

                                                        return false;
                                                    }
                                                } else {
                                                    plugin.getMessageManager().sendPrefixedLang(sender,
                                                        GhostLangPath.MESSAGE_PREFIX, SharedLangPath.NOT_ENOUGH_ARGS);
                                                    return false;
                                                }
                                            }
                                            case END_PLAYERTIME -> {
                                                if (args.size() >= 4) {
                                                    try {
                                                        int newEndPlayerTime = Integer.decode(args.get(3));

                                                        if (0 <= newEndPlayerTime && newEndPlayerTime <= 24000) {
                                                            if (newEndPlayerTime >= game.getConfig().getStartPlayerTime()) {
                                                                game.getConfig().setEndPlayerTime(newEndPlayerTime);

                                                                plugin.getMessageManager().sendPrefixedLang(sender,
                                                                    GhostLangPath.MESSAGE_PREFIX, GhostLangPath.COMMAND_SET_END_PLAYERTIME_SUCCESS,
                                                                    Placeholder.unparsed(SharedPlaceHolder.NUMBER.getKey(), String.valueOf(newEndPlayerTime)));
                                                            } else {
                                                                plugin.getMessageManager().sendPrefixedLang(sender,
                                                                    GhostLangPath.MESSAGE_PREFIX, GhostLangPath.COMMAND_SET_END_PLAYERTIME_ERROR_SMALLER_THEN_START,
                                                                    Placeholder.unparsed(SharedPlaceHolder.NUMBER.getKey(), String.valueOf(newEndPlayerTime)),
                                                                    Placeholder.unparsed(SharedPlaceHolder.MIN.getKey(), String.valueOf(game.getConfig().getEndPlayerTime())));
                                                            }
                                                        } else {
                                                            plugin.getMessageManager().sendPrefixedLang(sender,
                                                                GhostLangPath.MESSAGE_PREFIX, SharedLangPath.ARG_NUMBER_OUT_OF_BOUNDS,
                                                                Placeholder.unparsed(SharedPlaceHolder.NUMBER.getKey(), String.valueOf(newEndPlayerTime)),
                                                                Placeholder.unparsed(SharedPlaceHolder.MIN.getKey(), "0"),
                                                                Placeholder.unparsed(SharedPlaceHolder.MAX.getKey(), "24000"));
                                                        }
                                                    } catch (NumberFormatException e) {
                                                        plugin.getMessageManager().sendPrefixedLang(sender,
                                                            GhostLangPath.MESSAGE_PREFIX, SharedLangPath.ARG_NOT_A_NUMBER,
                                                            Placeholder.unparsed(SharedPlaceHolder.ARGUMENT.getKey(), args.get(3)));

                                                        plugin.getComponentLogger().debug("could not decode Integer argument {} in command to set max player amount!", args.get(3), e);

                                                        return false;
                                                    }
                                                } else {
                                                    plugin.getMessageManager().sendPrefixedLang(sender,
                                                        GhostLangPath.MESSAGE_PREFIX, SharedLangPath.NOT_ENOUGH_ARGS);
                                                    return false;
                                                }
                                            }
                                            case IS_LATE_JOIN_ALLOWED -> {
                                                if (args.size() >= 4) {

                                                    Boolean isallowed = BooleanUtils.toBooleanObject(args.get(3));
                                                    if (isallowed != null) {
                                                        game.getConfig().setIsLateJoinAllowed(isallowed);

                                                        plugin.getMessageManager().sendPrefixedLang(sender,
                                                            GhostLangPath.MESSAGE_PREFIX, GhostLangPath.COMMAND_SET_LATEJOIN_SUCCESS,
                                                            Placeholder.unparsed(SharedPlaceHolder.BOOL.getKey(), String.valueOf(isallowed)));
                                                    } else {
                                                        plugin.getMessageManager().sendPrefixedLang(sender,
                                                            GhostLangPath.MESSAGE_PREFIX, SharedLangPath.ARG_NOT_A_BOOL,
                                                            Placeholder.unparsed(SharedPlaceHolder.ARGUMENT.getKey(), args.get(3)));

                                                        return false;
                                                    }
                                                } else {
                                                    plugin.getMessageManager().sendPrefixedLang(sender,
                                                        GhostLangPath.MESSAGE_PREFIX, SharedLangPath.NOT_ENOUGH_ARGS);
                                                    return false;
                                                }
                                            }
                                            case MIN_AMOUNT_PLAYERS -> {
                                                if (args.size() >= 4) {
                                                    try {
                                                        int newMinAmount = Integer.decode(args.get(3));

                                                        if (-1 <= newMinAmount) {
                                                            if (newMinAmount <= game.getConfig().getMaxAmountPlayers()) {
                                                                game.getConfig().setMinAmountPlayers(newMinAmount);

                                                                plugin.getMessageManager().sendPrefixedLang(sender,
                                                                    GhostLangPath.MESSAGE_PREFIX, GhostLangPath.COMMAND_SET_MIN_PLAYERS_SUCCESS,
                                                                    Placeholder.unparsed(SharedPlaceHolder.NUMBER.getKey(), String.valueOf(newMinAmount)));
                                                            } else {
                                                                plugin.getMessageManager().sendPrefixedLang(sender,
                                                                    GhostLangPath.MESSAGE_PREFIX, GhostLangPath.COMMAND_SET_MIN_PLAYERS_ERROR_LARGER_THEN_MAX,
                                                                    Placeholder.unparsed(SharedPlaceHolder.NUMBER.getKey(), String.valueOf(newMinAmount)),
                                                                    Placeholder.unparsed(SharedPlaceHolder.MAX.getKey(), String.valueOf(game.getConfig().getMaxAmountPlayers())));
                                                            }
                                                        } else {
                                                            plugin.getMessageManager().sendPrefixedLang(sender,
                                                                GhostLangPath.MESSAGE_PREFIX, SharedLangPath.ARG_NUMBER_OUT_OF_BOUNDS,
                                                                Placeholder.unparsed(SharedPlaceHolder.NUMBER.getKey(), String.valueOf(newMinAmount)),
                                                                Placeholder.unparsed(SharedPlaceHolder.MIN.getKey(), "-1"),
                                                                Placeholder.unparsed(SharedPlaceHolder.MAX.getKey(), String.valueOf(game.getConfig().getMaxAmountPlayers())));
                                                        }
                                                    } catch (NumberFormatException e) {
                                                        plugin.getMessageManager().sendPrefixedLang(sender,
                                                            GhostLangPath.MESSAGE_PREFIX, SharedLangPath.ARG_NOT_A_NUMBER,
                                                            Placeholder.unparsed(SharedPlaceHolder.ARGUMENT.getKey(), args.get(3)));
                                                        plugin.getComponentLogger().debug("could not decode Integer argument {} in command to set min player amount!", args.get(3), e);

                                                        return false;
                                                    }
                                                } else {
                                                    plugin.getMessageManager().sendPrefixedLang(sender,
                                                        GhostLangPath.MESSAGE_PREFIX, SharedLangPath.NOT_ENOUGH_ARGS);
                                                    return false;
                                                }
                                            }
                                            case MAX_AMOUNT_PLAYERS -> {
                                                if (args.size() >= 4) {
                                                    try {
                                                        int newMaxAmount = Integer.decode(args.get(3));

                                                        if (-1 <= newMaxAmount) {
                                                            if (newMaxAmount <= game.getConfig().getMaxAmountPlayers()) {
                                                                game.getConfig().setMaxAmountPlayers(newMaxAmount);

                                                                plugin.getMessageManager().sendPrefixedLang(sender,
                                                                    GhostLangPath.MESSAGE_PREFIX, GhostLangPath.COMMAND_SET_MAX_PLAYERS_SUCCESS,
                                                                    Placeholder.unparsed(SharedPlaceHolder.NUMBER.getKey(), String.valueOf(newMaxAmount)));
                                                            } else {
                                                                plugin.getMessageManager().sendPrefixedLang(sender,
                                                                    GhostLangPath.MESSAGE_PREFIX, GhostLangPath.COMMAND_SET_MAX_PLAYERS_ERROR_SMALLER_THEN_MIN,
                                                                    Placeholder.unparsed(SharedPlaceHolder.NUMBER.getKey(), String.valueOf(newMaxAmount)),
                                                                    Placeholder.unparsed(SharedPlaceHolder.MIN.getKey(), String.valueOf(game.getConfig().getMinAmountPlayers())));
                                                            }
                                                        } else {
                                                            plugin.getMessageManager().sendPrefixedLang(sender,
                                                                GhostLangPath.MESSAGE_PREFIX, SharedLangPath.ARG_NUMBER_OUT_OF_BOUNDS,
                                                                Placeholder.unparsed(SharedPlaceHolder.NUMBER.getKey(), String.valueOf(newMaxAmount)),
                                                                Placeholder.unparsed(SharedPlaceHolder.MIN.getKey(), String.valueOf(game.getConfig().getMinAmountPlayers())),
                                                                Placeholder.unparsed(SharedPlaceHolder.MAX.getKey(), String.valueOf(Integer.MAX_VALUE)));
                                                        }
                                                    } catch (NumberFormatException e) {
                                                        plugin.getMessageManager().sendPrefixedLang(sender,
                                                            GhostLangPath.MESSAGE_PREFIX, SharedLangPath.ARG_NOT_A_NUMBER,
                                                            Placeholder.unparsed(SharedPlaceHolder.ARGUMENT.getKey(), args.get(3)));
                                                        plugin.getComponentLogger().debug("could not decode Integer argument {} in command to set max player amount!", args.get(3), e);

                                                        return false;
                                                    }
                                                } else {
                                                    plugin.getMessageManager().sendPrefixedLang(sender,
                                                        GhostLangPath.MESSAGE_PREFIX, SharedLangPath.NOT_ENOUGH_ARGS);
                                                    return false;
                                                }
                                            }
                                            case PLAYER_TELEPORT_SPREAD_DISTANCE -> {
                                                if (args.size() >= 4) {
                                                    try {
                                                        double newMaxSpread = Double.parseDouble(args.get(3));

                                                        if (0 <= newMaxSpread) {
                                                            game.getConfig().setPlayerSpreadDistanceTeleport(newMaxSpread);

                                                            plugin.getMessageManager().sendPrefixedLang(sender,
                                                                GhostLangPath.MESSAGE_PREFIX, GhostLangPath.COMMAND_SET_PLAYER_SPEAD_SUCCESS,
                                                                Placeholder.unparsed(SharedPlaceHolder.NUMBER.getKey(), String.valueOf(newMaxSpread)));
                                                        } else {
                                                            plugin.getMessageManager().sendPrefixedLang(sender,
                                                                GhostLangPath.MESSAGE_PREFIX, SharedLangPath.ARG_NUMBER_OUT_OF_BOUNDS,
                                                                Placeholder.unparsed(SharedPlaceHolder.NUMBER.getKey(), String.valueOf(newMaxSpread)),
                                                                Placeholder.unparsed(SharedPlaceHolder.MIN.getKey(), "0"),
                                                                Placeholder.unparsed(SharedPlaceHolder.MAX.getKey(), String.valueOf(Double.MAX_VALUE)));
                                                        }
                                                    } catch (NumberFormatException e) {
                                                        plugin.getMessageManager().sendPrefixedLang(sender,
                                                            GhostLangPath.MESSAGE_PREFIX, SharedLangPath.ARG_NOT_A_NUMBER,
                                                            Placeholder.unparsed(SharedPlaceHolder.ARGUMENT.getKey(), args.get(3)));
                                                        plugin.getComponentLogger().debug("could not decode Integer argument {} in command to set player teleport max spread distance!", args.get(3), e);

                                                        return false;
                                                    }
                                                } else {
                                                    plugin.getMessageManager().sendPrefixedLang(sender,
                                                        GhostLangPath.MESSAGE_PREFIX, SharedLangPath.NOT_ENOUGH_ARGS);
                                                    return false;
                                                }
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
                            case ADD -> {
                                if (sender.hasPermission(EDIT_PERM)) {
                                    if (args.size() >= 3) {
                                        switch (args.get(2).toLowerCase(Locale.ENGLISH)) {
                                            case GHOST_SPAWN_LOCATION -> {
                                                if (sender instanceof Entity entity) {
                                                    game.getConfig().addGhostSpawnLocation(entity.getLocation());

                                                    plugin.getMessageManager().sendPrefixedLang(sender,
                                                        GhostLangPath.MESSAGE_PREFIX, GhostLangPath.COMMAND_ADD_GHOST_SPAWNPOINT_SUCCESS,
                                                        Placeholder.unparsed(SharedPlaceHolder.NUMBER.getKey(), String.valueOf(game.getConfig().getGhostSpawnLocations().size())));
                                                } else {
                                                    plugin.getMessageManager().sendPrefixedLang(sender,
                                                        GhostLangPath.MESSAGE_PREFIX, SharedLangPath.CMD_ERROR_SENDER_NOT_A_PLAYER);
                                                }
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
                        }
                    } else {
                        plugin.getMessageManager().sendPrefixedLang(sender,
                            GhostLangPath.MESSAGE_PREFIX, GhostLangPath.ARG_NOT_A_GAME,
                            Placeholder.unparsed(SharedPlaceHolder.ARGUMENT.getKey(), args.get(0)));
                    }
                }
            } else {
                if (args.size() == 1 && args.get(0).equalsIgnoreCase(QUIT)) {
                    if (sender instanceof Player player) {
                        GhostGame game = ghostModul.getGameParticipatingIn(player);

                        if (game != null) {
                            game.playerQuit(player, true);
                            plugin.getMessageManager().sendLang(player, GhostLangPath.PLAYER_GAME_QUIT);
                        } else {
                            plugin.getMessageManager().sendLang(player, GhostLangPath.ERROR_NOT_PLAYING_SELF);
                        }
                    } else {
                        plugin.getMessageManager().sendLang(sender, SharedLangPath.CMD_ERROR_SENDER_NOT_A_PLAYER);
                    }

                    return true;
                } else {

                    plugin.getMessageManager().sendPrefixedLang(sender,
                        GhostLangPath.MESSAGE_PREFIX, SharedLangPath.NOT_ENOUGH_ARGS);
                    return false;
                }
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
                if (sender.hasPermission(EDIT_PERM)) {
                    Set<String> gameIds = ghostModul.getGameNameIds();
                    List<String> result = new ArrayList<>(gameIds.size() + 1);
                    result.add(QUIT);
                    result.add(CREATE); // fist;
                    result.addAll(gameIds);

                    return result;
                } else {
                    return List.of(QUIT);
                }
            }
            case 1 -> {
                final String arg_0 = args.get(0);
                if (sender.hasPermission(EDIT_PERM)) {
                    Set<String> gameIds = ghostModul.getGameNameIds();

                    List<String> result = new ArrayList<>(gameIds.size());

                    if (StringUtils.startsWithIgnoreCase(CREATE, arg_0)) {
                        result.add(CREATE);
                    }

                    for (String gameNameId : gameIds) {
                        if (StringUtils.startsWithIgnoreCase(gameNameId, arg_0)) {
                            result.add(gameNameId);
                        }
                    }

                    return result;
                } else {
                    return StringUtils.startsWithIgnoreCase(QUIT, arg_0) ? List.of(QUIT) : List.of();
                }
            }
            case 2 -> {
                if (sender.hasPermission(EDIT_PERM)) {
                    final @Nullable GhostGame game = ghostModul.getGameByName(args.get(0));

                    if (game != null) {
                        List<String> result = new ArrayList<>();

                        final String arg_1 = args.get(1);
                        if (StringUtils.startsWithIgnoreCase(START_GAME, arg_1)) {
                            if (sender.hasPermission(START_PERM)) {
                                result.add(START_GAME);
                            }
                        }

                        if (StringUtils.startsWithIgnoreCase(END_GAME, arg_1)) {
                            if (sender.hasPermission(END_PERM)) {
                                result.add(END_GAME);
                            }
                        }

                        if (StringUtils.startsWithIgnoreCase(RELOAD, arg_1)) {
                            if (sender.hasPermission(RELOAD_PERM)) {
                                result.add(RELOAD);
                            }
                        }

                        if (StringUtils.startsWithIgnoreCase(SET, arg_1)) {
                            result.add(SET);
                        }

                        if (StringUtils.startsWithIgnoreCase(ADD, arg_1)) {
                            result.add(ADD);
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
                    final @Nullable GhostGame game = ghostModul.getGameByName(args.get(0));

                    if (game != null) {
                        switch (args.get(1)) {
                            case SET -> {
                                List<String> result = new ArrayList<>();
                                String arg_2 = args.get(2);

                                if (StringUtils.startsWithIgnoreCase(GHOST_OFFSET, arg_2)) {
                                    result.add(GHOST_OFFSET);
                                }
                                if (StringUtils.startsWithIgnoreCase(AMONT_OF_GHOSTS, arg_2)) {
                                    result.add(AMONT_OF_GHOSTS);
                                }
                                if (StringUtils.startsWithIgnoreCase(LOBBY_LOCATION, arg_2)) {
                                    result.add(LOBBY_LOCATION);
                                }
                                if (StringUtils.startsWithIgnoreCase(START_LOCATION, arg_2)) {
                                    result.add(START_LOCATION);
                                }
                                if (StringUtils.startsWithIgnoreCase(END_LOCATION, arg_2)) {
                                    result.add(END_LOCATION);
                                }
                                if (StringUtils.startsWithIgnoreCase(GAME_DURATION, arg_2)) {
                                    result.add(GAME_DURATION);
                                }
                                if (StringUtils.startsWithIgnoreCase(START_PLAYERTIME, arg_2)) {
                                    result.add(START_PLAYERTIME);
                                }
                                if (StringUtils.startsWithIgnoreCase(END_PLAYERTIME, arg_2)) {
                                    result.add(END_PLAYERTIME);
                                }
                                if (StringUtils.startsWithIgnoreCase(IS_LATE_JOIN_ALLOWED, arg_2)) {
                                    result.add(IS_LATE_JOIN_ALLOWED);
                                }
                                if (StringUtils.startsWithIgnoreCase(MAX_AMOUNT_PLAYERS, arg_2)) {
                                    result.add(MAX_AMOUNT_PLAYERS);
                                }
                                if (StringUtils.startsWithIgnoreCase(PLAYER_TELEPORT_SPREAD_DISTANCE, arg_2)) {
                                    result.add(PLAYER_TELEPORT_SPREAD_DISTANCE);
                                }

                                return result;
                            }
                            case ADD -> {
                                List<String> result = new ArrayList<>();
                                String arg_2 = args.get(2);

                                if (StringUtils.startsWithIgnoreCase(GHOST_SPAWN_LOCATION, arg_2)) {
                                    result.add(GHOST_SPAWN_LOCATION);
                                }

                                return result;
                            }
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

                                return result;
                            }
                        }
                    }
                }
            }
        }

        return List.of();
    }
}
