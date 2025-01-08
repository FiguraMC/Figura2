package org.figuramc.figura.script_languages.lua;

import org.figuramc.figura.FiguraModClient;
import org.figuramc.figura.manage.AvatarLoadingException;
import org.figuramc.figura.script_languages.lua.cobalt.org.squiddev.cobalt.*;
import org.figuramc.figura.script_languages.lua.cobalt.org.squiddev.cobalt.compiler.CompileException;
import org.figuramc.figura.script_languages.lua.cobalt.org.squiddev.cobalt.compiler.LoadState;
import org.figuramc.figura.script_languages.lua.cobalt.org.squiddev.cobalt.function.LuaClosure;

import java.io.IOException;
import java.io.InputStream;

/**
 * Events logic in another class for safe keeping.
 * Uses 'events.lua' resource to define most logic in Lua directly.
 */
public class FiguraEvents {

    /**
     * Runs the "events" script in the given state, and create several default events.
     * These default events are then returned in the table.
     * Make sure to run this before any user scripts are added.
     */
    public static LuaTable init(LuaState state, String... defaultEvents) throws AvatarLoadingException {
        try(InputStream input = FiguraModClient.class.getResourceAsStream("/assets/figura/scripts/lua/events.lua")) {
            // Compile the file
            if (input == null) throw new AvatarLoadingException("Figura was unable to find \"events.lua\"? Likely bug in Figura, please report.");
            LuaClosure c = LoadState.load(state, input, "=EVENTS", state.globals());
            // Tell the file what events we want
            LuaString[] eventNames = new LuaString[defaultEvents.length];
            for (int i = 0; i < defaultEvents.length; i++)
                eventNames[i] = LuaString.valueOf(state.allocationTracker, defaultEvents[i]);
            Varargs args = ValueFactory.varargsOf(eventNames);
            // Execute the file
            return LuaThread.runMain(state, c, args).first().checkTable(state);
        } catch (IOException ex) {
            throw new AvatarLoadingException("Figura was unable to load \"events.lua\"? Likely bug in Figura, please report.", ex);
        } catch (CompileException shouldNotHappen) {
            throw new AvatarLoadingException("Figura internal \"events.lua\" failed to compile! Likely bug in Figura, please report.", shouldNotHappen);
        } catch (LuaError e) {
            throw new AvatarLoadingException("Error while initializing Figura internal \"events.lua\". Likely bug in Figura, please report.", e);
        }
    }


}
