package de.greensurvivors.eventhelper.command.ghost;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import de.greensurvivors.eventhelper.EventHelper;
import de.greensurvivors.eventhelper.Utils;
import de.greensurvivors.eventhelper.command.ACmd;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.Location;
import org.bukkit.entity.Ghast;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.SequencedSet;
import java.util.Set;

@SuppressWarnings("UnstableApiUsage") // brigadier api
public class CreateGhost extends ACmd {

    public CreateGhost(@NotNull EventHelper plugin) {
        super(plugin, new Permission("eventhelper.cmd.createghost", "allows to create ghosts", PermissionDefault.OP));
    }

    @Override
    public @NotNull String getLabel() {
        return "create";
    }

    @Override
    public @NotNull SequencedSet<@NotNull String> getAliases() {
        return Utils.unmodifiableSequencedSetOf("new", "c");
    }

    @Override
    public int run(final @NotNull CommandContext<@NotNull CommandSourceStack> context) throws CommandSyntaxException {
        Location locOfCmd = context.getSource().getLocation();

        locOfCmd.getWorld().spawn(locOfCmd, Ghast.class, CreatureSpawnEvent.SpawnReason.COMMAND, false, ghast -> {
           ghast.setAI(false);
           ghast.setPersistent(true);
           ghast.setInvulnerable(true);
           ghast.setCollidable(false);
           ghast.setNoPhysics(true);
           ghast.setSilent(true);
        });

        context.getSource().getSender().sendMessage("spawned ghost at " + locOfCmd);

        return SINGLE_SUCCESS;
    }
}
