package org.figuramc.figura.script_languages.lua;

import net.minecraft.network.chat.Component;
import org.figuramc.figura.model.part.FiguraModelPart;
import org.figuramc.figura.script_hooks.ScriptError;
import org.figuramc.figura.script_hooks.callback.CallbackType;
import org.figuramc.figura.script_hooks.callback.ScriptCallback;
import org.figuramc.figura.script_hooks.mem_count.MarkedObjectBase;
import org.figuramc.figura.script_hooks.mem_count.MemoryCounter;
import org.figuramc.figura.script_languages.lua.cobalt.org.squiddev.cobalt.*;
import org.figuramc.figura.script_languages.lua.cobalt.org.squiddev.cobalt.function.Dispatch;
import org.figuramc.figura.script_languages.lua.cobalt.org.squiddev.cobalt.function.LuaFunction;
import org.figuramc.figura.script_languages.lua.model_parts.ModelPartAPI;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Implementation of ScriptCallback for Lua
// TODO improve error messages for wrong arguments, can even make them translatable?
public class LuaCallback extends MarkedObjectBase implements ScriptCallback {

    private final CallbackType.Func type;
    public final LuaState state;
    private final FiguraMetatables metatables;
    private final LuaValue wrapped;

    public LuaCallback(CallbackType.Func type, LuaState state, FiguraMetatables metatables, LuaValue wrapped) {
        this.type = type;
        this.state = state;
        this.metatables = metatables;
        this.wrapped = wrapped;
    }

    @Override
    public CallbackType.Func type() {
        return type;
    }

    // TODO look into caching the result of conversion when multiple callbacks are run with the same args (for example, events)
    @Override
    public Object call(Object... args) throws ScriptError {
        // Convert args into Lua
        LuaValue[] argsInLua = new LuaValue[args.length];
        for (int i = 0; i < args.length; i++)
            argsInLua[i] = toLua(state, metatables, args[i], type.paramTypes()[i]);
        // Run the function, passing converted args
        LuaValue result;
        try {
            result = LuaThread.run(new LuaThread(state, wrapped), ValueFactory.varargsOf(argsInLua)).first();
            // TODO check if thread is still alive, if so error with "cannot yield here" message
        } catch (LuaError luaError) {
            throw new ScriptError(Component.literal(luaError.getMessage().replace("\t", "  ")));
        }
        // Convert result back to generic
        return fromLua(state, metatables, result, type.returnType());
    }

    // Do NOT invoke this across different LuaState! Ensure it's the same first.
    // This is here for the fast track of invoking callbacks from within the same LuaState,
    // meaning no conversions are needed.
    public LuaValue localCall(Varargs args) throws LuaError {
        try {
            return Dispatch.invoke(state, wrapped, args).first();
        } catch (UnwindThrowable yielded) {
            throw new LuaError("Cannot yield() from within Figura callback!", state.allocationTracker);
        }
    }

    @Override
    protected long traceNoMark(MemoryCounter counter, int depth) {
        // TODO trace type
        counter.trace(state, depth);
        counter.trace(metatables, depth);
        counter.trace(wrapped, depth);
        return 32;
    }

