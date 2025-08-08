package org.figuramc.figura.script_languages.lua.type_apis.model_parts;

import org.figuramc.figura.animation.AnimationInstance;
import org.figuramc.figura.comptime.lua.annotations.LuaExpose;
import org.figuramc.figura.comptime.lua.annotations.LuaTypeAPI;
import org.figuramc.figura.model.part.FigmodelModelPart;
import org.figuramc.figura.script_languages.lua.cobalt.org.squiddev.cobalt.LuaState;
import org.figuramc.figura.script_languages.lua.cobalt.org.squiddev.cobalt.LuaString;
import org.figuramc.figura.script_languages.lua.cobalt.org.squiddev.cobalt.LuaUserdata;
import org.jetbrains.annotations.Nullable;

@LuaTypeAPI(typeName = "Figmodel", wrappedClass = FigmodelModelPart.class, hasSuperclass = true)
public class FigmodelAPI {

    public static LuaUserdata wrap(FigmodelModelPart modelPart, LuaState state) {
        return new LuaUserdata(modelPart, state.figuraMetatables.figmodelModelPart);
    }

    @LuaExpose
    public static @Nullable AnimationInstance animation(FigmodelModelPart self, LuaString animName) {
        return self.animation(animName.toJavaStringNoAlloc());
    }

}
