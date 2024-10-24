package de.greensurvivors.eventhelper.modules.ghost;

import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.flags.Flag;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.flags.registry.FlagConflictException;
import com.sk89q.worldguard.protection.flags.registry.FlagRegistry;
import de.greensurvivors.eventhelper.EventHelper;
import de.greensurvivors.eventhelper.command.MainCmd;
import de.greensurvivors.eventhelper.messages.SharedLangPath;
import de.greensurvivors.eventhelper.messages.SharedPlaceHolder;
import de.greensurvivors.eventhelper.modules.AModul;
import de.greensurvivors.eventhelper.modules.ghost.command.GhostCmd;
import de.greensurvivors.eventhelper.modules.ghost.player.AGhostGameParticipant;
import de.greensurvivors.eventhelper.modules.ghost.player.AGhostGamePlayer;
import de.greensurvivors.eventhelper.modules.ghost.player.SpectatingPlayer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.block.sign.Side;
import org.bukkit.block.sign.SignSide;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.plugin.PluginManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

public class GhostModul extends AModul<GeneralGhostConfig> implements Listener {
    private final @NotNull Map<@NotNull String, GhostGame> games = new HashMap<>();
    protected @Nullable StateFlag ghostVexAllowedFlag;
    private final static @NotNull Permission PERMISSION_EDIT_SIGN = new Permission("eventhelper.ghost.edit-sign", PermissionDefault.OP);
    private final static @NotNull Permission PERMISSION_GHOST_WILDCARD = new Permission("eventhelper.ghost.*", PermissionDefault.OP,
        Map.of(
            PERMISSION_EDIT_SIGN.getName(), true
        )); // todo add as parent to cmds

    public GhostModul(final @NotNull EventHelper plugin) {
        super(plugin, new GeneralGhostConfig(plugin));
        this.getConfig().setModul(this);

        plugin.getMainCmd().registerSubCommand(new GhostCmd(plugin, MainCmd.getParentPermission(), this));
        plugin.getServer().getPluginManager().addPermission(PERMISSION_EDIT_SIGN);
        plugin.getServer().getPluginManager().addPermission(PERMISSION_GHOST_WILDCARD);

        FlagRegistry registry = WorldGuard.getInstance().getFlagRegistry();

        try {
            // create a flag with the name "inventory-identifier", defaulting to "default"
            StateFlag flag = new StateFlag("ghostVexAllowed", false);
            registry.register(flag);
            ghostVexAllowedFlag = flag; // only set our field if there was no error
        } catch (FlagConflictException e) {
            // some other plugin registered a flag by the same name already.
            // you can use the existing flag, but this may cause conflicts - be sure to check type
            Flag<?> existing = registry.get("ghostVexAllowed");
            if (existing instanceof StateFlag) {
                ghostVexAllowedFlag = (StateFlag) existing;
            } else {
                ghostVexAllowedFlag = null;
                // types don't match - this is bad news! some other plugin conflicts with you
                // hopefully this never actually happens
                plugin.getComponentLogger().warn("Couldn't enable Flag \"ghostVexAllowed\". Might conflict with other plugin.");
            }
        }
    }

    @Override
    public @NotNull String getName() {
        return "ghost";
    }

    @Override
    public void onEnable() {
        GhostLangPath.moduleName = getName(); // set here, not in constructor, so this class can expanded by another one if ever needed in the future
        PluginManager pluginManager = Bukkit.getPluginManager();
        games.clear();

        Path gamesPath = plugin.getDataFolder().toPath().resolve(getName()).resolve("games");
        if (Files.isDirectory(gamesPath)) {
            try (Stream<Path> stream = Files.list(gamesPath)) { // stream need to get closed

                stream.filter(Files::isRegularFile).
                    map(path -> {
                        String fileName = path.getFileName().toString();
                        return fileName.endsWith(".yaml") ? fileName.substring(0, fileName.length() - 5) : fileName;
                    }).
                    map(gameName -> new GhostGame(plugin, this, gameName)). // create a new game instance
                    forEach(game -> {
                    // register game
                    games.put(game.getName_id().toLowerCase(Locale.ENGLISH), game);
                    pluginManager.registerEvents(game, plugin);

                    // reload game
                    game.getConfig().reload();

                    // enable all now loaded mouseTraps
                    game.getMouseTraps().forEach(MouseTrap::onEnable);
                });
            } catch (IOException e) {
                plugin.getComponentLogger().error("could open ghost game directories.", e);
            }
        }

        pluginManager.registerEvents(this, plugin);
    }

    @Override
    public void onDisable() {
        for (GhostGame ghostGame : games.values()) {
            ghostGame.resetGame();
            HandlerList.unregisterAll(ghostGame);

            ghostGame.getMouseTraps().forEach(MouseTrap::onDisable);
        }

        HandlerList.unregisterAll(this);
    }

