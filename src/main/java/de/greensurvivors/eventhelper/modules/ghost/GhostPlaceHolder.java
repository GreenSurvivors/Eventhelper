package de.greensurvivors.eventhelper.modules.ghost;

import de.greensurvivors.eventhelper.messages.IPlaceHolder;
import org.intellij.lang.annotations.Pattern;
import org.intellij.lang.annotations.Subst;
import org.jetbrains.annotations.NotNull;

public enum GhostPlaceHolder implements IPlaceHolder {
    GAME_STATUS("game_status"),
    AMOUNT_PLAYERS_ALIVE("alive_players"),
    AMOUNT_PLAYERS_TRAPPED("trapped_players"),
    AMOUNT_PLAYERS_DEAD("dead_players"),
    POINTS_CURRENT("current_points"),
    POINTS_MAX("max_points");

    private final @NotNull
    @Pattern("[!?#]?[a-z0-9_-]*") String key;

    GhostPlaceHolder(@NotNull @Pattern("[!?#]?[a-z0-9_-]*") String key) {
        this.key = key;
    }

    @Subst("name") // substitution; will be inserted if the IDE/compiler tests if input is valid.
    @Override
    public @NotNull @Pattern("[!?#]?[a-z0-9_-]*") String getKey() {
        return key;
    }
}
