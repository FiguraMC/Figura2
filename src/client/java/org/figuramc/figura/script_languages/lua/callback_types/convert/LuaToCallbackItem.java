package org.figuramc.figura.script_languages.lua.callback_types.convert;

import org.figuramc.figura.avatars.AvatarError;
import org.figuramc.figura.script_hooks.callback.CallbackType;
import org.figuramc.figura.script_hooks.callback.items.CallbackItem;
import org.figuramc.figura.script_hooks.callback.items.EntityView;
import org.figuramc.figura.script_hooks.callback.items.ListView;
import org.figuramc.figura.script_hooks.callback.items.StringView;
import org.figuramc.figura.script_languages.lua.callback_types.LuaListView;
import org.figuramc.figura.script_languages.lua.callback_types.LuaStringView;
import org.figuramc.figura.script_languages.lua.cobalt.org.squiddev.cobalt.LuaError;
import org.figuramc.figura.script_languages.lua.cobalt.org.squiddev.cobalt.LuaState;
import org.figuramc.figura.script_languages.lua.cobalt.org.squiddev.cobalt.LuaUserdata;
import org.figuramc.figura.script_languages.lua.cobalt.org.squiddev.cobalt.LuaValue;

// TODO make conversion errors translatable / have more context...
public class LuaToCallbackItem implements CallbackType.ToItemVisitor<LuaValue, LuaError, AvatarError> {

    private final LuaState state;

    public LuaToCallbackItem(LuaState state) {
        this.state = state;
    }

    @Override
    public CallbackItem.Unit visit(CallbackType.Unit __, LuaValue value) {
        return CallbackItem.Unit.INSTANCE; // TODO decide, should we typecheck that the value is nil?
    }

    @Override
    public CallbackItem visit(CallbackType.Any __, LuaValue value) {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public CallbackItem.Bool visit(CallbackType.Bool __, LuaValue value) throws LuaError, AvatarError {
        return new CallbackItem.Bool(value.toBoolean()); // Use truthiness here? Or typecheck it as a bool? Not sure...
    }

    @Override
    public CallbackItem.F32 visit(CallbackType.F32 __, LuaValue value) throws LuaError, AvatarError {
        return new CallbackItem.F32((float) value.checkDouble(state));
    }

    @Override
    public CallbackItem.F64 visit(CallbackType.F64 __, LuaValue value) throws LuaError, AvatarError {
        return new CallbackItem.F64(value.checkDouble(state));
    }

    @Override
    public StringView visit(CallbackType.Str __, LuaValue value) throws LuaError, AvatarError {
        if (value instanceof LuaUserdata userdata && userdata.instance instanceof StringView stringView) return stringView;
        return new LuaStringView(value.checkLuaString(state));
    }

    @Override
    public EntityView<?> visit(CallbackType.Entity __, LuaValue value) throws LuaError, AvatarError {
        return value.checkUserdata(state, EntityView.class);
    }

    @Override
    public <T extends CallbackItem> ListView<T> visit(CallbackType.List<T> list, LuaValue value) throws LuaError, AvatarError {
        // TODO figure out list passthrough (what if it's a different type of list? needs typecheck)
        return new LuaListView<>(state, value.checkTable(state), list.element());
    }
}
