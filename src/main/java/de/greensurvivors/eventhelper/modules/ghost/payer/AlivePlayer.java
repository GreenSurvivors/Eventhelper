package de.greensurvivors.eventhelper.modules.ghost.payer;

import de.greensurvivors.eventhelper.EventHelper;
import de.greensurvivors.eventhelper.modules.ghost.GhostGame;
import de.greensurvivors.eventhelper.modules.ghost.MouseTrap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class AlivePlayer extends AGhostGamePlayer { // todo
    private final @NotNull Set<String> doneTaskIds = new HashSet<>();
    private @Nullable String currentTaskId;
    private @Nullable MouseTrap trappedIn;

    public AlivePlayer(final @NotNull EventHelper plugin,
                       final @NotNull GhostGame game,
                       final @NotNull UUID uuid) {
        super(plugin, game, uuid, new PlayerData(plugin, plugin.getServer().getPlayer(uuid)));
    }

    public void setNewTaskId(final @NotNull String newTaskId) {
        this.currentTaskId = newTaskId;
    }

    @Override
    public @Nullable String getTask_id() {
        return currentTaskId;
    }

    public void trapInMouseTrap(final @NotNull MouseTrap trap) {
        this.trappedIn = trap;
        trap.trapPlayer(this);
    }

    public @Nullable MouseTrap getMouseTrapTrappedIn() {
        return trappedIn;
    }

    @Override
    public void finishCurrentQuest() {
        doneTaskIds.add(currentTaskId);

        final Set<String> allTaskIds = getGame().getConfig().getTasks().keySet();
        final List<String> availebleTaskIds = new ArrayList<>(allTaskIds.size());

        for (String taskId : allTaskIds) {
            if (!doneTaskIds.contains(taskId)) {
                availebleTaskIds.add(taskId);
            }
        }

        if (availebleTaskIds.isEmpty()) {
            plugin.getComponentLogger().warn("Player with UUID \"{}\" has finished all available ghost game tasks!", getUuid());
            currentTaskId = null;
        } else {
            int index = ThreadLocalRandom.current().nextInt(allTaskIds.size());
            currentTaskId = availebleTaskIds.get(index);
        }
    }

    // used for dying
    public @NotNull Set<@NotNull String> generateGhostTasks() {
        final Set<String> result = new HashSet<>();

        final Set<String> allTaskIds = getGame().getConfig().getTasks().keySet();
        final List<String> availebleTaskIds = new ArrayList<>(allTaskIds.size());

        for (String taskId : allTaskIds) {
            if (!doneTaskIds.contains(taskId)) {
                availebleTaskIds.add(taskId);
            }
        }

        final int perishedTaskAmount = getGame().getConfig().getPerishedTaskAmount();
        for (int i = 0; i <= perishedTaskAmount; i++) {
            if (availebleTaskIds.isEmpty()) {
                plugin.getComponentLogger().warn("Player with UUID \"{}\" could not generate enough ghost game tasks ({} / {})!", getUuid(), result.size(), perishedTaskAmount);

                break;
            } else {
                int index = ThreadLocalRandom.current().nextInt(allTaskIds.size());
                result.add(availebleTaskIds.remove(index));
            }
        }

        return result;
    }

    // used for dying
    public @NotNull PlayerData getPlayerData() {
        return playerData;
    }
}
