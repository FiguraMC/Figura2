package org.figuramc.figura.avatars;

import com.demonwav.mcdev.annotations.Translatable;
import net.minecraft.network.chat.Component;
import org.figuramc.figura.util.exception.FiguraException;
import org.jetbrains.annotations.NotNull;

/**
 * Indicates an error that occurred with an Avatar, after it's been imported
 */
public class AvatarError extends FiguraException {
    public AvatarError(@NotNull Component message) {
        super(message);
    }

    public AvatarError(@NotNull Component message, @NotNull Throwable cause) {
        super(message, cause);
    }

    public AvatarError(@Translatable String translationKey, Object... args) {
        super(translationKey, args);
    }

    public AvatarError(@Translatable String translationKey, @NotNull Throwable cause, Object... args) {
        super(translationKey, cause, args);
    }

    // Special "Escaper" avatar error, thrown when an error has already occurred, in order to "escape" a given scope.
    // Currently used in Lua. If the script detects that its avatar has errored, throw an Escaper to escape the Lua environment.
    // It should not be caught until we're safely in java land.
    public final static class Escaper extends AvatarError {
        public Escaper() {
            super("figura.error.internal.escaper");
        }
    }

}
