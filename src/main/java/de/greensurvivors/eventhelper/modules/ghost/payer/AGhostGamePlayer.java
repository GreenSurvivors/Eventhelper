package de.greensurvivors.eventhelper.modules.ghost.payer;

import de.greensurvivors.eventhelper.EventHelper;
import de.greensurvivors.eventhelper.modules.ghost.GhostGame;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public abstract class AGhostGamePlayer extends AGhostGameParticipant {

    public AGhostGamePlayer(final @NotNull EventHelper plugin,
                            final @NotNull GhostGame game,
                            final @NotNull UUID uuid,
                            final @NotNull PlayerData playerData) {
        super(plugin, game, uuid, playerData);
    }

    public abstract @Nullable String getTask_id();

    public abstract void finishCurrentQuest();
}
