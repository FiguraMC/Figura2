package org.figuramc.figura.script_languages.lua.type_apis.model_parts;

import org.figuramc.figura.avatars.AvatarError;
import org.figuramc.figura.comptime.lua.annotations.LuaExpose;
import org.figuramc.figura.comptime.lua.annotations.LuaPassState;
import org.figuramc.figura.comptime.lua.annotations.LuaTypeAPI;
import org.figuramc.figura.model.part.FigmodelModelPart;
import org.figuramc.figura.model.part.FiguraModelPart;
import org.figuramc.figura.script_languages.lua.cobalt.org.squiddev.cobalt.*;
import org.jetbrains.annotations.Nullable;

@LuaTypeAPI(typeName = "FiguraPart", wrappedClass = FiguraModelPart.class, hasSuperclass = true)
public class FiguraPartAPI {

    public static LuaUserdata wrap(FiguraModelPart part, LuaState state) {
        return switch (part) {
            case FigmodelModelPart figmodel -> FigmodelAPI.wrap(figmodel, state);
            default -> new LuaUserdata(part, state.figuraMetatables.modelPart);
        };
    }

    // Get child by name
    @LuaExpose
    public static @Nullable FiguraModelPart child(FiguraModelPart self, LuaString childName) {
        return self.getChildByName(childName.toJavaStringNoAlloc());
    }

    // Get a table of all children, indexed by name
    @LuaExpose @LuaPassState
    public static LuaTable children(LuaState s, FiguraModelPart self) throws LuaError, AvatarError {
        LuaTable result = new LuaTable(s.allocationTracker);
        for (var childEntry : self.children.entrySet())
            result.rawset(childEntry.getKey(), wrap(childEntry.getValue(), s));
        return result;
    }

    // Custom __index which fetches a child by name.
    @LuaExpose
    public static @Nullable FiguraModelPart __index(FiguraModelPart self, LuaString key) {
        return child(self, key);
    }

}
