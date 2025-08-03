package org.figuramc.figura.script_languages.lua.model_parts;

import org.figuramc.figura.animation.AnimationInstance;
import org.figuramc.figura.avatars.AvatarError;
import org.figuramc.figura.model.part.FigmodelModelPart;
import org.figuramc.figura.script_languages.lua.FiguraMetatables;
import org.figuramc.figura.script_languages.lua.animations.AnimationInstanceAPI;
import org.figuramc.figura.script_languages.lua.cobalt.org.squiddev.cobalt.*;
import org.figuramc.figura.script_languages.lua.cobalt.org.squiddev.cobalt.function.LibFunction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class FigmodelAPI {

    public static LuaTable createMetatable(LuaState state, @NotNull LuaTable modelPart) throws LuaError, AvatarError {
        LuaTable metatable = new LuaTable(state.allocationTracker);

        // Fetch an AnimationInstance from the model with the given name
        metatable.rawset("animation", LibFunction.create((s, p, n) -> {
            FigmodelModelPart figmodel = p.checkUserdata(s, FigmodelModelPart.class);
            @Nullable AnimationInstance anim =  figmodel.animation(n.checkString(s));
            if (anim == null) return Constants.NIL;
            return AnimationInstanceAPI.wrap(anim, s);
        }));

        metatable.rawset(Constants.NAME, LuaString.valueOfNoAlloc("Figmodel"));

        FiguraMetatables.setupIndexingWithSuperclass(state, metatable, modelPart);
        return metatable;
    }
}
