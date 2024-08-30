package de.greensurvivors.eventhelper.command.ghost;

import com.mojang.brigadier.Message;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.tree.LiteralCommandNode;
import de.greensurvivors.eventhelper.EventHelper;
import de.greensurvivors.eventhelper.command.ACmd;
import io.papermc.paper.adventure.AdventureComponent;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import org.bukkit.permissions.Permission;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("UnstableApiUsage") // brigadier api
public class Ghost extends ACmd {
    private final @NotNull Map<@NotNull String, @NotNull ACmd> subCommands = new HashMap<>();

    public Ghost(@NotNull EventHelper plugin) {
        super(plugin, new Permission("eventhelper.cmd.ghost.*"));

        CreateGhost createGhost = new CreateGhost(plugin);
        subCommands.put(createGhost.getLabel(), createGhost);
    }

    @Override
    public @NotNull String getLabel() {
        return "ghost";
    }

    public void register(@NotNull Commands commands, @Nullable ArgumentBuilder<CommandSourceStack, LiteralArgumentBuilder<CommandSourceStack>> parent, @Nullable Permission parentPerm){
        LiteralArgumentBuilder<CommandSourceStack> builder = Commands.literal(getLabel());

        for (ACmd subCommand : subCommands.values()){
            subCommand.register(commands, builder, permission);
        }

        LiteralCommandNode<CommandSourceStack> node = builder.build();

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

    @Override
    public int run(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Message message = new AdventureComponent(Component.text("'/eventhelper ghost' is missing arguments!"));
        throw new CommandSyntaxException(new SimpleCommandExceptionType(message), message);
    }
}
