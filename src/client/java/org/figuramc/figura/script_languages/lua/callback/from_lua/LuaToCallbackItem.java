package org.figuramc.figura.script_languages.lua.callback.from_lua;

import org.figuramc.figura.script_hooks.callback.CallbackType;
import org.figuramc.figura.script_hooks.callback.items.CallbackItem;
import org.figuramc.figura.script_hooks.callback.items.ListView;
import org.figuramc.figura.script_hooks.callback.items.StringView;
import org.figuramc.figura.script_hooks.mem_count.AllocationTracker;
import org.figuramc.figura.script_languages.lua.cobalt.org.squiddev.cobalt.LuaError;
import org.figuramc.figura.script_languages.lua.cobalt.org.squiddev.cobalt.LuaState;
import org.figuramc.figura.script_languages.lua.cobalt.org.squiddev.cobalt.LuaValue;

// TODO make conversion errors translatable / have more context...
public class LuaToCallbackItem implements CallbackType.ToItemVisitor<LuaValue, LuaError, AllocationTracker.AvatarOOMException> {

    private final LuaState state;

    public LuaToCallbackItem(LuaState state) {
        this.state = state;
    }

    @Override
    public CallbackItem.Unit visit(CallbackType.Unit unit, LuaValue value) {
        return CallbackItem.Unit.INSTANCE; // TODO decide, should we typecheck that the value is nil?
    }

    @Override
    public CallbackItem visit(CallbackType.Any any, LuaValue value) {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public CallbackItem.Bool visit(CallbackType.Bool bool, LuaValue value) throws LuaError, AllocationTracker.AvatarOOMException {
        return new CallbackItem.Bool(value.toBoolean()); // Use truthiness here? Or typecheck it as a bool? Not sure...
    }

    @Override
    public CallbackItem.F32 visit(CallbackType.F32 f32, LuaValue value) throws LuaError, AllocationTracker.AvatarOOMException {
        return new CallbackItem.F32((float) value.checkDouble(state));
    }

    @Override
    public CallbackItem.F64 visit(CallbackType.F64 f64, LuaValue value) throws LuaError, AllocationTracker.AvatarOOMException {
        return new CallbackItem.F64(value.checkDouble(state));
    }

    @Override
    public StringView visit(CallbackType.Str str, LuaValue value) throws LuaError, AllocationTracker.AvatarOOMException {
        return new LuaStringView(value.checkLuaString(state));
    }

    @Override
    public <T extends CallbackItem> ListView<T> visit(CallbackType.List<T> list, LuaValue value) throws LuaError, AllocationTracker.AvatarOOMException {
        return new LuaListView<>(state, value.checkTable(state), list.element());
    }
}
