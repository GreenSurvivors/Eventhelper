package de.greensurvivors.eventhelper.command;

import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import de.greensurvivors.eventhelper.EventHelper;
import de.greensurvivors.eventhelper.Utils;
import de.greensurvivors.eventhelper.command.ghost.Ghost;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.permissions.Permission;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

@SuppressWarnings("UnstableApiUsage") // brigadier api
public class MainCmd extends ACmd {
    private final @NotNull SequencedMap<@NotNull String, @NotNull ACmd> subCommands = new LinkedHashMap<>();

    public MainCmd(final @NotNull EventHelper plugin) {
        super(plugin, new Permission("eventhelper.cmd.*"));

        Ghost createGhost = new Ghost(plugin);
        subCommands.put(createGhost.getLabel(), createGhost);
    }

    @Override
    public @NotNull String getLabel() {
        return "eventhelper";
    }

    @Override
    public @NotNull SequencedSet<@NotNull String> getAliases() {
        return Utils.unmodifiableSequencedSetOf("evh", "ehelper");
    }

    public void register(@NotNull Commands commands, @Nullable ArgumentBuilder<CommandSourceStack, LiteralArgumentBuilder<CommandSourceStack>> parent, @Nullable Permission parentPerm){
        LiteralArgumentBuilder<CommandSourceStack> builder = Commands.literal(getLabel());

        builder.executes(this);

        for (ACmd subCommand : subCommands.values()){
            subCommand.register(commands, builder, permission);
        }

        commands.register(builder.build(), getDescription(), getAliases());

        if (permission != null) {
            if (parentPerm != null) {
                permission.addParent(parentPerm, true);
            }

            plugin.getServer().getPluginManager().addPermission(permission);
        }
    }

    @Override
    public int run(CommandContext<CommandSourceStack> context) {
        context.getSource().getSender().sendMessage("need help? use /eventhelper help.");
        return SINGLE_SUCCESS;
    }
}
