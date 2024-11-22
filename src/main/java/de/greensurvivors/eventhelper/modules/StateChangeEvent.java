package de.greensurvivors.eventhelper.modules;

import net.kyori.adventure.key.Key;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * This event gets called whenever a state change needs to get propagated up in the class tree,
 * like in case, if a module needs to be dis-/enabled
 */
public class StateChangeEvent<T> extends Event {
    private static final HandlerList handlerList = new HandlerList();
    /// the namespace represents the modul mame, the value the subcategory
    private final @NotNull Key key;
    private final T newState;

    /**
     * @param key the namespace represents the modul mame, the value the subcategory
     */
    public StateChangeEvent(final @NotNull Key key, T newState) {
        this.key = key;
        this.newState = newState;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return handlerList;
    }

    @SuppressWarnings("unused") // used by server
    public static HandlerList getHandlerList() {
        return handlerList;
    }

    /**
     * @return the namespace represents the modul mame, the value the subcategory
     */
    public @NotNull Key getKey() {
        return key;
    }

    public T getNewState() {
        return newState;
    }
}
