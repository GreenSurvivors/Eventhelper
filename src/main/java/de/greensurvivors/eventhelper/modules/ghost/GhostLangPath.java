package de.greensurvivors.eventhelper.modules.ghost;

import de.greensurvivors.eventhelper.messages.LangPath;
import org.jetbrains.annotations.NotNull;

public enum GhostLangPath implements LangPath { // todo
    COMMAND_START_SUCCESS(""),
    COMMAND_END_SUCCESS(""),
    COMMAND_RELOAD_SUCCESS(""),
    COMMAND_SET_GHOST_OFFSET_SUCCESS(""),
    COMMAND_SET_GHOST_AMOUNT_SUCCESS(""),
    COMMAND_SET_LOBBY_LOCATION_SUCCESS(""),
    COMMAND_SET_START_LOCATION_SUCCESS(""),
    COMMAND_SET_END_LOCATION_SUCCESS(""),
    COMMAND_SET_START_PLAYERTIME_SUCCESS(""),
    COMMAND_SET_START_PLAYERTIME_ERROR_LARGER_THEN_END(""),
    COMMAND_SET_END_PLAYERTIME_SUCCESS(""),
    COMMAND_SET_END_PLAYERTIME_ERROR_SMALLER_THEN_START(""),
    COMMAND_SET_LATEJOIN_SUCCESS(""),
    COMMAND_SET_MIN_PLAYERS_SUCCESS(""),
    COMMAND_SET_MIN_PLAYERS_ERROR_LARGER_THEN_MAX(""),
    COMMAND_SET_MAX_PLAYERS_SUCCESS(""),
    COMMAND_SET_MAX_PLAYERS_ERROR_SMALLER_THEN_MIN(""),
    COMMAND_SET_PLAYER_SPEAD_SUCCESS(""),
    COMMAND_ADD_GHOST_SPAWNPOINT_SUCCESS(""),
    COMMAND_REMOVEALL_GHOST_SPAWNPOINT_SUCCESS(""),

    PLAYER_GAME_JOIN(""),
    PLAYER_GAME_JOIN_BROADCAST(""),
    PLAYER_GAME_QUIT(""),
    PLAYER_GAME_QUIT_BROADCAST(""),
    GAME_WIN_BROADCAST(""),
    GAME_LOOSE_TIME_BROADCAST(""),
    GAME_LOOSE_DEATH_BROADCAST(""),
    ERROR_GAME_FULL(""),
    ERROR_NO_LATE_JOIN(""),
    ERROR_ALREADY_PLAYING(""),
    ERROR_NOT_PLAYING(""),
    ERROR_GAME_STATE(""),
    ARG_NOT_A_GAME("");


    private final @NotNull String path;
    private final @NotNull String defaultValue;

    GhostLangPath(@NotNull String path) {
        this.path = path;
        this.defaultValue = path; // we don't need to define a default value, but if something couldn't get loaded we have to return at least helpful information
    }

    GhostLangPath(@NotNull String path, @NotNull String defaultValue) {
        this.path = path;
        this.defaultValue = defaultValue;
    }

    public @NotNull String getPath() {
        return path;
    }

    public @NotNull String getDefaultValue() {
        return defaultValue;
    }
}
