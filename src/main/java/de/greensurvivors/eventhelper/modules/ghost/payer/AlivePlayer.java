package de.greensurvivors.eventhelper.modules.ghost.payer;

import de.greensurvivors.eventhelper.EventHelper;
import de.greensurvivors.eventhelper.modules.ghost.GhostGame;
import de.greensurvivors.eventhelper.modules.ghost.MouseTrap;
import de.greensurvivors.eventhelper.modules.ghost.QuestModifier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class AlivePlayer extends AGhostGamePlayer { // todo
    private final @NotNull Set<QuestModifier> doneTaskIds = new HashSet<>();
    private @Nullable QuestModifier currentTaskModifier;
    private @Nullable MouseTrap trappedIn;

    public AlivePlayer(final @NotNull EventHelper plugin,
                       final @NotNull GhostGame game,
                       final @NotNull UUID uuid) {
        super(plugin, game, uuid, new PlayerData(plugin, plugin.getServer().getPlayer(uuid)));

        final Collection<QuestModifier> allTaskIds = getGame().getConfig().getTasks().values();

        if (allTaskIds.isEmpty()) {
            plugin.getComponentLogger().warn("Player with UUID \"{}\" has finished all available ghost game tasks, while still joining the game!", getUuid());
            currentTaskModifier = null;
        } else {
            setNewTaskModifier(allTaskIds.stream().skip((int) (allTaskIds.size() * ThreadLocalRandom.current().nextDouble())).findFirst().orElse(null));
        }
    }

    /**
     * creates a new Alive player with new player data, untrapped, but with the same task progress
     * Used when a player rejoins a game
     */
    public AlivePlayer(final @NotNull AlivePlayer alivePlayer) {
        super(alivePlayer.plugin, alivePlayer.getGame(), alivePlayer.getUuid(), new PlayerData(alivePlayer.plugin, alivePlayer.getBukkitPlayer()));
        this.doneTaskIds.addAll(alivePlayer.doneTaskIds);

        if (alivePlayer.currentTaskModifier == null || getGame().getConfig().getTasks().containsKey(alivePlayer.currentTaskModifier.getQuestIdentifier())) {
            this.currentTaskModifier = alivePlayer.currentTaskModifier;
        } else {
            finishCurrentQuest();
        }
    }

    public void setNewTaskModifier(final @Nullable QuestModifier questModifier) {
        this.currentTaskModifier = questModifier;
    }

    @Override
    public @Nullable QuestModifier getQuestModifier() {
        return currentTaskModifier;
    }

    public boolean trapInMouseTrap(final @NotNull MouseTrap trap) {
        this.trappedIn = trap;
        return trap.trapPlayer(this);
    }

    public @Nullable MouseTrap getMouseTrapTrappedIn() {
        return trappedIn;
    }

    @Override
    public @Nullable QuestModifier finishCurrentQuest() {
        doneTaskIds.add(currentTaskModifier);

        final Collection<QuestModifier> allTaskModifiers = getGame().getConfig().getTasks().values();
        final List<QuestModifier> availebleTaskIds = new ArrayList<>(allTaskModifiers.size());

        for (QuestModifier taskId : allTaskModifiers) {
            if (!doneTaskIds.contains(taskId)) {
                availebleTaskIds.add(taskId);
            }
        }

        if (availebleTaskIds.isEmpty()) {
            plugin.getComponentLogger().warn("Player with UUID \"{}\" has finished all available ghost game tasks!", getUuid());
            currentTaskModifier = null;
        } else {
            int index = ThreadLocalRandom.current().nextInt(allTaskModifiers.size());
            setNewTaskModifier(availebleTaskIds.get(index));
        }

        return currentTaskModifier;
    }

    // used for dying
    public @NotNull List<@NotNull QuestModifier> generateGhostTasks() {
        final List<QuestModifier> result = new ArrayList<>();

        final Collection<QuestModifier> allTaskIds = getGame().getConfig().getTasks().values();
        final List<QuestModifier> availebleTaskIds = new ArrayList<>(allTaskIds.size());

        for (QuestModifier taskId : allTaskIds) {
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
}
