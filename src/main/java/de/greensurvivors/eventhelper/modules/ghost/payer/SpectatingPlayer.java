package de.greensurvivors.eventhelper.modules.ghost.payer;

import de.greensurvivors.eventhelper.modules.ghost.GhostGame;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * while this does share much in common with AGhostGamePlayer;
 * this not extending AGhostGamePlayer is intentional so this can never get mistaken as
 * a player actively playing the game!
 */
public class SpectatingPlayer {
    private final @NotNull UUID uuid;
    private final @NotNull GhostGame game;

    public SpectatingPlayer(final @NotNull UUID uuid, @NotNull GhostGame game) {
        this.uuid = uuid;
        this.game = game;
    }

    public @Nullable Player getBukkitPlayer() {
        return Bukkit.getPlayer(uuid);
    }

    public @NotNull UUID getUuid() {
        return uuid;
    }

    public @NotNull GhostGame getGame() {
        return game;
    }
}
