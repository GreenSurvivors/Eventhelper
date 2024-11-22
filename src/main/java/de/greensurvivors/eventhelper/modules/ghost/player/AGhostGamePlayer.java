package de.greensurvivors.eventhelper.modules.ghost.player;

import de.greensurvivors.eventhelper.EventHelper;
import de.greensurvivors.eventhelper.modules.ghost.GhostGame;
import de.greensurvivors.eventhelper.modules.ghost.QuestModifier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/// A participant that is still part of the game and can change its outcome
public abstract class AGhostGamePlayer extends AGhostGameParticipant {

    public AGhostGamePlayer(final @NotNull EventHelper plugin,
                            final @NotNull GhostGame game,
                            final @NotNull UUID uuid,
                            final @NotNull PlayerData playerData) {
        super(plugin, game, uuid, playerData);
    }

    /// get the current QuestModifier or null if this player has none
    public abstract @Nullable QuestModifier getQuestModifier();

    /**
     * finish the current quest
     *
     * @return returns the next one
     */
    public abstract @Nullable QuestModifier finishCurrentQuest();

    public @NotNull PlayerData getPlayerData() {
        return playerDataBeforeGame;
    }
}
