package org.figuramc.figura.avatars;

import com.demonwav.mcdev.annotations.Translatable;
import net.minecraft.network.chat.Component;
import org.figuramc.figura.util.exception.FiguraException;
import org.jetbrains.annotations.NotNull;

/**
 * Indicates an error that occurred after the Avatar finished loading, while it was running.
 */
public class AvatarError extends FiguraException {

    public final String translationKey;
    public final Object[] args;

    public AvatarError(@NotNull @Translatable String translationKey, Object... args) {
        super(Component.empty());
        this.translationKey = translationKey;
        this.args = args;
    }
    public AvatarError(@NotNull @Translatable String translationKey, Throwable cause, @NotNull Boolean showCause, Object... args) {
        super(Component.empty(), cause, showCause);
        this.translationKey = translationKey;
        this.args = args;
    }
}
