package de.greensurvivors.eventhelper.modules.ghost.payer;

import de.greensurvivors.eventhelper.EventHelper;
import de.greensurvivors.eventhelper.modules.ghost.GhostGame;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

public class DeadPlayer extends AGhostGamePlayer { // todo
    private final @NotNull List<@NotNull String> stillPossibleTasks;

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

    /**
     * Importent: Check via {@link #getTask_id()} if the player has still a task left.
     * Trying to finish a task, when the task list is empty will result in an exception
     */
    @Override
    public void finishCurrentQuest() throws IndexOutOfBoundsException {
        stillPossibleTasks.remove(0);
    }

    public @NotNull List<@NotNull String> getGhostTasks() {
        return stillPossibleTasks;
    }
}
