package org.figuramc.figura.script_hooks;

/**
 * An interface to be implemented by any language to be added.
 * The mod itself will only depend upon this interface, and not implementation
 * details of any one runtime.
 */
public interface ScriptRuntime {

    /**
     * Add the given user script into the runtime. The script has a name and associated data.
     * In the case of string-based languages, the data will probably be UTF-8 encoded text(? todo explain more).
     */
    void addScript(String name, byte[] data);

    /**
     * Run the snippet of code which was typed into chat through "/figura run".
     * Not all engines necessarily need to support this, it's fine to just print
     * an error to chat here if the language doesn't easily support a REPL.
     */
    void runCode(String snippet) throws ScriptError;

    void init() throws ScriptError; // Runs once on Avatar creation
    void tick() throws ScriptError; // Runs every tick (regardless of whether user is loaded or not)
    void render(float tickDelta) throws ScriptError; // Runs every frame (regardless of whether the user is visible or not)

}
