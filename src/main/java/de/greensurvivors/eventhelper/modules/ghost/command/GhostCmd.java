package de.greensurvivors.eventhelper.modules.ghost.command;

import de.greensurvivors.eventhelper.EventHelper;
import de.greensurvivors.eventhelper.command.ASubCommand;
import de.greensurvivors.eventhelper.messages.StandardLangPath;
import de.greensurvivors.eventhelper.messages.StandartPlaceHolders;
import de.greensurvivors.eventhelper.modules.ghost.GhostGame;
import de.greensurvivors.eventhelper.modules.ghost.GhostLangPath;
import de.greensurvivors.eventhelper.modules.ghost.GhostModul;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class GhostCmd extends ASubCommand { // todo make toplevel command; check sub permissions!
    // arg pos0
    private static final @NotNull String CREATE = "create";
    // args pos2
    private static final @NotNull String // todo kick player; force join player; info --> game status, amount players, (info ghosts), time left, amount trapped, items delivered / points
        START_GAME = "startgame", END_GAME = "endgame", RELOAD = "reload", SET = "set", ADD = "add", REMOVE = "remove", REMOVE_NEAR = "removenear", REMOVE_ALL = "removeall";
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
        super(plugin, new Permission("eventhelper.command.ghost", "GhostCmd Command", PermissionDefault.OP));

        if (parentpermission != null) {
            super.permission.addParent(parentpermission, true);
        }

        this.ghostModul = ghostModul;
    }

    @Override
    public @NotNull Set<@NotNull String> getAliases() {
        return Set.of("ghost", "ghostgame", "gg");
    }

    @Override
    public boolean execute(final @NotNull CommandSender sender, final @NotNull List<String> args) {
        if (ghostModul.getConfig().isEnabled()) {
            if (args.size() >= 2) {
                if (args.get(0).equalsIgnoreCase(CREATE)) {
                    GhostGame newGame = ghostModul.createNewGame(args.get(1));

                    if (newGame != null) {
                        plugin.getMessageManager().sendLang(sender, GhostLangPath.COMMAND_CREATE_SUCCESS,
                            Placeholder.parsed(StandartPlaceHolders.TEXT.getPlaceholder(), newGame.getName()));
                    } else {
                        plugin.getMessageManager().sendLang(sender, GhostLangPath.COMMAND_CREATE_ERROR_EXISTS);
                        return true;
                    }
                } else {
                    final @Nullable GhostGame game = ghostModul.getGameByName(args.get(0));

                    if (game != null) {

                        switch (args.get(1).toLowerCase(Locale.ENGLISH)) {
                            case START_GAME -> {
                                game.startGame();

                                plugin.getMessageManager().sendLang(sender, GhostLangPath.COMMAND_START_SUCCESS);
                            }
                            case END_GAME -> {
                                game.endGame(GhostGame.EndReason.EXTERN);

                                plugin.getMessageManager().sendLang(sender, GhostLangPath.COMMAND_END_SUCCESS);
                            }
                            case RELOAD -> {
                                game.reload();

                                plugin.getMessageManager().sendLang(sender, GhostLangPath.COMMAND_RELOAD_SUCCESS);
                            }
                            case SET -> {
                                if (args.size() >= 3) {
                                    switch (args.get(2).toLowerCase(Locale.ENGLISH)) {
                                        case GHOST_OFFSET -> {
                                            if (args.size() >= 4) {
                                                try {
                                                    double newGhostOffset = Double.parseDouble(args.get(3));

                                                    if (newGhostOffset >= 0) {
                                                        game.getConfig().setPathfindOffset(newGhostOffset);

                                                        plugin.getMessageManager().sendLang(sender, GhostLangPath.COMMAND_SET_GHOST_OFFSET_SUCCESS,
                                                            Placeholder.unparsed(StandartPlaceHolders.NUMBER.getPlaceholder(), String.valueOf(newGhostOffset)));
                                                    } else {
                                                        plugin.getMessageManager().sendLang(sender, StandardLangPath.ARG_NUMBER_OUT_OF_BOUNDS,
                                                            Placeholder.unparsed(StandartPlaceHolders.NUMBER.getPlaceholder(), String.valueOf(newGhostOffset)),
                                                            Placeholder.unparsed(StandartPlaceHolders.MIN.getPlaceholder(), String.valueOf(0)),
                                                            Placeholder.unparsed(StandartPlaceHolders.MAX.getPlaceholder(), String.valueOf(Double.MAX_VALUE)));
                                                    }
                                                } catch (NumberFormatException e) {
                                                    plugin.getMessageManager().sendLang(sender, StandardLangPath.ARG_NOT_A_NUMBER,
                                                        Placeholder.unparsed(StandartPlaceHolders.TEXT.getPlaceholder(), args.get(3)));

                                                    plugin.getComponentLogger().debug("could not decode Integer argument {} in command to set max player amount!", args.get(3), e);

                                                    return false;
                                                }
                                            } else {
                                                plugin.getMessageManager().sendLang(sender, StandardLangPath.NOT_ENOUGH_ARGS);
                                                return false;
                                            }
                                        }
                                        case AMONT_OF_GHOSTS -> {
                                            if (args.size() >= 4) {
                                                try {
                                                    int newGhostAmount = Integer.decode(args.get(3));

                                                    if (newGhostAmount >= 1) {
                                                        game.getConfig().setAmountOfGhosts(newGhostAmount);

                                                        plugin.getMessageManager().sendLang(sender, GhostLangPath.COMMAND_SET_GHOST_AMOUNT_SUCCESS,
                                                            Placeholder.unparsed(StandartPlaceHolders.NUMBER.getPlaceholder(), String.valueOf(newGhostAmount)));
                                                    } else {
                                                        plugin.getMessageManager().sendLang(sender, StandardLangPath.ARG_NUMBER_OUT_OF_BOUNDS,
                                                            Placeholder.unparsed(StandartPlaceHolders.NUMBER.getPlaceholder(), String.valueOf(newGhostAmount)),
                                                            Placeholder.unparsed(StandartPlaceHolders.MIN.getPlaceholder(), String.valueOf(1)),
                                                            Placeholder.unparsed(StandartPlaceHolders.MAX.getPlaceholder(), String.valueOf(Double.MAX_VALUE)));
                                                    }
                                                } catch (NumberFormatException e) {
                                                    plugin.getMessageManager().sendLang(sender, StandardLangPath.ARG_NOT_A_NUMBER,
                                                        Placeholder.unparsed(StandartPlaceHolders.TEXT.getPlaceholder(), args.get(3)));

                                                    plugin.getComponentLogger().debug("could not decode Integer argument {} in command to set max player amount!", args.get(3), e);

                                                    return false;
                                                }
                                            } else {
                                                plugin.getMessageManager().sendLang(sender, StandardLangPath.NOT_ENOUGH_ARGS);
                                                return false;
                                            }
                                        }
                                        case LOBBY_LOCATION -> {
                                            if (sender instanceof Entity entity) {
                                                game.getConfig().setLobbyLocation(entity.getLocation());

                                                plugin.getMessageManager().sendLang(sender, GhostLangPath.COMMAND_SET_LOBBY_LOCATION_SUCCESS);
                                            } else {
                                                plugin.getMessageManager().sendLang(sender, StandardLangPath.NOT_A_PLAYER);
                                            }
                                        }
                                        case START_LOCATION -> {
                                            if (sender instanceof Entity entity) {
                                                game.getConfig().setStartLocation(entity.getLocation());

                                                plugin.getMessageManager().sendLang(sender, GhostLangPath.COMMAND_SET_START_LOCATION_SUCCESS);
                                            } else {
                                                plugin.getMessageManager().sendLang(sender, StandardLangPath.NOT_A_PLAYER);
                                            }
                                        }
                                        case END_LOCATION -> {
                                            if (sender instanceof Entity entity) {
                                                game.getConfig().setEndLocation(entity.getLocation());

                                                plugin.getMessageManager().sendLang(sender, GhostLangPath.COMMAND_SET_END_LOCATION_SUCCESS);
                                            } else {
                                                plugin.getMessageManager().sendLang(sender, StandardLangPath.NOT_A_PLAYER);
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

                                                            plugin.getMessageManager().sendLang(sender, GhostLangPath.COMMAND_SET_START_PLAYERTIME_SUCCESS,
                                                                Placeholder.unparsed(StandartPlaceHolders.NUMBER.getPlaceholder(), String.valueOf(newStartPlayerTime)));
                                                        } else {
                                                            plugin.getMessageManager().sendLang(sender, GhostLangPath.COMMAND_SET_START_PLAYERTIME_ERROR_LARGER_THEN_END,
                                                                Placeholder.unparsed(StandartPlaceHolders.NUMBER.getPlaceholder(), String.valueOf(newStartPlayerTime)),
                                                                Placeholder.unparsed(StandartPlaceHolders.MAX.getPlaceholder(), String.valueOf(game.getConfig().getEndPlayerTime())));
                                                        }
                                                    } else {
                                                        plugin.getMessageManager().sendLang(sender, StandardLangPath.ARG_NUMBER_OUT_OF_BOUNDS,
                                                            Placeholder.unparsed(StandartPlaceHolders.NUMBER.getPlaceholder(), String.valueOf(newStartPlayerTime)),
                                                            Placeholder.unparsed(StandartPlaceHolders.MIN.getPlaceholder(), "0"),
                                                            Placeholder.unparsed(StandartPlaceHolders.MAX.getPlaceholder(), "24000"));
                                                    }
                                                } catch (NumberFormatException e) {
                                                    plugin.getMessageManager().sendLang(sender, StandardLangPath.ARG_NOT_A_NUMBER,
                                                        Placeholder.unparsed(StandartPlaceHolders.TEXT.getPlaceholder(), args.get(3)));

                                                    plugin.getComponentLogger().debug("could not decode Integer argument {} in command to set max player amount!", args.get(3), e);

                                                    return false;
                                                }
                                            } else {
                                                plugin.getMessageManager().sendLang(sender, StandardLangPath.NOT_ENOUGH_ARGS);
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

                                                            plugin.getMessageManager().sendLang(sender, GhostLangPath.COMMAND_SET_END_PLAYERTIME_SUCCESS,
                                                                Placeholder.unparsed(StandartPlaceHolders.NUMBER.getPlaceholder(), String.valueOf(newEndPlayerTime)));
                                                        } else {
                                                            plugin.getMessageManager().sendLang(sender, GhostLangPath.COMMAND_SET_END_PLAYERTIME_ERROR_SMALLER_THEN_START,
                                                                Placeholder.unparsed(StandartPlaceHolders.NUMBER.getPlaceholder(), String.valueOf(newEndPlayerTime)),
                                                                Placeholder.unparsed(StandartPlaceHolders.MIN.getPlaceholder(), String.valueOf(game.getConfig().getEndPlayerTime())));
                                                        }
                                                    } else {
                                                        plugin.getMessageManager().sendLang(sender, StandardLangPath.ARG_NUMBER_OUT_OF_BOUNDS,
                                                            Placeholder.unparsed(StandartPlaceHolders.NUMBER.getPlaceholder(), String.valueOf(newEndPlayerTime)),
                                                            Placeholder.unparsed(StandartPlaceHolders.MIN.getPlaceholder(), "0"),
                                                            Placeholder.unparsed(StandartPlaceHolders.MAX.getPlaceholder(), "24000"));
                                                    }
                                                } catch (NumberFormatException e) {
                                                    plugin.getMessageManager().sendLang(sender, StandardLangPath.ARG_NOT_A_NUMBER,
                                                        Placeholder.unparsed(StandartPlaceHolders.TEXT.getPlaceholder(), args.get(3)));

                                                    plugin.getComponentLogger().debug("could not decode Integer argument {} in command to set max player amount!", args.get(3), e);

                                                    return false;
                                                }
                                            } else {
                                                plugin.getMessageManager().sendLang(sender, StandardLangPath.NOT_ENOUGH_ARGS);
                                                return false;
                                            }
                                        }
                                        case IS_LATE_JOIN_ALLOWED -> {
                                            if (args.size() >= 4) {

                                                Boolean isallowed = BooleanUtils.toBooleanObject(args.get(3));
                                                if (isallowed != null) {
                                                    game.getConfig().setIsLateJoinAllowed(isallowed);

                                                    plugin.getMessageManager().sendLang(sender, GhostLangPath.COMMAND_SET_LATEJOIN_SUCCESS,
                                                        Placeholder.unparsed(StandartPlaceHolders.BOOL.getPlaceholder(), String.valueOf(isallowed)));
                                                } else {
                                                    plugin.getMessageManager().sendLang(sender, StandardLangPath.ARG_NOT_A_BOOL,
                                                        Placeholder.unparsed(StandartPlaceHolders.TEXT.getPlaceholder(), args.get(3)));

                                                    return false;
                                                }
                                            } else {
                                                plugin.getMessageManager().sendLang(sender, StandardLangPath.NOT_ENOUGH_ARGS);
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

                                                            plugin.getMessageManager().sendLang(sender, GhostLangPath.COMMAND_SET_MIN_PLAYERS_SUCCESS,
                                                                Placeholder.unparsed(StandartPlaceHolders.NUMBER.getPlaceholder(), String.valueOf(newMinAmount)));
                                                        } else {
                                                            plugin.getMessageManager().sendLang(sender, GhostLangPath.COMMAND_SET_MIN_PLAYERS_ERROR_LARGER_THEN_MAX,
                                                                Placeholder.unparsed(StandartPlaceHolders.NUMBER.getPlaceholder(), String.valueOf(newMinAmount)),
                                                                Placeholder.unparsed(StandartPlaceHolders.MAX.getPlaceholder(), String.valueOf(game.getConfig().getMaxAmountPlayers())));
                                                        }
                                                    } else {
                                                        plugin.getMessageManager().sendLang(sender, StandardLangPath.ARG_NUMBER_OUT_OF_BOUNDS,
                                                            Placeholder.unparsed(StandartPlaceHolders.NUMBER.getPlaceholder(), String.valueOf(newMinAmount)),
                                                            Placeholder.unparsed(StandartPlaceHolders.MIN.getPlaceholder(), "-1"),
                                                            Placeholder.unparsed(StandartPlaceHolders.MAX.getPlaceholder(), String.valueOf(game.getConfig().getMaxAmountPlayers())));
                                                    }
                                                } catch (NumberFormatException e) {
                                                    plugin.getMessageManager().sendLang(sender, StandardLangPath.ARG_NOT_A_NUMBER,
                                                        Placeholder.unparsed(StandartPlaceHolders.TEXT.getPlaceholder(), args.get(3)));
                                                    plugin.getComponentLogger().debug("could not decode Integer argument {} in command to set min player amount!", args.get(3), e);

                                                    return false;
                                                }
                                            } else {
                                                plugin.getMessageManager().sendLang(sender, StandardLangPath.NOT_ENOUGH_ARGS);
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

                                                            plugin.getMessageManager().sendLang(sender, GhostLangPath.COMMAND_SET_MAX_PLAYERS_SUCCESS,
                                                                Placeholder.unparsed(StandartPlaceHolders.NUMBER.getPlaceholder(), String.valueOf(newMaxAmount)));
                                                        } else {
                                                            plugin.getMessageManager().sendLang(sender, GhostLangPath.COMMAND_SET_MAX_PLAYERS_ERROR_SMALLER_THEN_MIN,
                                                                Placeholder.unparsed(StandartPlaceHolders.NUMBER.getPlaceholder(), String.valueOf(newMaxAmount)),
                                                                Placeholder.unparsed(StandartPlaceHolders.MIN.getPlaceholder(), String.valueOf(game.getConfig().getMinAmountPlayers())));
                                                        }
                                                    } else {
                                                        plugin.getMessageManager().sendLang(sender, StandardLangPath.ARG_NUMBER_OUT_OF_BOUNDS,
                                                            Placeholder.unparsed(StandartPlaceHolders.NUMBER.getPlaceholder(), String.valueOf(newMaxAmount)),
                                                            Placeholder.unparsed(StandartPlaceHolders.MIN.getPlaceholder(), String.valueOf(game.getConfig().getMinAmountPlayers())),
                                                            Placeholder.unparsed(StandartPlaceHolders.MAX.getPlaceholder(), String.valueOf(Integer.MAX_VALUE)));
                                                    }
                                                } catch (NumberFormatException e) {
                                                    plugin.getMessageManager().sendLang(sender, StandardLangPath.ARG_NOT_A_NUMBER,
                                                        Placeholder.unparsed(StandartPlaceHolders.TEXT.getPlaceholder(), args.get(3)));
                                                    plugin.getComponentLogger().debug("could not decode Integer argument {} in command to set max player amount!", args.get(3), e);

                                                    return false;
                                                }
                                            } else {
                                                plugin.getMessageManager().sendLang(sender, StandardLangPath.NOT_ENOUGH_ARGS);
                                                return false;
                                            }
                                        }
                                        case PLAYER_TELEPORT_SPREAD_DISTANCE -> {
                                            if (args.size() >= 4) {
                                                try {
                                                    double newMaxSpread = Double.parseDouble(args.get(3));

                                                    if (0 <= newMaxSpread) {
                                                        game.getConfig().setPlayerSpreadDistanceTeleport(newMaxSpread);

                                                        plugin.getMessageManager().sendLang(sender, GhostLangPath.COMMAND_SET_PLAYER_SPEAD_SUCCESS,
                                                            Placeholder.unparsed(StandartPlaceHolders.NUMBER.getPlaceholder(), String.valueOf(newMaxSpread)));
                                                    } else {
                                                        plugin.getMessageManager().sendLang(sender, StandardLangPath.ARG_NUMBER_OUT_OF_BOUNDS,
                                                            Placeholder.unparsed(StandartPlaceHolders.NUMBER.getPlaceholder(), String.valueOf(newMaxSpread)),
                                                            Placeholder.unparsed(StandartPlaceHolders.MIN.getPlaceholder(), "0"),
                                                            Placeholder.unparsed(StandartPlaceHolders.MAX.getPlaceholder(), String.valueOf(Double.MAX_VALUE)));
                                                    }
                                                } catch (NumberFormatException e) {
                                                    plugin.getMessageManager().sendLang(sender, StandardLangPath.ARG_NOT_A_NUMBER,
                                                        Placeholder.unparsed(StandartPlaceHolders.TEXT.getPlaceholder(), args.get(3)));
                                                    plugin.getComponentLogger().debug("could not decode Integer argument {} in command to set player teleport max spread distance!", args.get(3), e);

                                                    return false;
                                                }
                                            } else {
                                                plugin.getMessageManager().sendLang(sender, StandardLangPath.NOT_ENOUGH_ARGS);
                                                return false;
                                            }
                                        }
                                    }
                                } else {
                                    plugin.getMessageManager().sendLang(sender, StandardLangPath.NOT_ENOUGH_ARGS);
                                    return false;
                                }
                            }
                            case ADD -> {
                                if (args.size() >= 3) {
                                    switch (args.get(2).toLowerCase(Locale.ENGLISH)) {
                                        case GHOST_SPAWN_LOCATION -> {
                                            if (sender instanceof Entity entity) {
                                                game.getConfig().addGhostSpawnLocation(entity.getLocation());

                                                plugin.getMessageManager().sendLang(sender, GhostLangPath.COMMAND_ADD_GHOST_SPAWNPOINT_SUCCESS,
                                                    Placeholder.unparsed(StandartPlaceHolders.NUMBER.getPlaceholder(), String.valueOf(game.getConfig().getGhostSpawnLocations().size())));
                                            } else {
                                                plugin.getMessageManager().sendLang(sender, StandardLangPath.NOT_A_PLAYER);
                                            }
                                        }
                                    }
                                } else {
                                    plugin.getMessageManager().sendLang(sender, StandardLangPath.NOT_ENOUGH_ARGS);
                                    return false;
                                }
                            }
                            case REMOVE -> {
                                if (args.size() >= 3) {
                                    // todo

                                } else {
                                    plugin.getMessageManager().sendLang(sender, StandardLangPath.NOT_ENOUGH_ARGS);
                                    return false;
                                }
                            }
                            case REMOVE_NEAR -> {
                                if (args.size() >= 3) {
                                    // todo

                                } else {
                                    plugin.getMessageManager().sendLang(sender, StandardLangPath.NOT_ENOUGH_ARGS);
                                    return false;
                                }
                            }
                            case REMOVE_ALL -> {
                                if (args.size() >= 3) {
                                    switch (args.get(2).toLowerCase(Locale.ENGLISH)) {
                                        case GHOST_SPAWN_LOCATION -> {
                                            game.getConfig().removeAllGhostSpawnLocations();

                                            plugin.getMessageManager().sendLang(sender, GhostLangPath.COMMAND_REMOVEALL_GHOST_SPAWNPOINT_SUCCESS);
                                        }
                                    }
                                } else {
                                    plugin.getMessageManager().sendLang(sender, StandardLangPath.NOT_ENOUGH_ARGS);
                                    return false;
                                }
                            }
                        }


                    } else {
                        plugin.getMessageManager().sendLang(sender, GhostLangPath.ARG_NOT_A_GAME);
                    }
                }
            } else {
                plugin.getMessageManager().sendLang(sender, StandardLangPath.NOT_ENOUGH_ARGS);
                return false;
            }
        } else {
            plugin.getMessageManager().sendLang(sender, GhostLangPath.ERROR_MODUL_NOT_ENABLED);
        }

        return true;
    }

    @Override
    public List<String> tabComplete(@NotNull CommandSender sender, @NotNull List<String> args) {
        switch (args.size()) {
            case 0 -> {
                return new ArrayList<>(ghostModul.getGameNames());
            }
            case 1 -> {
                List<String> result = new ArrayList<>();

                for (String gameName : ghostModul.getGameNames()) {
                    if (StringUtils.startsWithIgnoreCase(gameName, args.get(0))) {
                        result.add(gameName);
                    }
                }

                return result;
            }
            case 2 -> {
                final @Nullable GhostGame game = ghostModul.getGameByName(args.get(0));

                if (game != null) {
                    List<String> result = new ArrayList<>();

                    final String arg_1 = args.get(1);
                    if (StringUtils.startsWithIgnoreCase(START_GAME, arg_1)) {
                        result.add(START_GAME);
                    }

                    if (StringUtils.startsWithIgnoreCase(END_GAME, arg_1)) {
                        result.add(END_GAME);
                    }

                    if (StringUtils.startsWithIgnoreCase(RELOAD, arg_1)) {
                        result.add(RELOAD);
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
            case 3 -> {
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

        return List.of();
    }
}
