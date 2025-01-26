package org.figuramc.figura.script_hooks;

import net.minecraft.network.chat.Component;
import org.figuramc.figura.util.exception.FiguraException;
import org.jetbrains.annotations.NotNull;

/**
 * If an error occurs during script execution, throw one of these!
 */
public class ScriptError extends FiguraException {

    public ScriptError(@NotNull Component message) {
        super(message);
    }
}
