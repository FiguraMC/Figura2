package org.figuramc.figura.script_languages.lua.callback.to_lua;

import org.figuramc.figura.script_hooks.callback.CallbackType;
import org.figuramc.figura.script_hooks.callback.items.CallbackItem;
import org.figuramc.figura.script_hooks.callback.items.ListView;
import org.figuramc.figura.script_hooks.callback.items.StringView;
import org.figuramc.figura.script_languages.lua.cobalt.org.squiddev.cobalt.*;

public class CallbackItemToLua implements CallbackType.FromItemVisitor<LuaValue> {

    private final LuaState state;

    public CallbackItemToLua(LuaState state) {
        this.state = state;
    }

    @Override
    public LuaValue visit(CallbackType.Unit unit, CallbackItem.Unit item) {
        return Constants.NIL;
    }

    @Override
    public LuaValue visit(CallbackType.Any any, CallbackItem item) {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public LuaValue visit(CallbackType.Bool bool, CallbackItem.Bool item) {
        return LuaBoolean.valueOf(item.value());
    }

    @Override
    public LuaValue visit(CallbackType.F32 f32, CallbackItem.F32 item) {
        return LuaDouble.valueOf(item.value());
    }

    @Override
    public LuaValue visit(CallbackType.F64 f64, CallbackItem.F64 item) {
        return LuaDouble.valueOf(item.value());
    }

    @Override
    public LuaValue visit(CallbackType.Str str, StringView item) {
        return StringViewAPI.wrap(item, state);
    }

    @Override
    public <T extends CallbackItem> LuaValue visit(CallbackType.List<T> list, ListView<T> item) {
        return ListViewAPI.wrap(item, state);
    }
}
