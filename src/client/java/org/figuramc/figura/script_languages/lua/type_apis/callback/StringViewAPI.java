package org.figuramc.figura.script_languages.lua.type_apis.callback;

import org.figuramc.figura.avatars.AvatarError;
import org.figuramc.figura.comptime.lua.annotations.LuaExpose;
import org.figuramc.figura.comptime.lua.annotations.LuaPassState;
import org.figuramc.figura.comptime.lua.annotations.LuaTypeAPI;
import org.figuramc.figura.script_hooks.callback.items.StringView;
import org.figuramc.figura.script_languages.lua.callback_types.LuaStringView;
import org.figuramc.figura.script_languages.lua.cobalt.org.squiddev.cobalt.LuaState;
import org.figuramc.figura.script_languages.lua.cobalt.org.squiddev.cobalt.LuaString;
import org.figuramc.figura.script_languages.lua.cobalt.org.squiddev.cobalt.LuaUserdata;

@LuaTypeAPI(typeName = "StringView", wrappedClass = StringView.class)
public class StringViewAPI {

    public static LuaUserdata wrap(StringView stringView, LuaState state) {
        return new LuaUserdata(stringView, state.figuraMetatables.stringView);
    }

    @LuaExpose(name = "new")
    public static StringView _new(LuaString luaString) { return new LuaStringView(luaString); }

    @LuaExpose public static void revoke(StringView self) { self.revoke(); }
    @LuaExpose public static boolean isRevoked(StringView self) { return self.isRevoked(); }
    @LuaExpose public static int length(StringView self) { return self.length(); }

    @LuaExpose @LuaPassState
    public static LuaString copy(LuaState s, StringView self) throws AvatarError {
        return LuaString.valueOf(s.allocationTracker, self.copy());
    }


}
