package org.figuramc.figura.script_languages.lua.model_parts;

import net.minecraft.util.Mth;
import org.figuramc.figura.model.part.Transformable;
import org.figuramc.figura.script_hooks.mem_count.AllocationTracker;
import org.figuramc.figura.script_languages.lua.FiguraMetatables;
import org.figuramc.figura.script_languages.lua.cobalt.org.squiddev.cobalt.LuaError;
import org.figuramc.figura.script_languages.lua.cobalt.org.squiddev.cobalt.LuaState;
import org.figuramc.figura.script_languages.lua.cobalt.org.squiddev.cobalt.LuaTable;
import org.figuramc.figura.script_languages.lua.cobalt.org.squiddev.cobalt.function.LibFunction;
import org.figuramc.figura.script_languages.lua.math.vector.Vector3API;
import org.joml.Vector3d;

import static org.figuramc.figura.script_languages.lua.cobalt.org.squiddev.cobalt.ValueFactory.tableOf;
import static org.figuramc.figura.script_languages.lua.cobalt.org.squiddev.cobalt.ValueFactory.valueOf;

public class TransformableAPI {

    public static LuaTable createMetatable(LuaState state, FiguraMetatables metatables) throws LuaError {
        AllocationTracker t = state.allocationTracker;
        LuaTable metatable = tableOf(t);

        metatable.rawset("origin", LibFunction.createV((s, args) -> {
            Transformable part = args.first().checkUserdata(s, Transformable.class);
            switch (args.count()) {
                case 1 -> { return Vector3API.wrap(new Vector3d(part.getTransform().getOrigin()), metatables); }
                case 2 -> {
                    Vector3d vec = args.arg(2).checkUserdata(s, Vector3d.class);
                    part.getTransform().setOrigin((float) vec.x, (float) vec.y, (float) vec.z);
                }
                case 4 -> part.getTransform().setOrigin(
                        (float) args.arg(2).checkDouble(s),
                        (float) args.arg(3).checkDouble(s),
                        (float) args.arg(4).checkDouble(s));
                default -> throw new LuaError("Invalid number of args to ModelPart:origin(): expected 0, 1, or 3", s.allocationTracker);
            }
            return args.first();
        }));
        metatable.rawset("pos", LibFunction.createV((s, args) -> {
            Transformable part = args.first().checkUserdata(s, Transformable.class);
            switch (args.count()) {
                case 1 -> { return Vector3API.wrap(new Vector3d(part.getTransform().getPosition()), metatables); }
                case 2 -> {
                    Vector3d vec = args.arg(2).checkUserdata(s, Vector3d.class);
                    part.getTransform().setPosition((float) vec.x, (float) vec.y, (float) vec.z);
                }
                case 4 -> part.getTransform().setPosition(
                        (float) args.arg(2).checkDouble(s),
                        (float) args.arg(3).checkDouble(s),
                        (float) args.arg(4).checkDouble(s));
                default -> throw new LuaError("Invalid number of args to ModelPart:pos(): expected 0, 1, or 3", s.allocationTracker);
            }
            return args.first();
        }));
        metatable.rawset("rot", LibFunction.createV((s, args) -> {
            Transformable part = args.first().checkUserdata(s, Transformable.class);
            switch (args.count()) {
                case 1 -> { return Vector3API.wrap(new Vector3d(part.getTransform().getEulerRad()).mul(Mth.RAD_TO_DEG), metatables); }
                case 2 -> {
                    Vector3d vec = args.arg(2).checkUserdata(s, Vector3d.class);
                    part.getTransform().setEulerDeg((float) vec.x, (float) vec.y, (float) vec.z);
                }
                case 4 -> part.getTransform().setEulerDeg(
                        (float) args.arg(2).checkDouble(s),
                        (float) args.arg(3).checkDouble(s),
                        (float) args.arg(4).checkDouble(s));
                default -> throw new LuaError("Invalid number of args to ModelPart:rot(): expected 0, 1, or 3", s.allocationTracker);
            }
            return args.first();
        }));
        metatable.rawset("rad", LibFunction.createV((s, args) -> {
            Transformable part = args.first().checkUserdata(s, Transformable.class);
            switch (args.count()) {
                case 1 -> { return Vector3API.wrap(new Vector3d(part.getTransform().getEulerRad()), metatables); }
                case 2 -> {
                    Vector3d vec = args.arg(2).checkUserdata(s, Vector3d.class);
                    part.getTransform().setEulerRad((float) vec.x, (float) vec.y, (float) vec.z);
                }
                case 4 -> part.getTransform().setEulerRad(
                        (float) args.arg(2).checkDouble(s),
                        (float) args.arg(3).checkDouble(s),
                        (float) args.arg(4).checkDouble(s));
                default -> throw new LuaError("Invalid number of args to ModelPart:rad(): expected 0, 1, or 3", s.allocationTracker);
            }
            return args.first();
        })); // Angles in radians
        // quat() accepts 0 args, 1 quaternion arg, or 4 numeric args
//        metatable.rawset("quat", LibFunction.createV((s, args) -> {
//            Transformable part = args.first().checkUserdata(s, Transformable.class);
//        }));
        metatable.rawset("scale", LibFunction.createV((s, args) -> {
            Transformable part = args.first().checkUserdata(s, Transformable.class);
            switch (args.count()) {
                case 1 -> { return Vector3API.wrap(new Vector3d(part.getTransform().getScale()), metatables); }
                case 2 -> {
                    Vector3d vec = args.arg(2).checkUserdata(s, Vector3d.class);
                    part.getTransform().setScale((float) vec.x, (float) vec.y, (float) vec.z);
                }
                case 4 -> part.getTransform().setScale(
                        (float) args.arg(2).checkDouble(s),
                        (float) args.arg(3).checkDouble(s),
                        (float) args.arg(4).checkDouble(s));
                default -> throw new LuaError("Invalid number of args to ModelPart:scale(): expected 0, 1, or 3", s.allocationTracker);
            }
            return args.first();
        }));
        // color() accepts 0 args, 1 vector3 arg, or 3 numeric args.
        metatable.rawset("color", LibFunction.createV((s, args) -> {
            Transformable part = args.first().checkUserdata(s, Transformable.class);
            switch (args.count()) {
                case 1 -> { return Vector3API.wrap(part.getTransform().getColor().xyz(new Vector3d()), metatables); }
                case 2 -> {
                    Vector3d vec = args.arg(2).checkUserdata(s, Vector3d.class);
                    part.getTransform().setColor((float) vec.x, (float) vec.y, (float) vec.z, part.getTransform().getColor().w);
                }
                case 4 -> part.getTransform().setColor(
                        (float) args.arg(2).checkDouble(s),
                        (float) args.arg(3).checkDouble(s),
                        (float) args.arg(4).checkDouble(s),
                        part.getTransform().getColor().w
                );
                default -> throw new LuaError("Invalid number of args to ModelPart:color(): expected 0, 1, or 3", s.allocationTracker);
            }
            return args.first();
        }));
        metatable.rawset("vis", LibFunction.createV((s, args) -> {
            Transformable part = args.first().checkUserdata(s, Transformable.class);
            switch (args.count()) {
                case 1 -> { return valueOf(part.getTransform().getVisible()); }
                case 2 -> part.getTransform().setVisible(args.arg(2).checkBoolean(s));
                default -> throw new LuaError("Invalid number of args to ModelPart:vis(): expected 0 or 1", s.allocationTracker);
            }
            return args.first();
        }));

        FiguraMetatables.setupInheritance(state, metatable, null, null);

        return metatable;
    }


}
