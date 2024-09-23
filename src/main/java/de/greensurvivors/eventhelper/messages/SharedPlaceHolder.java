package de.greensurvivors.eventhelper.messages;

import org.intellij.lang.annotations.Pattern;
import org.intellij.lang.annotations.Subst;
import org.jetbrains.annotations.NotNull;

/**
 * placeholder strings used. will be surrounded in Minimassage typical format of <>
 */
public enum SharedPlaceHolder implements PlaceHolder {
    ITEM("item"),
    BOOL("bool"),
    MIN("min"),
    MAX("max"),
    NUMBER("number"),
    PLAYER("player"),
    PLAYER2("other-player"),
    TEXT("text");

    @Subst("name") // substitution; will be inserted if the IDE/compiler tests if input is valid.
    private final @NotNull String placeholder;

    SharedPlaceHolder(@NotNull String placeholder) {
        this.placeholder = placeholder;
    }

    @Subst("name") // substitution; will be inserted if the IDE/compiler tests if input is valid.
    public @NotNull @Pattern("[!?#]?[a-z0-9_-]*") String getKey() {
        return placeholder;
    }
}
