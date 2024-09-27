package de.greensurvivors.eventhelper.modules.ghost.payer;

import de.greensurvivors.eventhelper.EventHelper;
import de.greensurvivors.eventhelper.modules.ghost.GhostGame;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

public class DeadPlayer extends AGhostGamePlayer { // todo
    private final List<String> stillPossibleTasks;

    public DeadPlayer(final @NotNull EventHelper plugin,
                      final @NotNull GhostGame game,
                      final @NotNull UUID uuid,
                      final @NotNull PlayerData playerData,
                      final @NotNull List<@NotNull String> perishedTasks) {
        super(plugin, game, uuid, playerData);
        this.stillPossibleTasks = perishedTasks;
    }

    @Override
    public @Nullable String getTask_id() {
        if (stillPossibleTasks.isEmpty()) {
            return null;
        } else {
            return stillPossibleTasks.get(0);
        }
    }

    @Override
    public void finishCurrentQuest() {
        stillPossibleTasks.remove(0);
    }

    // used for spectating
    public @NotNull PlayerData getPlayerData() {
        return playerData;
    }
}
