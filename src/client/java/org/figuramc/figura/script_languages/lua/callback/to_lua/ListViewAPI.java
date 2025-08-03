package org.figuramc.figura.script_languages.lua.callback.to_lua;

import org.figuramc.figura.avatars.AvatarError;
import org.figuramc.figura.script_hooks.callback.CallbackType;
import org.figuramc.figura.script_hooks.callback.items.CallbackItem;
import org.figuramc.figura.script_hooks.callback.items.ListView;
import org.figuramc.figura.script_hooks.mem_count.AllocationTracker;
import org.figuramc.figura.script_languages.lua.FiguraMetatables;
import org.figuramc.figura.script_languages.lua.callback.from_lua.LuaListView;
import org.figuramc.figura.script_languages.lua.cobalt.org.squiddev.cobalt.*;
import org.figuramc.figura.script_languages.lua.cobalt.org.squiddev.cobalt.function.LibFunction;

public class ListViewAPI {

    public static LuaUserdata wrap(ListView<?> listView, LuaState state) {
        return new LuaUserdata(listView, state.figuraMetatables.listView);
    }

    public static LuaTable createMetatable(LuaState state) throws LuaError, AvatarError {
        LuaTable metatable = new LuaTable(state.allocationTracker);

        // ListView.new({x, y, z}, CallbackType) -> ListView
        metatable.rawset("new", LibFunction.create((s, list, elemType) -> new LuaUserdata(new LuaListView<>(s, list.checkTable(s), elemType.checkUserdata(s, CallbackType.class)), metatable)));

        // ListView:length() -> int
        LibFunction length = LibFunction.create((s, view) -> LuaInteger.valueOf(view.checkUserdata(s, ListView.class).length()));
        metatable.rawset("length", length);
        metatable.rawset(Constants.LEN, length);

        // ListView:copy() -> LuaTable
        metatable.rawset("copy", LibFunction.create((s, view) -> copyImpl(s, view.checkUserdata(s, ListView.class))));

        metatable.rawset(Constants.NAME, LuaString.valueOfNoAlloc("ListView"));

        FiguraMetatables.setupIndexing(state, metatable);
        return metatable;
    }

    // Separate into its own method to use generics properly
    private static <T extends CallbackItem> LuaTable copyImpl(LuaState state, ListView<T> view) throws AvatarError {
        int len = view.length();
        LuaTable tab = new LuaTable(len, 0, state.allocationTracker);
        for (int i = 0; i < len; i++)
            tab.rawset(i + 1, view.callbackType.fromItem(state.callbackItemToLua, view.get(i)));
        return tab;
    }

}
