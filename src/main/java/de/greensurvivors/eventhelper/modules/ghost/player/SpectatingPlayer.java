package de.greensurvivors.eventhelper.modules.ghost.player;

import de.greensurvivors.eventhelper.EventHelper;
import de.greensurvivors.eventhelper.modules.ghost.GhostGame;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/// A spectator can only spectate, but not influence the game in any shape or form
public class SpectatingPlayer extends AGhostGameParticipant {

    public SpectatingPlayer(final @NotNull EventHelper plugin,
                            final @NotNull GhostGame game,
                            final @NotNull UUID uuid,
                            final @NotNull PlayerData playerData) {
        super(plugin, game, uuid, playerData);
    }
}
