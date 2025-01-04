package org.figuramc.figura.script_hooks;

import org.figuramc.figura.script_hooks.mem_count.MemoryCountable;

/**
 * An interface to be implemented by any language to be added.
 * The mod itself will only depend upon this interface, and not implementation
 * details of any one runtime.
 *
 * Script Runtimes are memory-countable and will be used as tracing roots.
 */
public interface ScriptRuntime extends MemoryCountable {

    /**
     * Run the snippet of code which was typed into chat through "/figura run".
     * Not all engines necessarily need to support this, it's fine to just print
     * an error to chat here if the language doesn't easily support a REPL.
     */
    void runCode(String snippet) throws ScriptError;

    /**
     * Run when the Avatar is destroyed. Should clean up any native resources used
     * by the runtime.
     */
    void destroy();

    void init() throws ScriptError; // Runs once on Avatar creation
    void tick() throws ScriptError; // Runs every tick (regardless of whether user is loaded or not)
    void render(float tickDelta) throws ScriptError; // Runs every frame (regardless of whether the user is visible or not)

}
