package org.figuramc.figura.script_languages.lua.callback_types;

import org.figuramc.figura.avatars.Avatar;
import org.figuramc.figura.avatars.AvatarError;
import org.figuramc.figura.script_hooks.callback.CallbackType;
import org.figuramc.figura.script_hooks.callback.ScriptCallback;
import org.figuramc.figura.script_hooks.callback.items.CallbackItem;
import org.figuramc.figura.script_languages.lua.cobalt.org.squiddev.cobalt.*;
import org.figuramc.figura.script_languages.lua.cobalt.org.squiddev.cobalt.function.Dispatch;

import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Stream;

// Implementation of ScriptCallback for Lua
// This represents a callback CREATED BY LUA and sent elsewhere.
public class LuaCallback<I extends CallbackItem, O extends CallbackItem> implements ScriptCallback<I, O> {

    private final CallbackType.Func<I, O> type;
    public final LuaState state; // The state that created this callback
    private final LuaValue wrapped; // The object which will be called as the callback, generally a LuaFunction but anything with __call works

    public LuaCallback(CallbackType.Func<I, O> type, LuaState state, LuaValue wrapped) {
        this.type = type;
        this.state = state;
        this.wrapped = wrapped;
    }

    @Override
    public CallbackType.Func<I, O> type() {
        return type;
    }

    @Override
    public Avatar<?> getOwningAvatar() {
        return state.avatar;
    }

    // TODO look into caching the result of conversion when multiple callbacks are run with the same args (for example, events)
    // Remember that the things running here are the fault of the CALLEE.
    // So if an error arises, we don't throw it back to the CALLER, we fault the Avatar who created this incorrect callback.
    @Override
    public O call(I arg) {
        Objects.requireNonNull(state.avatar, "Attempt to call LuaCallback before its avatar was set! Internal Figura bug, please report");
        // If we're errored, don't call the function
        if (!state.avatar.isErrored()) {
            // Convert the arg into Lua Varargs to pass to our function.
            // We'll treat funcs with tuple/unit args specially, and make it simple to call it from Lua with just multiple args, not needing to wrap in a table.
            Varargs luaArgs = type.param() instanceof CallbackType.Tuple<I> tuple ? ValueFactory.varargsOf(tuple.fromItems(state.callbackItemToLua, arg, LuaValue[]::new)) : type.param().fromItem(state.callbackItemToLua, arg);

            // Run the function, getting a result:
            try {
                Varargs luaResult = LuaThread.run(new LuaThread(state, wrapped), luaArgs);
                try {
                    // Attempt to convert the result back to an O, and return.
                    return type.returnType() instanceof CallbackType.Tuple<O> tuple ? tuple.toItems(state.luaToCallbackItem, luaResult.toArray()) : type.returnType().toItem(state.luaToCallbackItem, luaResult.first());
                } catch (LuaError luaError) {
                    // Lua returned an incorrect type from the callback? Let's error with an appropriate message.
                    String expectedType = type.returnType().stringify();
                    String actualType = (luaResult.count() == 1 ? Stream.of(luaResult.first()) : Arrays.stream(luaResult.toArray())).map(LuaValue::typeName).toList().toString(); // (WIP. Could be better...)
                    state.avatar.error(new AvatarError("figura.error.script.lua.incorrect_callback_return", expectedType, actualType));
                }
            } catch (LuaError luaError) {
                // The callback encountered an error while running.
                state.avatar.error(new AvatarError("figura.error.script.lua.error_in_callback", luaError, luaError.getMessage()));
            } catch (AvatarError avatarError) {
                // The callback's owner ran out of memory
                state.avatar.error(avatarError);
            }
        }
        // TODO: Figure out what to return to the caller if the callee failed. A default return value?
        throw new UnsupportedOperationException("TODO");
    }

    // Do NOT invoke this across different LuaState! Ensure it's the same first.
    // This is here for the fast track of invoking callbacks from within the same LuaState,
    // meaning no conversions are needed.
    public LuaValue localCall(Varargs args) throws LuaError, AvatarError {
        try {
            // TODO optionally type-check the args/return type for improved error messages, even if there's no conversions happening
            return Dispatch.invoke(state, wrapped, args).first();
        } catch (UnwindThrowable yielded) {
            throw new LuaError("Cannot yield() from within Figura callback!", state.allocationTracker);
        }
    }

}