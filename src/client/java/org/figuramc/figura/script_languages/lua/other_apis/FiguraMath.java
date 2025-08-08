package org.figuramc.figura.script_languages.lua.other_apis;

import org.figuramc.figura.avatars.AvatarError;
import org.figuramc.figura.script_languages.lua.LuaRuntime;
import org.figuramc.figura.script_languages.lua.cobalt.org.squiddev.cobalt.LuaError;
import org.figuramc.figura.script_languages.lua.cobalt.org.squiddev.cobalt.LuaState;
import org.figuramc.figura.script_languages.lua.cobalt.org.squiddev.cobalt.function.LibFunction;
import org.figuramc.figura.script_languages.lua.type_apis.math.vector.Vec2API;
import org.figuramc.figura.script_languages.lua.type_apis.math.vector.Vec4API;
import org.figuramc.figura.script_languages.lua.type_apis.math.vector.Vec3API;
import org.joml.Vector2d;
import org.joml.Vector3d;
import org.joml.Vector4d;

/**
 * Set up the Figura Math functions, by running math.lua and adding some manually
 */
public class FiguraMath {

    public static void init(LuaState state) throws AvatarError {

        // Run math.lua
        LuaRuntime.runAssetFile(state, "math");

        // Math object creation functions

        // vec2(), vec3(), vec4(), creates a vec of the appropriate size, taking 0, 1, or N args.
        state.globals().rawset("vec2", LibFunction.createV((s, args) -> switch (args.count()) {
            case 0 -> Vec2API.wrap(new Vector2d(), s);
            case 1 -> Vec2API.wrap(new Vector2d(args.arg(1).checkDouble(s)), s);
            case 2 -> Vec2API.wrap(new Vector2d(args.arg(1).checkDouble(s), args.arg(2).checkDouble(s)), s);
            default -> throw new LuaError("Invalid arg count to vec2() - expected 0, 1, or 2 args, got " + args.count(), s.allocationTracker);
        }));
        state.globals().rawset("vec3", LibFunction.createV((s, args) -> switch (args.count()) {
            case 0 -> Vec3API.wrap(new Vector3d(), s);
            case 1 -> Vec3API.wrap(new Vector3d(args.arg(1).checkDouble(s)), s);
            case 3 -> Vec3API.wrap(new Vector3d(args.arg(1).checkDouble(s), args.arg(2).checkDouble(s), args.arg(3).checkDouble(s)), s);
            default -> throw new LuaError("Invalid arg count to vec3() - expected 0, 1, or 3 args, got " + args.count(), s.allocationTracker);
        }));
        state.globals().rawset("vec4", LibFunction.createV((s, args) -> switch (args.count()) {
            case 0 -> Vec4API.wrap(new Vector4d(), s);
            case 1 -> Vec4API.wrap(new Vector4d(args.arg(1).checkDouble(s)), s);
            case 4 -> Vec4API.wrap(new Vector4d(args.arg(1).checkDouble(s), args.arg(2).checkDouble(s), args.arg(3).checkDouble(s), args.arg(4).checkDouble(s)), s);
            default -> throw new LuaError("Invalid arg count to vec4() - expected 0, 1, or 4 args, got " + args.count(), s.allocationTracker);
        }));



    }


}
