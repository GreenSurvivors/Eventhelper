package de.greensurvivors.eventhelper.modules.ghost;

import de.greensurvivors.eventhelper.messages.LangPath;
import org.jetbrains.annotations.NotNull;

public enum GhostLangPath implements LangPath {
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
    ERROR_GAME_STATE("");


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
