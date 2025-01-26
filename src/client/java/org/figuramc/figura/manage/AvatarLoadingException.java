package org.figuramc.figura.manage;

import com.demonwav.mcdev.annotations.Translatable;
import org.figuramc.figura.util.exception.FiguraTranslatableException;
import org.jetbrains.annotations.NotNull;

public class AvatarLoadingException extends FiguraTranslatableException {
    public AvatarLoadingException(@NotNull @Translatable String translationKey, Object... args) { super(translationKey, args); }
    public AvatarLoadingException(@NotNull @Translatable String translationKey, Throwable cause, @NotNull Boolean showCause, Object... args) { super(translationKey, cause, showCause, args); }
}