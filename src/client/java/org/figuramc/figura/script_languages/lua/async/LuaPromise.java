package org.figuramc.figura.script_languages.lua.async;

import org.figuramc.figura.script_hooks.mem_count.MarkedObjectBase;
import org.figuramc.figura.script_hooks.mem_count.MemoryCounter;
import org.figuramc.figura.script_languages.lua.FiguraMetatables;
import org.figuramc.figura.script_languages.lua.cobalt.cc.tweaked.cobalt.internal.unwind.AutoUnwind;
import org.figuramc.figura.script_languages.lua.cobalt.cc.tweaked.cobalt.internal.unwind.SuspendedAction;
import org.figuramc.figura.script_languages.lua.cobalt.org.squiddev.cobalt.*;
import org.figuramc.figura.script_languages.lua.cobalt.org.squiddev.cobalt.function.LibFunction;
import org.figuramc.figura.script_languages.lua.cobalt.org.squiddev.cobalt.unwind.SuspendedTask;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Userdata object used for async/await in Lua
 * Works same as coroutines underneath
 *
 * // Note: This doesn't work and fails instrumentation, so it's commented out
 */
public class LuaPromise /*extends MarkedObjectBase*/ {

//    private @Nullable Varargs result = null; // Null if promise is not yet completed
//    private List<SuspendedTask<Void>> awaiters = new ArrayList<>(0); // Threads awaiting this one's completion
//
//    @AutoUnwind
//    public void complete(LuaState state, @NotNull Varargs result) throws LuaError, UnwindThrowable {
//        if (isCompleted()) throw new LuaError("Attempt to complete the same promise multiple times", state.allocationTracker);
//        this.result = result;
//        for (SuspendedTask<Void> awaiter : awaiters)
//            awaiter.resume(result);
//        this.awaiters = null;
//    }
//
//    public boolean isCompleted() {
//        return result != null;
//    }
//
//    public void thenRun(SuspendedTask<Void> task) throws LuaError, UnwindThrowable {
//        if (isCompleted()) {
//            // If we're already completed, run the task right away
//            task.resume(result);
//        } else {
//            // Otherwise, add the task to list of awaiters
//            awaiters.add(task);
//        }
//    }
//
//    public static LuaUserdata wrap(LuaPromise promise, FiguraMetatables metatables) {
//        return new LuaUserdata(promise, metatables.promise);
//    }
//
//    public static LuaTable createMetatable(LuaState state, FiguraMetatables metatables) throws LuaError {
//        LuaTable metatable = new LuaTable(state.allocationTracker);
//
//        // Promise:await() is literally just coroutine.yield(self).
//        // The actual handling for this situation is part of the definition of async().
//        metatable.rawset("await", LibFunction.createS((s, di, args) -> LuaThread.yield(s, args)));
//
//        // Create a new Promise, which can be awaited and completed manually later with :complete()
//        metatable.rawset("new", LibFunction.create(s -> wrap(new LuaPromise(), metatables)));
//
//        // Complete this promise with the given args.
//        // The args will be forwarded to tasks that await this promise.
//        metatable.rawset("complete", LibFunction.createS((s, di, args) -> {
//            // Completing the promise may resume other threads which were waiting on it, so this is a suspendable task.
//            LuaPromise promise = args.first().checkUserdata(s, LuaPromise.class);
//            return SuspendedAction.run(di, () -> { promise.complete(s, args.subargs(2)); return Constants.NONE; });
//        }));
//
//        metatable.rawset(Constants.NAME, ValueFactory.valueOf("Promise", state.allocationTracker));
//
//        FiguraMetatables.setupIndexing(state, metatable, null, null);
//        return metatable;
//    }
//
//
//    @Override
//    protected long traceNoMark(MemoryCounter counter, int depth) {
//        throw new IllegalStateException("TODO");
//    }
}
