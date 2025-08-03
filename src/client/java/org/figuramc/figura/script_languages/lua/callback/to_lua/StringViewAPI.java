package org.figuramc.figura.script_languages.lua.callback.to_lua;

import org.figuramc.figura.avatars.AvatarError;
import org.figuramc.figura.script_hooks.callback.items.StringView;
import org.figuramc.figura.script_languages.lua.FiguraMetatables;
import org.figuramc.figura.script_languages.lua.callback.from_lua.LuaStringView;
import org.figuramc.figura.script_languages.lua.cobalt.org.squiddev.cobalt.*;
import org.figuramc.figura.script_languages.lua.cobalt.org.squiddev.cobalt.function.LibFunction;

public class StringViewAPI {

    public static LuaUserdata wrap(StringView stringView, LuaState state) {
        return new LuaUserdata(stringView, state.figuraMetatables.stringView);
    }

    public static LuaTable createMetatable(LuaState state) throws LuaError, AvatarError {
        LuaTable metatable = new LuaTable(state.allocationTracker);

        // StringView.new("some string") -> StringView
        metatable.rawset("new", LibFunction.create((s, string) -> new LuaUserdata(new LuaStringView(string.checkLuaString(s)), metatable)));

        // StringView:revoke()
        metatable.rawset("revoke", LibFunction.create((s, view) -> {
            view.checkUserdata(s, StringView.class).revoke();
            return Constants.NIL;
        }));

        // StringView:length() -> int
        LibFunction length = LibFunction.create((s, view) -> LuaInteger.valueOf(view.checkUserdata(s, StringView.class).length()));
        metatable.rawset("length", length);
        metatable.rawset(Constants.LEN, length);

        // StringView:copy() -> LuaString
        metatable.rawset("copy", LibFunction.create((s, view) -> LuaString.valueOf(s.allocationTracker, view.checkUserdata(s, StringView.class).copy())));

        metatable.rawset(Constants.NAME, LuaString.valueOfNoAlloc("StringView"));

        FiguraMetatables.setupIndexing(state, metatable);
        return metatable;
    }

}
