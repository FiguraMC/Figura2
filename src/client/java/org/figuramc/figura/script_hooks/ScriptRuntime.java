package org.figuramc.figura.script_hooks;

import org.figuramc.figura.avatars.AvatarError;
import org.figuramc.figura.avatars.AvatarModules;

/**
 * An interface to be implemented by any language to be added.
 * The mod itself will only depend upon this interface, and not implementation
 * details of any one runtime.
 * <p>
 * Script Runtimes are memory-countable and will be used as tracing roots.
 */
public interface ScriptRuntime {

    /**
     * Run the snippet of code which was typed into chat through "/figura run".
     * Not all engines necessarily need to support this, it's fine to just print
     * an error to chat here if the language doesn't easily support a REPL.
     */
    void runCode(String snippet) throws AvatarError;

    /**
     * Run when the Avatar is destroyed. Should clean up any native resources used
     * by the runtime.
     */
    void destroy();

    /**
     * Initialize the given module (which uses this runtime)
     */
    void initModule(AvatarModules.Module module) throws AvatarError;

}
