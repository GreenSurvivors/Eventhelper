package de.greensurvivors.eventhelper.messages;

import org.intellij.lang.annotations.Pattern;
import org.jetbrains.annotations.NotNull;

public interface IPlaceHolder {

    /**
     * Since this will be used in Mini-messages placeholder only the pattern "[!?#]?[a-z0-9_-]*" is valid.
     * if used inside an unparsed text you have to add surrounding <> yourself.
     */
    @NotNull
    @Pattern("[!?#]?[a-z0-9_-]*")
    String getKey();
}