    /// returns null, if a game with the same name already exists
    public @Nullable GhostGame createNewGame(final @NotNull String gameName) {
        if (!games.containsKey(gameName.toLowerCase(Locale.ENGLISH))) {
            GhostGame newGame = new GhostGame(plugin, this, gameName);

            if (config.isEnabled()) {
                Bukkit.getPluginManager().registerEvents(newGame, plugin);
                newGame.getConfig().save().thenRun(() -> newGame.getConfig().reload()); // safe to disk
            } else {
                newGame.getConfig().save(); // safe to disk
            }

            games.put(gameName.toLowerCase(Locale.ENGLISH), newGame);

            return newGame;
        } else {
            return null;
        }
    }

    public boolean deleteGame(final @NotNull String gameName) { // todo
        final @Nullable GhostGame game = games.remove(gameName.toLowerCase(Locale.ENGLISH));

        if (game != null) {
            if (game.getGameState() != GhostGame.GameState.IDLE) {
                game.disableGame();
            }

            return true;
        } else {
            return false;
        }
    }

    public @Nullable GhostGame getGameByName(final @NotNull String gameName) {
        return games.get(gameName.toLowerCase(Locale.ENGLISH)); // todo locale??
    }

    public @NotNull Set<String> getGameNameIds() {
        return games.keySet();
    }

    /// if the player is spectating a game it will return null
    public @Nullable GhostGame getGamePlaying(@NotNull Player player) {
        AGhostGamePlayer ghostGamePlayer = getGhosteGamePlayer(player);
        return ghostGamePlayer == null ? null : ghostGamePlayer.getGame();
    }

    /// includes spectators
    public @Nullable GhostGame getGameParticipatingIn(@NotNull Player player) {
        AGhostGameParticipant ghostGameParticipant = getGhostGameParticipant(player);
        return ghostGameParticipant == null ? null : ghostGameParticipant.getGame();
    }

    public @Nullable AGhostGamePlayer getGhosteGamePlayer(@NotNull Player player) {
        for (GhostGame game : games.values()) {
            AGhostGamePlayer ghostGamePlayer = game.getGhostGamePlayer(player);
            if (ghostGamePlayer != null) {
                return ghostGamePlayer;
            }
        }

        return null;
    }

    public @Nullable AGhostGameParticipant getGhostGameParticipant(@NotNull Player player) {
        for (GhostGame game : games.values()) {
            AGhostGameParticipant ghostGameParticipant = game.getGhostGhostGameParticipant(player);
            if (ghostGameParticipant != null) {
                return ghostGameParticipant;
            }
        }

        return null;
    }

