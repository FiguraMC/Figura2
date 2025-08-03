package org.figuramc.figura.data;

import com.demonwav.mcdev.annotations.Translatable;
import org.figuramc.figura.util.exception.FiguraException;
import org.jetbrains.annotations.NotNull;

/**
 * An exception that occurs while importing a module (reading it from disk).
 * Indicates some kind of problem with the FigModel or other module folder structure.
 */
public class ModuleImportingException extends FiguraException {
    public ModuleImportingException(@NotNull @Translatable String translationKey, Object... args) { super(translationKey, args); }
    public ModuleImportingException(@NotNull @Translatable String translationKey, Throwable cause, @NotNull Boolean showCause, Object... args) { super(translationKey, cause, showCause, args); }
}
