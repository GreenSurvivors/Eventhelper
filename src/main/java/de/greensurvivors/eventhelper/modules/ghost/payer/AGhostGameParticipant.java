package de.greensurvivors.eventhelper.modules.ghost.payer;

import de.greensurvivors.eventhelper.EventHelper;
import de.greensurvivors.eventhelper.modules.ghost.GhostGame;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public abstract class AGhostGameParticipant {
    protected final @NotNull EventHelper plugin;
    private final @NotNull UUID uuid;
    private final @NotNull GhostGame game;
    protected final @NotNull PlayerData playerDataBeforeGame;

    public AGhostGameParticipant(final @NotNull EventHelper plugin,
                                 final @NotNull GhostGame game,
                                 final @NotNull UUID uuid,
                                 final @NotNull PlayerData playerDataBeforeGame) {
        this.plugin = plugin;
        this.uuid = uuid;
        this.game = game;
        this.playerDataBeforeGame = playerDataBeforeGame;
    }

    public @Nullable Player getBukkitPlayer() {
        return Bukkit.getPlayer(getUuid());
    }

    public @NotNull UUID getUuid() {
        return uuid;
    }

    public @NotNull GhostGame getGame() {
        return game;
    }

    public void restorePlayer() {
        playerDataBeforeGame.restorePlayer();
    }
}
