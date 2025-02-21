package org.figuramc.figura.data;

import com.demonwav.mcdev.annotations.Translatable;
import org.figuramc.figura.util.exception.FiguraTranslatableException;
import org.jetbrains.annotations.NotNull;

/**
 * An exception that occurs while importing the avatar (reading it from disk).
 * Indicates some kind of problem with the FigModel or other avatar folder structure.
 */
public class AvatarImportingException extends FiguraTranslatableException {
    public AvatarImportingException(@NotNull @Translatable String translationKey, Object... args) { super(translationKey, args); }
}
