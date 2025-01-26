package org.figuramc.figura.util.exception;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

// An exception which can be reported to chat, using Minecraft components.
// Many of the subclasses don't actually do anything except propagate the superclass constructors.
// The purpose of these subclasses is generally just to split up the exception types according to where they might appear.
public class FiguraException extends Exception {

    public final boolean showCause; // Whether to show the cause in chat.
    private final Component component;

    public FiguraException(@NotNull Component message) {
        super((String) null);
        this.showCause = false;
        this.component = message;
    }
    public FiguraException(@NotNull Component message, @NotNull Throwable cause, boolean showCause) {
        super(cause);
        this.showCause = showCause;
        this.component = message;
    }

    // Getting component message
    public @NotNull MutableComponent getComponent() {
        return this.component.copy();
    }

    // Get messsage from the component
    public @NotNull String getMessage() { return this.component.getString(); }

    // Add nullable annotation
    public @Nullable Throwable getCause() { return super.getCause(); }

}
