package de.greensurvivors.eventhelper.modules.ghost.player;

import de.greensurvivors.eventhelper.EventHelper;
import de.greensurvivors.eventhelper.modules.ghost.GhostGame;
import de.greensurvivors.eventhelper.modules.ghost.QuestModifier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

public class DeadPlayer extends AGhostGamePlayer { // todo
    private final @NotNull List<@NotNull QuestModifier> stillPossibleTasks;

    public DeadPlayer(final @NotNull EventHelper plugin,
                      final @NotNull GhostGame game,
                      final @NotNull UUID uuid,
                      final @NotNull PlayerData playerData,
                      final @NotNull List<@NotNull QuestModifier> perishedTasks) {
        super(plugin, game, uuid, playerData);
        this.stillPossibleTasks = perishedTasks;
    }

    @Override
    public @Nullable QuestModifier getQuestModifier() {
        if (stillPossibleTasks.isEmpty()) {
            return null;
        } else {
            return stillPossibleTasks.get(0);
        }
    }

    @Override
    public @Nullable QuestModifier finishCurrentQuest() {
        if (stillPossibleTasks.isEmpty()) {
            return null;
        } else {
            stillPossibleTasks.remove(0);

            return getQuestModifier();
        }
    }

    public @NotNull List<@NotNull QuestModifier> getGhostTasks() {
        return stillPossibleTasks;
    }
}
