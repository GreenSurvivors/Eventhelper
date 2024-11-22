package de.greensurvivors.eventhelper.modules.ghost.command;

import de.greensurvivors.eventhelper.EventHelper;
import de.greensurvivors.eventhelper.messages.LangPath;
import de.greensurvivors.eventhelper.messages.SharedLangPath;
import de.greensurvivors.eventhelper.messages.SharedPlaceHolder;
import de.greensurvivors.eventhelper.modules.ghost.GhostGame;
import de.greensurvivors.eventhelper.modules.ghost.GhostLangPath;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.permissions.Permissible;
import org.bukkit.permissions.Permission;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class AddSubCmd extends AGameSubCmd {

    public AddSubCmd(@NotNull EventHelper plugin, @Nullable Permission permission) {
        super(plugin, permission);
    }

    @Override
    public @NotNull Set<@NotNull String> getAliases() {
        return Set.of("add");
    }

    @Override
    public @NotNull LangPath getHelpTextPath(@NotNull Permissible permissible, @NotNull LinkedList<String> arguments) {
        return null;
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull GhostGame game, @NotNull LinkedList<@NotNull String> args) {
        if (hasPermission(sender)) {
            if (!args.isEmpty()) {
                switch (args.getFirst().toLowerCase(Locale.ENGLISH)) {
                    case GhostCmd.GHOST_SPAWN_LOCATION -> {
                        if (sender instanceof Entity entity) {
                            game.getConfig().addGhostSpawnLocation(entity.getLocation());

                            plugin.getMessageManager().sendPrefixedLang(sender,
                                GhostLangPath.MESSAGE_PREFIX, GhostLangPath.COMMAND_ADD_GHOST_SPAWNPOINT_SUCCESS,
                                Placeholder.unparsed(SharedPlaceHolder.NUMBER.getKey(), String.valueOf(game.getConfig().getGhostSpawnLocations().size())));
                        } else {
                            plugin.getMessageManager().sendPrefixedLang(sender,
                                GhostLangPath.MESSAGE_PREFIX, SharedLangPath.CMD_ERROR_SENDER_NOT_A_PLAYER);
                        }
                    }
                    case GhostCmd.GHOST_IDLE_POSTION -> {
                        if (sender instanceof Entity entity) {
                            game.getConfig().addGhostIdlePosition(entity.getLocation());

                            plugin.getMessageManager().sendPrefixedLang(sender,
                                GhostLangPath.MESSAGE_PREFIX, GhostLangPath.COMMAND_ADD_GHOST_IDLE_POS_SUCCESS,
                                Placeholder.unparsed(SharedPlaceHolder.NUMBER.getKey(), String.valueOf(game.getConfig().getGhostSpawnLocations().size())));
                        } else {
                            plugin.getMessageManager().sendPrefixedLang(sender,
                                GhostLangPath.MESSAGE_PREFIX, SharedLangPath.CMD_ERROR_SENDER_NOT_A_PLAYER);
                        }
                    }
                    case GhostCmd.VEX_SPAWN_LOCATION -> {
                        if (sender instanceof Entity entity) {
                            game.getConfig().addVexSpawnLocation(entity.getLocation());

                            plugin.getMessageManager().sendPrefixedLang(sender,
                                GhostLangPath.MESSAGE_PREFIX, GhostLangPath.COMMAND_ADD_VEX_SPAWNPOINT_SUCCESS,
                                Placeholder.unparsed(SharedPlaceHolder.NUMBER.getKey(), String.valueOf(game.getConfig().getGhostSpawnLocations().size())));
                        } else {
                            plugin.getMessageManager().sendPrefixedLang(sender,
                                GhostLangPath.MESSAGE_PREFIX, SharedLangPath.CMD_ERROR_SENDER_NOT_A_PLAYER);
                        }
                    }
                    default -> {
                        plugin.getMessageManager().sendPrefixedLang(sender,
                            GhostLangPath.MESSAGE_PREFIX, SharedLangPath.UNKNOWN_ARG,
                            Placeholder.unparsed(SharedPlaceHolder.ARGUMENT.getKey(), args.getFirst()));
                        return false;
                    }
                }
            } else {
                plugin.getMessageManager().sendPrefixedLang(sender,
                    GhostLangPath.MESSAGE_PREFIX, SharedLangPath.NOT_ENOUGH_ARGS);
                return false;
            }
        } else {
            plugin.getMessageManager().sendLang(sender, SharedLangPath.NO_PERMISSION);
        }

        return true;
    }

    @Override
    public @NotNull List<@NotNull String> tabComplete(@NotNull CommandSender sender, @NotNull GhostGame game, @NotNull LinkedList<@NotNull String> args) {
        if (args.size() == 1) {
            List<String> result = new ArrayList<>();
            final @NotNull String arg_0 = args.getFirst();

            if (StringUtils.startsWithIgnoreCase(GhostCmd.GHOST_SPAWN_LOCATION, arg_0)) {
                result.add(GhostCmd.GHOST_SPAWN_LOCATION);
            }
            if (StringUtils.startsWithIgnoreCase(GhostCmd.GHOST_IDLE_POSTION, arg_0)) {
                result.add(GhostCmd.GHOST_IDLE_POSTION);
            }
            if (StringUtils.startsWithIgnoreCase(GhostCmd.VEX_SPAWN_LOCATION, arg_0)) {
                result.add(GhostCmd.VEX_SPAWN_LOCATION);
            }

            return result;
        }

        return List.of();
    }
}
