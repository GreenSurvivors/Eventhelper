package de.greensurvivors.eventhelper.modules.ghost.player;

import de.greensurvivors.eventhelper.EventHelper;
import de.greensurvivors.eventhelper.modules.ghost.*;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

public class AlivePlayer extends AGhostGamePlayer {
    private final @NotNull Set<@NotNull QuestModifier> doneTaskIds = new HashSet<>();
    private final @NotNull Map<@NotNull UnsafeArea, Duration> unsafeAreasIn = new ConcurrentHashMap<>();
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

    public void releaseFromTrap() {
        this.trappedIn = null;
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
            // in case a player has done all possible tasks just pull a random one
            plugin.getComponentLogger().debug("Player with UUID \"{}\" has finished all available ghost game tasks!", getUuid());
            setNewTaskModifier(allTaskModifiers.stream().skip((int) (allTaskModifiers.size() * ThreadLocalRandom.current().nextDouble())).findFirst().orElse(null));
        } else {
            int index = ThreadLocalRandom.current().nextInt(availebleTaskIds.size());
            setNewTaskModifier(availebleTaskIds.get(index));
        }

        return currentTaskModifier;
    }

    // used for dying
    public @NotNull List<@NotNull QuestModifier> generatePerishedTasks() {
        final List<QuestModifier> result = new ArrayList<>();

        final Collection<QuestModifier> allTaskModifiers = getGame().getConfig().getTasks().values();
        final List<QuestModifier> availebleTaskIds = new ArrayList<>(allTaskModifiers.size());

        for (QuestModifier taskId : allTaskModifiers) {
            if (!doneTaskIds.contains(taskId)) {
                availebleTaskIds.add(taskId);
            }
        }

        final int perishedTaskAmount = getGame().getConfig().getPerishedTaskAmount();
        for (int i = 0; i <= perishedTaskAmount; i++) {
            if (availebleTaskIds.isEmpty()) {
                plugin.getComponentLogger().debug("Player with UUID \"{}\" could not generate enough ghost game tasks ({} / {})!", getUuid(), result.size(), perishedTaskAmount);

                allTaskModifiers.stream().skip((int) (allTaskModifiers.size() * ThreadLocalRandom.current().nextDouble())).findFirst().ifPresent(result::add);
            } else {
                int index = ThreadLocalRandom.current().nextInt(availebleTaskIds.size());
                result.add(availebleTaskIds.remove(index));
            }
        }

        return result;
    }

    /**
     * The player keeps track of the unsafe areas they are in. If they stay long enough they will get an effect,
     * a message and may even die == get trapped
     */
    public void tickUnsafeAreas(final double millisSinceLastTick) {
        if (this.getMouseTrapTrappedIn() == null) {
            final Player bukkitPlayer = getBukkitPlayer();

            if (bukkitPlayer != null) {
                for (MouseTrap mouseTrap : getGame().getMouseTraps()) {
                    UnsafeArea unsafeArea = mouseTrap.getUnsafeArea();
                    if (unsafeArea != null) {
                        if (unsafeArea.isInArea(this)) {
                            Duration duration = unsafeAreasIn.get(unsafeArea);

                            if (duration == null) {
                                unsafeAreasIn.put(unsafeArea, Duration.ZERO);
                            } else {
                                duration = duration.plusMillis((long) millisSinceLastTick);

                                if (duration.compareTo(unsafeArea.getTimeUntilDeath()) > 0) {
                                    bukkitPlayer.setHealth(0.0);
                                } else {
                                    unsafeAreasIn.put(unsafeArea, duration);

                                    if (!unsafeArea.getWarnInterval().isNegative()) {
                                        if (duration.toMillis() % unsafeArea.getWarnInterval().toMillis() < millisSinceLastTick) {
                                            // at least 2 seconds or how long it takes to the next interval + a half second
                                            int effectDuration = Math.max(40, (int) unsafeArea.getWarnInterval().toMillis() / 50 + 10);

                                            plugin.getMessageManager().sendLang(bukkitPlayer, GhostLangPath.PLAYER_UNSAFE_AREA_WARNING);
                                            bukkitPlayer.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, effectDuration, 1, false, true, false));
                                        }
                                    }
                                }
                            }
                        } else {
                            unsafeAreasIn.remove(unsafeArea);
                        }
                    } // trap does not have an unsafe area
                }
            } else {
                throw new GhostGame.PlayerNotAliveException("Player with uuid " + getUuid() + " should tick unsafe areas but ins't a valid Bukkit player!");
            }
        }
    }
}
