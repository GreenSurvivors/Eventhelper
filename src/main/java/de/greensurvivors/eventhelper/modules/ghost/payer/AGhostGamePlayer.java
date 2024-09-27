package de.greensurvivors.eventhelper.modules.ghost.payer;

import de.greensurvivors.eventhelper.EventHelper;
import de.greensurvivors.eventhelper.modules.ghost.GhostGame;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public abstract class AGhostGamePlayer {
    protected final @NotNull EventHelper plugin;
    private final @NotNull UUID uuid;
    private final @NotNull GhostGame game;

    public AGhostGamePlayer(final @NotNull UUID uuid, @NotNull GhostGame game, @NotNull EventHelper plugin) {
        this.uuid = uuid;
        this.game = game;
        this.plugin = plugin;
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

    public abstract @Nullable String getTask_id();
}
