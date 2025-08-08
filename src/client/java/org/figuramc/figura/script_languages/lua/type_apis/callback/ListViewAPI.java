package org.figuramc.figura.script_languages.lua.type_apis.callback;

import org.figuramc.figura.avatars.AvatarError;
import org.figuramc.figura.comptime.lua.annotations.LuaExpose;
import org.figuramc.figura.comptime.lua.annotations.LuaPassState;
import org.figuramc.figura.comptime.lua.annotations.LuaTypeAPI;
import org.figuramc.figura.script_hooks.callback.CallbackType;
import org.figuramc.figura.script_hooks.callback.items.CallbackItem;
import org.figuramc.figura.script_hooks.callback.items.ListView;
import org.figuramc.figura.script_languages.lua.callback_types.LuaListView;
import org.figuramc.figura.script_languages.lua.cobalt.org.squiddev.cobalt.LuaState;
import org.figuramc.figura.script_languages.lua.cobalt.org.squiddev.cobalt.LuaTable;
import org.figuramc.figura.script_languages.lua.cobalt.org.squiddev.cobalt.LuaUserdata;

@LuaTypeAPI(typeName = "ListView", wrappedClass = ListView.class)
public class ListViewAPI {

    public static LuaUserdata wrap(ListView<?> listView, LuaState state) {
        return new LuaUserdata(listView, state.figuraMetatables.listView);
    }

    @LuaExpose(name = "new") @LuaPassState
    public static ListView<?> _new(LuaState s, LuaTable list, CallbackType<?> elemType) {
        return new LuaListView<>(s, list, elemType);
    }

    @LuaExpose public static void revoke(ListView<?> self) { self.revoke(); }
    @LuaExpose public static boolean isRevoked(ListView<?> self) { return self.isRevoked(); }
    @LuaExpose public static int length(ListView<?> self) { return self.length(); }

    @LuaExpose @LuaPassState
    public static LuaTable copy(LuaState s, ListView<?> self) throws AvatarError {
        return copyImpl(s, self);
    }

    private static <T extends CallbackItem> LuaTable copyImpl(LuaState state, ListView<T> view) throws AvatarError {
        int len = view.length();
        LuaTable tab = new LuaTable(len, 0, state.allocationTracker);
        for (int i = 0; i < len; i++)
            tab.rawset(i + 1, view.callbackType.fromItem(state.callbackItemToLua, view.get(i)));
        return tab;
    }

}
