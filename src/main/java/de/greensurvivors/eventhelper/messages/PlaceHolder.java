package de.greensurvivors.eventhelper.messages;

import org.jetbrains.annotations.NotNull;

public interface PlaceHolder {

    /**
     * Since this will be used in Mini-messages placeholder only the pattern "[!?#]?[a-z0-9_-]*" is valid.
     * if used inside an unparsed text you have to add surrounding <> yourself.
     */
    @NotNull String getPlaceholder();
}
