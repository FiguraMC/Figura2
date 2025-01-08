package org.figuramc.figura.script_languages.lua.math;

import org.figuramc.figura.manage.AvatarLoadingException;
import org.figuramc.figura.script_languages.lua.FiguraMetatables;
import org.figuramc.figura.script_languages.lua.LuaRuntime;
import org.figuramc.figura.script_languages.lua.cobalt.org.squiddev.cobalt.LuaError;
import org.figuramc.figura.script_languages.lua.cobalt.org.squiddev.cobalt.LuaState;
import org.figuramc.figura.script_languages.lua.cobalt.org.squiddev.cobalt.function.LibFunction;
import org.figuramc.figura.script_languages.lua.math.vector.Vector2API;
import org.figuramc.figura.script_languages.lua.math.vector.Vector3API;
import org.figuramc.figura.script_languages.lua.math.vector.Vector4API;
import org.joml.Vector2d;
import org.joml.Vector3d;
import org.joml.Vector4d;

/**
 * Set up the Figura Math functions, by running math.lua and adding some manually
 */
public class FiguraMath {

    public static void init(LuaState state, FiguraMetatables metatables) throws AvatarLoadingException {

        // Run math.lua
        LuaRuntime.runAssetFile(state, "math");

        // Math object creation functions

        // vec(varargs), creates a vec2, vec3, or vec4 based on the number of args.
        state.globals().rawset("vec", LibFunction.createV((s, args) -> switch (args.count()) {
            case 2 -> Vector2API.wrap(new Vector2d(
                    args.arg(1).checkDouble(s),
                    args.arg(2).checkDouble(s)
            ), metatables);
            case 3 -> Vector3API.wrap(new Vector3d(
                    args.arg(1).checkDouble(s),
                    args.arg(2).checkDouble(s),
                    args.arg(3).checkDouble(s)
            ), metatables);
            case 4 -> Vector4API.wrap(new Vector4d(
                    args.arg(1).checkDouble(s),
                    args.arg(2).checkDouble(s),
                    args.arg(3).checkDouble(s),
                    args.arg(4).checkDouble(s)
            ), metatables);
            default -> throw new LuaError("Invalid arg count to vec() - expected 2, 3, or 4 args, got " + args.count(), s.allocationTracker);
        }));

        // vec2(), vec3(), vec4(), creates a vec of the appropriate size, taking 0, 1, or N args.
        state.globals().rawset("vec2", LibFunction.createV((s, args) -> switch (args.count()) {
            case 0 -> Vector2API.wrap(new Vector2d(), metatables);
            case 1 -> Vector2API.wrap(new Vector2d(args.arg(1).checkDouble(s)), metatables);
            case 2 -> Vector2API.wrap(new Vector2d(
                    args.arg(1).checkDouble(s),
                    args.arg(2).checkDouble(s)
            ), metatables);
            default -> throw new LuaError("Invalid arg count to vec2() - expected 0, 1, or 2 args, got " + args.count(), s.allocationTracker);
        }));
        state.globals().rawset("vec3", LibFunction.createV((s, args) -> switch (args.count()) {
            case 0 -> Vector3API.wrap(new Vector3d(), metatables);
            case 1 -> Vector3API.wrap(new Vector3d(args.arg(1).checkDouble(s)), metatables);
            case 3 -> Vector3API.wrap(new Vector3d(
                    args.arg(1).checkDouble(s),
                    args.arg(2).checkDouble(s),
                    args.arg(3).checkDouble(s)
            ), metatables);
            default -> throw new LuaError("Invalid arg count to vec3() - expected 0, 1, or 3 args, got " + args.count(), s.allocationTracker);
        }));
        state.globals().rawset("vec4", LibFunction.createV((s, args) -> switch (args.count()) {
            case 0 -> Vector4API.wrap(new Vector4d(), metatables);
            case 1 -> Vector4API.wrap(new Vector4d(args.arg(1).checkDouble(s)), metatables);
            case 4 -> Vector4API.wrap(new Vector4d(
                    args.arg(1).checkDouble(s),
                    args.arg(2).checkDouble(s),
                    args.arg(3).checkDouble(s),
                    args.arg(4).checkDouble(s)
            ), metatables);
            default -> throw new LuaError("Invalid arg count to vec4() - expected 0, 1, or 4 args, got " + args.count(), s.allocationTracker);
        }));



    }


}