    // Convert generic Object -> Lua value in the given state, with the given metatables
    public static LuaValue toLua(LuaState state, FiguraMetatables metatables, @Nullable Object object, CallbackType type) throws ScriptError {
        return switch (type) {
            case CallbackType.Unit __ -> { if (object == null) yield Constants.NIL; throw new ScriptError(Component.literal("Expected null"));  }

            case CallbackType.Bool __ -> { if (object instanceof Boolean b) yield ValueFactory.valueOf(b); throw new ScriptError(Component.literal("Expected bool")); }
            case CallbackType.F32 __ -> { if (object instanceof Float f) yield ValueFactory.valueOf((double) f); throw new ScriptError(Component.literal("Expected float")); }
            case CallbackType.F64 __ -> { if (object instanceof Double d) yield ValueFactory.valueOf(d); throw new ScriptError(Component.literal("Expected double")); }
            case CallbackType.Str __ -> { if (object instanceof String s) yield ValueFactory.valueOf(s, state.allocationTracker); throw new ScriptError(Component.literal("Expected string")); }

            case CallbackType.FiguraPart __ -> { if (object instanceof FiguraModelPart p) yield ModelPartAPI.wrap(p, metatables); throw new ScriptError(Component.literal("Expected Figura modelpart")); }

            case CallbackType.List(CallbackType inner) -> {
                if (object instanceof ArrayList<?> list) {
                    LuaTable res = new LuaTable(list.size(), 0, state.allocationTracker);
                    for (int i = 0; i < list.size(); i++)
                        res.rawset(i + 1, toLua(state, metatables, list.get(i), inner));
                    yield res;
                }
                throw new ScriptError(Component.literal("Expected list"));
            }
            case CallbackType.Map(CallbackType key, CallbackType value) -> {
                if (object instanceof HashMap<?, ?> map) {
                    try {
                        LuaTable res = new LuaTable(0, map.size(), state.allocationTracker);
                        for (var entry : map.entrySet())
                            res.rawset(toLua(state, metatables, entry.getKey(), key), toLua(state, metatables, entry.getValue(), value));
                        yield res;
                    } catch (LuaError e) {
                        throw new ScriptError(Component.literal("Failed to convert map to Lua"));
                    }
                }
                throw new ScriptError(Component.literal("Expected map"));
            }
            case CallbackType.Tuple(CallbackType[] types) -> {
                if (object instanceof Object[] arr) {
                    LuaTable res = new LuaTable(arr.length, 0, state.allocationTracker);
                    for (int i = 0; i < arr.length; i++)
                        res.rawset(i + 1, toLua(state, metatables, arr[i], types[i]));
                    yield res;
                }
                throw new ScriptError(Component.literal("Expected tuple"));
            }
            case CallbackType.Func funcType -> {
                if (object instanceof ScriptCallback callback) {
                    // TODO check if funcType matches callback's type?
                    yield CallbackAPI.wrap(callback, metatables);
                }
                throw new ScriptError(Component.literal("Expected callback"));
            }
            case CallbackType.Nullable(CallbackType inner) -> {
                if (object == null) yield Constants.NIL;
                yield toLua(state, metatables, object, inner);
            }

            // "Any" wildcard
            case CallbackType.Any any -> switch (object) {
                case null -> Constants.NIL;
                case Boolean b -> ValueFactory.valueOf(b);
                case Number n -> ValueFactory.valueOf(n.doubleValue());
                case String s -> ValueFactory.valueOf(s, state.allocationTracker);
                case List<?> list -> {
                    LuaTable res = new LuaTable(list.size(), 0, state.allocationTracker);
                    for (int i = 0; i < list.size(); i++)
                        res.rawset(i + 1, toLua(state, metatables, list.get(i), any));
                    yield res;
                }
                case Map<?, ?> map -> {
                    try {
                        LuaTable res = new LuaTable(0, map.size(), state.allocationTracker);
                        for (var entry : map.entrySet())
                            res.rawset(toLua(state, metatables, entry.getKey(), any), toLua(state, metatables, entry.getValue(), any));
                        yield res;
                    } catch (LuaError e) {
                        throw new ScriptError(Component.literal("Failed to convert map to Lua"));
                    }
                }
                case Object[] arr -> {
                    LuaTable res = new LuaTable(arr.length, 0, state.allocationTracker);
                    for (int i = 0; i < arr.length; i++)
                        res.rawset(i + 1, toLua(state, metatables, arr[i], any));
                    yield res;
                }
                case ScriptCallback callback -> CallbackAPI.wrap(callback, metatables);
                case FiguraModelPart p -> ModelPartAPI.wrap(p, metatables);
                default -> throw new IllegalStateException("Unexpected type: " + object.getClass());
            };

        };
    }