    @EventHandler(ignoreCancelled = true)
    private void onSignClick(final @NotNull PlayerInteractEvent event) { // todo permission check
        Block clickedBlock = event.getClickedBlock();
        if (clickedBlock == null) {
            return;
        }

        if (event.getAction() == Action.RIGHT_CLICK_BLOCK && clickedBlock.getState() instanceof Sign sign) {
            final @NotNull SignSide frontSide = sign.getSide(Side.FRONT);
            final @NotNull Player eventPlayer = event.getPlayer();
            if (plugin.getMessageManager().isStrippedEqualsIgnoreCase(GhostLangPath.SIGN_JOIN, frontSide.line(0))) {
                @Nullable GhostGame game = getGameByName(PlainTextComponentSerializer.plainText().serialize(frontSide.line(1)).trim());

                if (game != null) {
                    game.playerJoin(eventPlayer);
                } else {
                    plugin.getMessageManager().sendLang(eventPlayer, GhostLangPath.ARG_NOT_A_GAME,
                        Placeholder.component(SharedPlaceHolder.ARGUMENT.getKey(), frontSide.line(1)));
                }
            } else if (plugin.getMessageManager().isStrippedEqualsIgnoreCase(GhostLangPath.SIGN_SPECTATE, frontSide.line(0))) {
                @Nullable GhostGame game = getGameByName(PlainTextComponentSerializer.plainText().serialize(frontSide.line(1)).trim());

                if (game != null) {
                    game.playerSpectate(eventPlayer);
                } else {
                    plugin.getMessageManager().sendLang(eventPlayer, GhostLangPath.ARG_NOT_A_GAME,
                        Placeholder.component(SharedPlaceHolder.ARGUMENT.getKey(), frontSide.line(1)));
                }
            } else if (plugin.getMessageManager().isStrippedEqualsIgnoreCase(GhostLangPath.SIGN_QUIT, frontSide.line(0))) {
                GhostGame game = getGameParticipatingIn(eventPlayer);

                if (game != null) {
                    game.playerQuit(eventPlayer, true);
                    plugin.getMessageManager().sendLang(eventPlayer, GhostLangPath.PLAYER_GAME_QUIT);
                } else {
                    plugin.getMessageManager().sendLang(eventPlayer, GhostLangPath.ERROR_NOT_PLAYING_SELF);
                }
            }
        } else {
            AGhostGameParticipant participant = getGhostGameParticipant(event.getPlayer());
            if (participant instanceof SpectatingPlayer) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    private void onSignEdit(final @NotNull SignChangeEvent event) {
        if (event.getSide() == Side.FRONT) {
            @Nullable Component firstLine = event.line(0);

            if (firstLine != null) {
                if (plugin.getMessageManager().isStrippedEqualsIgnoreCase(GhostLangPath.SIGN_JOIN, firstLine)) {
                    if (event.getPlayer().hasPermission(PERMISSION_EDIT_SIGN)) {
                        Component secondLine = event.line(1);

                        if (secondLine != null) {
                            @Nullable GhostGame game = getGameByName(PlainTextComponentSerializer.plainText().serialize(secondLine).trim());

                            if (game != null) {
                                event.line(0, plugin.getMessageManager().getLang(GhostLangPath.SIGN_JOIN));

                                if (event.getBlock().getState() instanceof Sign sign) {
                                    sign.setWaxed(true);
                                    sign.update();
                                }

                                plugin.getMessageManager().sendLang(event.getPlayer(), GhostLangPath.SIGN_CREATED_JOIN,
                                    Placeholder.component(SharedPlaceHolder.TEXT.getKey(), secondLine));
                            } else {
                                plugin.getMessageManager().sendLang(event.getPlayer(), GhostLangPath.ERROR_SIGN_CREATE_INVALID_GAME,
                                    Placeholder.component(SharedPlaceHolder.TEXT.getKey(), secondLine));
                                event.setCancelled(true);
                            }
                        } else {
                            plugin.getMessageManager().sendLang(event.getPlayer(), GhostLangPath.ERROR_SIGN_CREATE_INVALID_GAME,
                                Placeholder.component(SharedPlaceHolder.TEXT.getKey(), Component.empty()));
                            event.setCancelled(true);
                        }
                    } else {
                        plugin.getMessageManager().sendLang(event.getPlayer(), SharedLangPath.NO_PERMISSION);
                        event.setCancelled(true);
                    }
                } else if (plugin.getMessageManager().isStrippedEqualsIgnoreCase(GhostLangPath.SIGN_SPECTATE, firstLine)) {
                    if (event.getPlayer().hasPermission(PERMISSION_EDIT_SIGN)) {
                        Component secondLine = event.line(1);

                        if (secondLine != null) {
                            @Nullable GhostGame game = getGameByName(PlainTextComponentSerializer.plainText().serialize(secondLine).trim());

                            if (game != null) {
                                event.line(0, plugin.getMessageManager().getLang(GhostLangPath.SIGN_SPECTATE));

                                if (event.getBlock().getState() instanceof Sign sign) {
                                    sign.setWaxed(true);
                                    sign.update();
                                }

                                plugin.getMessageManager().sendLang(event.getPlayer(), GhostLangPath.SIGN_CREATED_SPECTATE,
                                    Placeholder.component(SharedPlaceHolder.TEXT.getKey(), secondLine));
                            } else {
                                plugin.getMessageManager().sendLang(event.getPlayer(), GhostLangPath.ERROR_SIGN_CREATE_INVALID_GAME,
                                    Placeholder.component(SharedPlaceHolder.TEXT.getKey(), secondLine));
                                event.setCancelled(true);
                            }
                        } else {
                            plugin.getMessageManager().sendLang(event.getPlayer(), GhostLangPath.ERROR_SIGN_CREATE_INVALID_GAME,
                                Placeholder.component(SharedPlaceHolder.TEXT.getKey(), Component.empty()));
                            event.setCancelled(true);
                        }
                    } else {
                        plugin.getMessageManager().sendLang(event.getPlayer(), SharedLangPath.NO_PERMISSION);
                        event.setCancelled(true);
                    }
                } else if (plugin.getMessageManager().isStrippedEqualsIgnoreCase(GhostLangPath.SIGN_QUIT, firstLine)) {
                    if (event.getPlayer().hasPermission(PERMISSION_EDIT_SIGN)) {
                        event.line(0, plugin.getMessageManager().getLang(GhostLangPath.SIGN_QUIT));

                        if (event.getBlock().getState() instanceof Sign sign) {
                            sign.setWaxed(true);
                            sign.update();
                        }

                        plugin.getMessageManager().sendLang(event.getPlayer(), GhostLangPath.SIGN_CREATED_QUIT);
                    } else {
                        plugin.getMessageManager().sendLang(event.getPlayer(), SharedLangPath.NO_PERMISSION);
                        event.setCancelled(true);
                    }
                }
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    private void spectatorDropItem(@NotNull PlayerDropItemEvent event) {
        AGhostGameParticipant participant = getGhostGameParticipant(event.getPlayer());
        if (participant instanceof SpectatingPlayer) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    private void spectatorPickUpItem(@NotNull EntityPickupItemEvent event) {
        if (event.getEntity() instanceof Player player) {
            AGhostGameParticipant participant = getGhostGameParticipant(player);
            if (participant instanceof SpectatingPlayer) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    private void spectatorHurt(@NotNull EntityDamageEvent event) {
        if (event.getEntity() instanceof Player player) {
            AGhostGameParticipant participant = getGhostGameParticipant(player);
            if (participant instanceof SpectatingPlayer) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    private void spectatorAttack(@NotNull EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player player) {
            AGhostGameParticipant participant = getGhostGameParticipant(player);
            if (participant instanceof SpectatingPlayer) {
                event.setCancelled(true);
            }
        }
    }
}
