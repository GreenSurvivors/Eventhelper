package de.greensurvivors.eventhelper.modules.ghost;

import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public class QuestModifier implements ConfigurationSerializable {
    private final static @NotNull String
        REQUIRED_QUEST_ID = "requiredQuestId",
        QUEST_ID = "QuestId",
        POINTS_REWARDED = "pointsReward";
    private final @NotNull String requiredQuestIdentifier;
    private final @NotNull String questIdentifier;
    private final double pointsRewarded;

    public QuestModifier(final @NotNull String questIdentifier, final @NotNull String requiredQuestIdentifier, final double pointsRewarded) {
        this.questIdentifier = questIdentifier;
        this.requiredQuestIdentifier = requiredQuestIdentifier;
        this.pointsRewarded = pointsRewarded;
    }

    public @NotNull String getRequiredQuestIdentifier() {
        return requiredQuestIdentifier;
    }

    public @NotNull String getQuestIdentifier() {
        return questIdentifier;
    }

    public double getPointsRewarded() {
        return pointsRewarded;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        } else if (obj instanceof QuestModifier questModifier) {
            return questModifier.questIdentifier.equalsIgnoreCase(this.questIdentifier);
        }

        return false;
    }

    @Override
    public @NotNull Map<@NotNull String, @NotNull Object> serialize() {
        return Map.of(
            REQUIRED_QUEST_ID, requiredQuestIdentifier,
            QUEST_ID, questIdentifier,
            POINTS_REWARDED, pointsRewarded
        );
    }

    @SuppressWarnings("unused") // used in ConfigurationSerializable
    public static @NotNull QuestModifier deserialize(@NotNull Map<String, Object> serializedMap) throws IllegalArgumentException {
        if (serializedMap.get(REQUIRED_QUEST_ID) instanceof String requiredID) {
            if (serializedMap.get(QUEST_ID) instanceof String questId) {
                if (serializedMap.get(POINTS_REWARDED) instanceof Number pointsRewarded) {
                    return new QuestModifier(questId, requiredID, pointsRewarded.doubleValue());
                } else {
                    throw new IllegalArgumentException("Could not get double for key " + POINTS_REWARDED);
                }
            } else {
                throw new IllegalArgumentException("Could not get string for key " + QUEST_ID);
            }
        } else {
            throw new IllegalArgumentException("Could not get string for key " + REQUIRED_QUEST_ID);
        }
    }
}
