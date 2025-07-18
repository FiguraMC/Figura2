package org.figuramc.figura.script_languages.lua.async;

import org.figuramc.figura.script_languages.lua.FiguraMetatables;
import org.figuramc.figura.script_languages.lua.cobalt.cc.tweaked.cobalt.internal.unwind.SuspendedAction;
import org.figuramc.figura.script_languages.lua.cobalt.org.squiddev.cobalt.*;
import org.figuramc.figura.script_languages.lua.cobalt.org.squiddev.cobalt.function.Dispatch;
import org.figuramc.figura.script_languages.lua.cobalt.org.squiddev.cobalt.function.LibFunction;
import org.figuramc.figura.script_languages.lua.cobalt.org.squiddev.cobalt.unwind.SuspendedFunction;

/**
 * Defines the key function "async()" used for running asynchronous tasks.
 * // TODO: Unsure if this functionality should be made in Lua or Java
 */
public class LuaAsync {

    // Returns the tick event loop, which should be advanced by 1 each tick!
    // Note: This doesn't work and fails instrumentation, so it's commented out
//    public static LuaEventLoop init(LuaState state, FiguraMetatables metatables) {
//
//        LuaEventLoop tickEventLoop = new LuaEventLoop();
//
//        state.globals().rawset("async", LibFunction.createS((s, di, args) -> {
//            LuaPromise promise = new LuaPromise();
//            SuspendedFunction<Void> task = SuspendedAction.toFunction(() -> {
//                promise.complete(s, Dispatch.invoke(s, args.first(), Constants.NONE));
//                return null;
//            });
//            try {
//                task.call(s);
//            } catch (UnwindThrowable unwind) {
//                tickEventLoop.handleYield(s, task, unwind);
//            }
//            return LuaPromise.wrap(promise, metatables);
//        }));
//
//        return tickEventLoop;
//    }

}
