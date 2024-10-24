package de.greensurvivors.eventhelper.modules.ghost;

import de.greensurvivors.eventhelper.messages.LangPath;
import org.jetbrains.annotations.NotNull;

public record GhostLangPath(@NotNull String path, @NotNull String defaultValue) implements LangPath { // todo
    public static final GhostLangPath
        MESSAGE_PREFIX = new GhostLangPath("message-prefix", "[GhostGame]"),
        COMMAND_CREATE_ERROR_EXISTS = new GhostLangPath("command.create.game.error.exists"),
        COMMAND_CREATE_SUCCESS = new GhostLangPath("command.create.success"),
        COMMAND_START_SUCCESS = new GhostLangPath("command.start.success"),
        COMMAND_END_SUCCESS = new GhostLangPath("command.end.success"),
        COMMAND_RELOAD_GAME_SUCCESS = new GhostLangPath("command.reload.game.success"),
        COMMAND_SET_GHOST_OFFSET_SUCCESS = new GhostLangPath("command.set.ghost.offset.success"),
        COMMAND_SET_GHOST_AMOUNT_SUCCESS = new GhostLangPath("command.set.ghost.amount.success"),
        COMMAND_SET_LOBBY_LOCATION_SUCCESS = new GhostLangPath("command.set.lobby.success"),
        COMMAND_SET_START_LOCATION_SUCCESS = new GhostLangPath("command.set.start.success"),
        COMMAND_SET_END_LOCATION_SUCCESS = new GhostLangPath("command.set.end.success"),
        COMMAND_SET_START_PLAYERTIME_SUCCESS = new GhostLangPath("command.set.player-time.start.success"),
        COMMAND_SET_START_PLAYERTIME_ERROR_LARGER_THEN_END = new GhostLangPath("command.set.player-time.start.error.larger-then-end"),
        COMMAND_SET_END_PLAYERTIME_SUCCESS = new GhostLangPath("command.set.player-time.end.success"),
        COMMAND_SET_END_PLAYERTIME_ERROR_SMALLER_THEN_START = new GhostLangPath("command.set.player-time.end.error.smaller-then-start"),
        COMMAND_SET_LATEJOIN_SUCCESS = new GhostLangPath("command.set.late-join.success"),
        COMMAND_SET_MIN_PLAYERS_SUCCESS = new GhostLangPath("command.min-players.success"),
        COMMAND_SET_MIN_PLAYERS_ERROR_LARGER_THEN_MAX = new GhostLangPath("command.set.min-players.error.larger-then-max"),
        COMMAND_SET_MAX_PLAYERS_SUCCESS = new GhostLangPath("command.set.max-players.success"),
        COMMAND_SET_MAX_PLAYERS_ERROR_SMALLER_THEN_MIN = new GhostLangPath("command.set.max-players.error.smaller-then-min"),
        COMMAND_SET_PLAYER_SPEAD_SUCCESS = new GhostLangPath("command.set.player-spread.success"),
        COMMAND_ADD_GHOST_SPAWNPOINT_SUCCESS = new GhostLangPath("command.add.ghost.spawnpoint.success"),
        COMMAND_ADD_VEX_SPAWNPOINT_SUCCESS = new GhostLangPath("command.add.vex.spawnpoint.success"),
        COMMAND_REMOVEALL_GHOST_SPAWNPOINT_SUCCESS = new GhostLangPath("command.remove-all.ghost.spawnpoint.success"),
        COMMAND_REMOVEALL_VEX_SPAWNPOINT_SUCCESS = new GhostLangPath("command.remove-all.vex.spawnpoint.success"),
        COMMAD_HELP_TEXT = new GhostLangPath("command.help-text"),

    PLAYER_GAME_JOIN = new GhostLangPath("game.player.join.self"), // todo per game override
        PLAYER_GAME_JOIN_BROADCAST = new GhostLangPath("game.player.join.broadcast"), // todo per game override
        PLAYER_GAME_QUIT = new GhostLangPath("game.player.quit.self"), // todo per game override
        PLAYER_GAME_QUIT_BROADCAST = new GhostLangPath("game.player.quit.broadcast"), // todo per game override
        PLAYER_CAPTURED = new GhostLangPath("game.player.trap.trapped"),
        PLAYER_TRAP_TIME_REMAINING = new GhostLangPath("game.player.trap.time.remaining"),
        PLAYER_TRAP_PERISH = new GhostLangPath("game.player.trap.perish"),
        PLAYER_TRAP_RELEASE_BROADCAST = new GhostLangPath("game.player.trap.release.broadcast"),
        PLAYER_TRAP_ONLY_ALIVE_CAN_RELEASE = new GhostLangPath("game.player.trap.release.error.not-alive"),
        PLAYER_PERISHED_TASK_DONE_BROADCAST = new GhostLangPath("game.player.perished.tasks-done.broadcast"),
        PLAYER_PERISHED_TASK_DONE_SELF = new GhostLangPath("game.player.perished.tasks-done.self"),
        GAME_WIN_BROADCAST = new GhostLangPath("game.win.broadcast"), // todo per game override
        GAME_LOOSE_TIME_BROADCAST = new GhostLangPath("game.loose.time.broadcast"), // todo per game override
        GAME_LOOSE_DEATH_BROADCAST = new GhostLangPath("game.loose.death.broadcast"), // todo per game override
        ERROR_GAME_FULL = new GhostLangPath("error.game-full"),
        ERROR_NO_LATE_JOIN = new GhostLangPath("error.no-late-join"),
        ERROR_NO_REJOIN = new GhostLangPath("error.no-rejion"),
        ERROR_ALREADY_PARTICIPATING = new GhostLangPath("error.already-participating"),
        ERROR_NOT_PLAYING_SELF = new GhostLangPath("error.not-playing.self"),
        ERROR_JOIN_GAME_STATE = new GhostLangPath("error.join.game-state"),
        ERROR_MODUL_NOT_ENABLED = new GhostLangPath("error.module-not-enabled"),
        ARG_NOT_A_GAME = new GhostLangPath("error.arg.not-a-game"),
        GAME_POINTS_MSG = new GhostLangPath("game.points.message"),
        GAME_POINTS_MILESTONE_25 = new GhostLangPath("game.points.milestone.25"),
        GAME_POINTS_MILESTONE_50 = new GhostLangPath("game.points.milestone.50"),
        GAME_POINTS_MILESTONE_75 = new GhostLangPath("game.points.milestone.75"),
        GAME_POINTS_MILESTONE_90 = new GhostLangPath("game.points.milestone.90"),
        GAME_COUNTDOWN = new GhostLangPath("game.countdown"),

    SIGN_JOIN = new GhostLangPath("game.sign.join", "[ghost join]"),
        SIGN_SPECTATE = new GhostLangPath("game.sign.spectate", "[ghost spectate]"),
        SIGN_QUIT = new GhostLangPath("game.sign.quit", "[ghost quit]"),
        ERROR_SIGN_CREATE_INVALID_GAME = new GhostLangPath("error.sign.create.invalid-game"),
        SIGN_CREATED_JOIN = new GhostLangPath("game.sign.create.join"),
        SIGN_CREATED_SPECTATE = new GhostLangPath("game.sign.create.spectate"),
        SIGN_CREATED_QUIT = new GhostLangPath("game.sign.create.quit");

    static @NotNull String moduleName = "ghost";

    private GhostLangPath(final @NotNull String path) {
        this(path, path);
    }

    public @NotNull String getPath() {
        return path;
    }

    public @NotNull String getDefaultValue() {
        return defaultValue;
    }

    @Override
    public @NotNull String getModulName() {
        return moduleName;
    }
}
