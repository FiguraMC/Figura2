package org.figuramc.figura.script_hooks;

import org.figuramc.figura.script_hooks.callback.CallbackType;
import org.figuramc.figura.util.enumlike.EnumLike;

import java.util.LinkedHashMap;
import java.util.List;

/**
 * Contains all events recognized by the Figura mod.
 * All Event constructors must be run before the call to EnumLike.freeze(Event.class)!
 */
public final class Event extends EnumLike {

    public final String name;
    public final CallbackType.Func type; // The type of the event signature

    public static final LinkedHashMap<String, Event> EVENTS_BY_NAME = new LinkedHashMap<>();

    public Event(String name, CallbackType... paramTypes) {
        if (EVENTS_BY_NAME.put(name, this) != null)
            throw new IllegalArgumentException("Event named \"" + name + "\" already exists! Please use some kind of disambiguation!");
        this.name = name;
        this.type = new CallbackType.Func(CallbackType.Bool.INSTANCE, paramTypes); // All events return bool
    }

    // Helper...
    public static List<? extends Event> values() { return EnumLike.values(Event.class); }

    // All event listeners return bool.
    // If it returns true, the listener is removed from the registered list.

    // () -> bool
    // Runs every client tick (20 times per second).
    public static final Event CLIENT_TICK = new Event("client_tick");

    // f32 -> bool
    // Runs every frame. The passed number is the tick progress, often called "tick delta", the progress 0 to 1 between the previous tick and the current tick.
    public static final Event CLIENT_FRAME = new Event("client_frame", CallbackType.F32.INSTANCE);

}
