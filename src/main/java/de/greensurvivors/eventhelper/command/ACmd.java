package de.greensurvivors.eventhelper.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import de.greensurvivors.eventhelper.EventHelper;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import org.bukkit.permissions.Permissible;
import org.bukkit.permissions.Permission;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

@SuppressWarnings("UnstableApiUsage") // brigadier api
public abstract class ACmd implements Command<CommandSourceStack> {
    protected final @NotNull EventHelper plugin;
    protected final @Nullable Permission permission;

    protected ACmd(@NotNull EventHelper plugin, @Nullable Permission permission) {
        this.plugin = plugin;
        this.permission = permission;
    }

    public abstract @NotNull String getLabel();

    protected @Nullable String getDescription() {
        return null;
    }

    public @NotNull SequencedSet<@NotNull String> getAliases() {
        return Collections.unmodifiableSequencedSet(new LinkedHashSet<>());
    }

    public boolean checkPermission(@NotNull Permissible permissible) {
        return permission == null || permissible.hasPermission(permission);
    }

    public void register(@NotNull Commands commands, @Nullable ArgumentBuilder<CommandSourceStack, LiteralArgumentBuilder<CommandSourceStack>> parent, @Nullable Permission parentPerm){
        LiteralCommandNode<CommandSourceStack> node = Commands.literal(getLabel()).requires(stack -> checkPermission(stack.getSender())).executes(this).build();

        if (parent == null) {
            commands.register(node, getDescription(), getAliases());
        } else {
            parent.then(node);

            for (String alias : getAliases()) {
                parent.then(Commands.literal(alias).requires(stack -> checkPermission(stack.getSender())).redirect(node).executes(this).build());
            }
        }

        if (permission != null) {
            if (parentPerm != null) {
                permission.addParent(parentPerm, true);
            }

            plugin.getServer().getPluginManager().addPermission(permission);
        }
    }
}
