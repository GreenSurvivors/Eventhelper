package de.greensurvivors.eventhelper.messages;

import org.jetbrains.annotations.NotNull;

public interface LangPath { // todo add data version to add a warning, if a key has changed
    @NotNull String getPath();

    @NotNull String getDefaultValue();

    @NotNull String getModulName();
}