    // Convert Lua value -> generic Object
    public static @Nullable Object fromLua(LuaState state, FiguraMetatables metatables, LuaValue value, CallbackType type) throws ScriptError {
        return switch (type) {
            case CallbackType.Unit __ -> { if (value.isNil()) yield null; throw new ScriptError(Component.literal("Expected nil")); }

            // For bools, use Lua's truthy/falsy rules
            case CallbackType.Bool __ -> value.toBoolean();
            case CallbackType.F64 __ -> { if (value instanceof LuaNumber n) yield n.toDouble(); throw new ScriptError(Component.literal("Expected number")); }
            case CallbackType.F32 __ -> { if (value instanceof LuaNumber n) yield (float) n.toDouble(); throw new ScriptError(Component.literal("Expected number")); }
            case CallbackType.Str __ -> { if (value instanceof LuaString s) yield s.toString(); throw new ScriptError(Component.literal("Expected string")); }

            case CallbackType.FiguraPart __ -> { if (value instanceof LuaUserdata u && u.instance instanceof FiguraModelPart part) yield part; throw new ScriptError(Component.literal("Expected Figura modelpart")); }

            case CallbackType.List(CallbackType inner) -> {
                // Can unfortunately have weird things with lists of nullable values, since Lua thinks the list ends when it sees a nil... TODO look into this, see if there's a better way
                if (value instanceof LuaTable table) {
                    ArrayList<Object> res = new ArrayList<>();
                    LuaValue v;
                    for (int i = 1; !(v = table.rawget(i)).isNil(); i++)
                        res.add(fromLua(state, metatables, v, inner));
                    yield res;
                }
                throw new ScriptError(Component.literal("Expected list table"));
            }
            case CallbackType.Map(CallbackType keyType, CallbackType valType) -> {
                if (value instanceof LuaTable table) {
                    HashMap<Object, Object> res = new HashMap<>();
                    try {
                        table.forEach((k, v) -> res.put(fromLua(state, metatables, k, keyType), fromLua(state, metatables, v, valType)));
                    } catch (LuaError luaError) {
                        // TODO figure out this disgusting try-catch and see if we can avoid it
                        throw new ScriptError(Component.literal(luaError.getMessage().replace("\t", "  ")));
                    }
                    yield res;
                }
                throw new ScriptError(Component.literal("Expected map table"));
            }
            case CallbackType.Tuple(CallbackType[] types) -> {
                if (value instanceof LuaTable table) {
                    Object[] res = new Object[types.length];
                    for (int i = 0; i < types.length; i++)
                        res[i] = fromLua(state, metatables, table.rawget(i + 1), types[i]);
                    yield res;
                }
                throw new ScriptError(Component.literal("Expected tuple table"));
            }
            case CallbackType.Func funcType -> {
                if (value instanceof LuaUserdata u && u.instance instanceof ScriptCallback callback) {
                    yield callback; // TODO type-check it here?
                } else if (value instanceof LuaFunction || !value.metatag(state, Constants.CALL).isNil()) {
                    yield new LuaCallback(funcType, state, metatables, value);
                }
                throw new ScriptError(Component.literal("Expected callable"));
            }
            case CallbackType.Nullable(CallbackType inner) -> {
                if (value.isNil()) yield null;
                yield fromLua(state, metatables, value, inner);
            }

            // "Any" wildcard. This is not recommended for APIs unless absolutely necessary.
            // - By default, numbers will be treated as F64
            // - Tables will try to be List<Any> if there are only numeric indices.
            // - Otherwise, they become Map<Any, Any>.
            case CallbackType.Any any -> switch (value) {
                case LuaNil __ -> null;
                case LuaBoolean bool -> bool.value;
                case LuaNumber num -> num.toDouble();
                case LuaString s -> s.toString();

                case LuaTable tab -> {
                    int len = tab.length();
                    int size = tab.size();
                    if (len == size) {
                        ArrayList<Object> result = new ArrayList<>(len);
                        for (int i = 1; i <= len; i++)
                            result.add(fromLua(state, metatables, tab.rawget(i), any));
                        yield result;
                    } else {
                        try {
                            HashMap<Object, Object> result = new HashMap<>();
                            tab.forEach((k, v) -> result.put(fromLua(state, metatables, k, any), fromLua(state, metatables, v, any)));
                            yield result;
                        } catch (LuaError luaError) {
                            // TODO figure out this disgusting try-catch and see if we can avoid it
                            throw new ScriptError(Component.literal(luaError.getMessage().replace("\t", "  ")));
                        }
                    }
                }

                case LuaUserdata userdata -> switch (userdata.instance) {
                    case null -> null;
                    case FiguraModelPart p -> p;

                    default -> throw new IllegalStateException("Unexpected Lua userdata type: " + userdata.instance.getClass());
                };

                default -> throw new IllegalStateException("Unexpected Lua type: " + value.getClass());
            };
        };
    }

}