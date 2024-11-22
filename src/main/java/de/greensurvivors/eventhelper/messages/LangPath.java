package de.greensurvivors.eventhelper.messages;

import org.jetbrains.annotations.NotNull;

public interface LangPath { // todo add data version to add a warning, if a key has changed
    /// @return the path under witch the translation value can be found
    @NotNull String getPath();

    /// @return the fallback value that should be returned if no actual value could get loaded from the lang files
    @NotNull String getDefaultValue();

    /// @return the name of the module this LangPath belongs to
    @NotNull String getModulName();
}
