package org.figuramc.figura.script_hooks;

/**
 * An interface to be implemented by any Script Engine to be added.
 * The mod itself will only depend upon this interface, and not implementation
 * details of any one engine.
 */
public interface ScriptEngine {

    void init() throws ScriptError; // Runs once on Avatar creation
    void tick() throws ScriptError; // Runs every tick
    void render(float tickDelta) throws ScriptError; // Runs every frame (regardless of whether the user is visible or not!)

}
