package org.figuramc.figura.util.exception;

import com.demonwav.mcdev.annotations.Translatable;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

// An exception which can be reported to chat, using Minecraft components.
// Many of the subclasses don't actually do anything except propagate the superclass constructors.
// The purpose of these subclasses is generally just to split up the exception types according to where they might appear.
public class FiguraException extends Exception {

    public final Component component;

    // Constructors taking a Component
    public FiguraException(@NotNull Component message) {
        super(message.getString());
        this.component = message;
    }
    public FiguraException(@NotNull Component message, @NotNull Throwable cause) {
        super(message.getString(), cause);
        this.component = message;
    }

    // Constructors taking translation string + args
    public FiguraException(@Translatable String translationKey, Object... args) {
        this(Component.translatable(translationKey, args));
    }
    public FiguraException(@Translatable String translationKey, @NotNull Throwable cause, Object... args) {
        this(Component.translatable(translationKey, args), cause);
    }


}
