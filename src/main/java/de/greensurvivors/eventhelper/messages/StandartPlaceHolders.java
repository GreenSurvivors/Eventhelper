package de.greensurvivors.eventhelper.messages;

import org.intellij.lang.annotations.Subst;
import org.jetbrains.annotations.NotNull;

/**
 * placeholder strings used. will be surrounded in Minimassage typical format of <>
 */
public enum StandartPlaceHolders implements PlaceHolder {
    ITEM("item"),
    BOOL("bool"),
    MIN("min"),
    MAX("max"),
    NUMBER("number"),
    PLAYER("player"),
    PLAYER2("other-player"),
    TEXT("text");

    private final @NotNull String placeholder;

    StandartPlaceHolders(@NotNull String placeholder) {
        this.placeholder = placeholder;
    }

    @Subst("name") // substitution; will be inserted if the IDE/compiler tests if input is valid.
    public @NotNull String getPlaceholder() {
        return placeholder;
    }
}
