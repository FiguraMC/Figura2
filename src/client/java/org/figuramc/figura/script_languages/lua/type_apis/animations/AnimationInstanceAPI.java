package org.figuramc.figura.script_languages.lua.type_apis.animations;

import org.figuramc.figura.animation.AnimationInstance;
import org.figuramc.figura.comptime.lua.annotations.LuaExpose;
import org.figuramc.figura.comptime.lua.annotations.LuaReturnSelf;
import org.figuramc.figura.comptime.lua.annotations.LuaTypeAPI;
import org.figuramc.figura.script_languages.lua.cobalt.org.squiddev.cobalt.LuaState;
import org.figuramc.figura.script_languages.lua.cobalt.org.squiddev.cobalt.LuaUserdata;

@LuaTypeAPI(typeName = "AnimationInstance", wrappedClass = AnimationInstance.class)
public class AnimationInstanceAPI {

    public static LuaUserdata wrap(AnimationInstance instance, LuaState state) {
        return new LuaUserdata(instance, state.figuraMetatables.animationInstance);
    }

    @LuaExpose public static double time(AnimationInstance instance) { return instance.getTime(); }
    @LuaExpose @LuaReturnSelf public static void time(AnimationInstance instance, float time) { instance.setTime(time); }

    @LuaExpose public static double strength(AnimationInstance instance) { return instance.getStrength(); }
    @LuaExpose @LuaReturnSelf public static void strength(AnimationInstance instance, float strength) { instance.setStrength(strength); }

}
