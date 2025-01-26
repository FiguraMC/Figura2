package org.figuramc.figura.util.exception;

import com.demonwav.mcdev.annotations.Translatable;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

// Translatable exception
public class FiguraTranslatableException extends FiguraException {

    // NOTE: Due to a limitation of the minecraft dev plugin, all overloads MUST be vararg.
    // If we use a non-vararg overload, the plugin won't check the number of translation arguments.
    public FiguraTranslatableException(@NotNull @Translatable String translationKey, Object... args) { super(Component.translatable(translationKey, args)); }
    public FiguraTranslatableException(@NotNull @Translatable String translationKey, Throwable cause, @NotNull Boolean showCause, Object... args) { super(Component.translatable(translationKey, args), cause, showCause); }

}
