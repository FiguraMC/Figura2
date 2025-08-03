package org.figuramc.figura.script_languages.lua.animations;

import org.figuramc.figura.animation.AnimationInstance;
import org.figuramc.figura.avatars.AvatarError;
import org.figuramc.figura.script_hooks.mem_count.AllocationTracker;
import org.figuramc.figura.script_languages.lua.FiguraMetatables;
import org.figuramc.figura.script_languages.lua.cobalt.org.squiddev.cobalt.*;
import org.figuramc.figura.script_languages.lua.cobalt.org.squiddev.cobalt.function.LibFunction;

import static org.figuramc.figura.script_languages.lua.cobalt.org.squiddev.cobalt.ValueFactory.tableOf;

public class AnimationInstanceAPI {

    public static LuaUserdata wrap(AnimationInstance instance, LuaState state) {
        return new LuaUserdata(instance, state.figuraMetatables.animationInstance);
    }

    public static LuaTable createMetatable(LuaState state) throws LuaError, AvatarError {
        AllocationTracker t = state.allocationTracker;
        LuaTable metatable = tableOf(t);

        metatable.rawset("time", LibFunction.createV((s, args) -> switch (args.count()) {
            case 1 -> LuaDouble.valueOf(args.first().checkUserdata(s, AnimationInstance.class).getTime());
            case 2 -> {
                AnimationInstance instance = args.first().checkUserdata(s, AnimationInstance.class);
                float time = (float) args.arg(2).checkNumber(s).toDouble();
                instance.setTime(time);
                yield args.first();
            }
            default -> throw new LuaError("Invalid number of args to AnimationInstance:time(): expected 0 or 1", s.allocationTracker);
        }));

        metatable.rawset("strength", LibFunction.createV((s, args) -> switch (args.count()) {
            case 1 -> LuaDouble.valueOf(args.first().checkUserdata(s, AnimationInstance.class).getStrength());
            case 2 -> {
                AnimationInstance instance = args.first().checkUserdata(s, AnimationInstance.class);
                float strength = (float) args.arg(2).checkNumber(s).toDouble();
                instance.setStrength(strength);
                yield args.first();
            }
            default -> throw new LuaError("Invalid number of args to AnimationInstance:strength(): expected 0 or 1", s.allocationTracker);
        }));

        metatable.rawset(Constants.NAME, LuaString.valueOfNoAlloc("AnimationInstance"));

        FiguraMetatables.setupIndexing(state, metatable);
        return metatable;
    }

}
