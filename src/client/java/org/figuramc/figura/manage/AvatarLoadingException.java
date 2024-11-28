package org.figuramc.figura.manage;

import org.figuramc.figura.script_hooks.ScriptError;

public class AvatarLoadingException extends Exception {
    public AvatarLoadingException(String message) { super(message); }
    public AvatarLoadingException(String message, Throwable cause) { super(message, cause); }

    public AvatarLoadingException(ScriptError scriptError) { this("Script error occurred: ", scriptError); }
}
