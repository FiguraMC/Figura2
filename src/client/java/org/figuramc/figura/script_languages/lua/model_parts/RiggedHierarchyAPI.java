package org.figuramc.figura.script_languages.lua.model_parts;

import net.minecraft.util.Mth;
import org.figuramc.figura.avatars.AvatarError;
import org.figuramc.figura.model.part.RiggedHierarchy;
import org.figuramc.figura.script_hooks.mem_count.AllocationTracker;
import org.figuramc.figura.script_languages.lua.FiguraMetatables;
import org.figuramc.figura.script_languages.lua.cobalt.org.squiddev.cobalt.LuaBoolean;
import org.figuramc.figura.script_languages.lua.cobalt.org.squiddev.cobalt.LuaError;
import org.figuramc.figura.script_languages.lua.cobalt.org.squiddev.cobalt.LuaState;
import org.figuramc.figura.script_languages.lua.cobalt.org.squiddev.cobalt.LuaTable;
import org.figuramc.figura.script_languages.lua.cobalt.org.squiddev.cobalt.function.LibFunction;
import org.figuramc.figura.script_languages.lua.math.vector.Vector3API;
import org.figuramc.figura.script_languages.lua.math.vector.Vector4API;
import org.joml.Vector3d;
import org.joml.Vector4d;

import static org.figuramc.figura.script_languages.lua.cobalt.org.squiddev.cobalt.ValueFactory.tableOf;

public class RiggedHierarchyAPI {

    public static LuaTable createMetatable(LuaState state) throws LuaError, AvatarError {
        AllocationTracker t = state.allocationTracker;
        LuaTable metatable = tableOf(t);

        metatable.rawset("origin", LibFunction.createV((s, args) -> {
            RiggedHierarchy<?> part = args.first().checkUserdata(s, RiggedHierarchy.class);
            switch (args.count()) {
                case 1 -> { return Vector3API.wrap(new Vector3d(part.getTransform().getOrigin()), s); }
                case 2 -> {
                    Vector3d vec = args.arg(2).checkUserdata(s, Vector3d.class);
                    part.getTransform().setOrigin((float) vec.x, (float) vec.y, (float) vec.z);
                }
                case 4 -> part.getTransform().setOrigin(
                        (float) args.arg(2).checkDouble(s),
                        (float) args.arg(3).checkDouble(s),
                        (float) args.arg(4).checkDouble(s));
                default -> throw new LuaError("Invalid number of args to Transformable:origin(): expected 0, 1, or 3", s.allocationTracker);
            }
            return args.first();
        }));
        metatable.rawset("pos", LibFunction.createV((s, args) -> {
            RiggedHierarchy<?> part = args.first().checkUserdata(s, RiggedHierarchy.class);
            switch (args.count()) {
                case 1 -> { return Vector3API.wrap(new Vector3d(part.getTransform().getPosition()), s); }
                case 2 -> {
                    Vector3d vec = args.arg(2).checkUserdata(s, Vector3d.class);
                    part.getTransform().setPosition((float) vec.x, (float) vec.y, (float) vec.z);
                }
                case 4 -> part.getTransform().setPosition(
                        (float) args.arg(2).checkDouble(s),
                        (float) args.arg(3).checkDouble(s),
                        (float) args.arg(4).checkDouble(s));
                default -> throw new LuaError("Invalid number of args to Transformable:pos(): expected 0, 1, or 3", s.allocationTracker);
            }
            return args.first();
        }));
        metatable.rawset("rot", LibFunction.createV((s, args) -> {
            RiggedHierarchy<?> part = args.first().checkUserdata(s, RiggedHierarchy.class);
            switch (args.count()) {
                case 1 -> { return Vector3API.wrap(new Vector3d(part.getTransform().getEulerRad()).mul(Mth.RAD_TO_DEG), s); }
                case 2 -> {
                    Vector3d vec = args.arg(2).checkUserdata(s, Vector3d.class);
                    part.getTransform().setEulerDeg((float) vec.x, (float) vec.y, (float) vec.z);
                }
                case 4 -> part.getTransform().setEulerDeg(
                        (float) args.arg(2).checkDouble(s),
                        (float) args.arg(3).checkDouble(s),
                        (float) args.arg(4).checkDouble(s));
                default -> throw new LuaError("Invalid number of args to Transformable:rot(): expected 0, 1, or 3", s.allocationTracker);
            }
            return args.first();
        }));
        metatable.rawset("rad", LibFunction.createV((s, args) -> {
            RiggedHierarchy<?> part = args.first().checkUserdata(s, RiggedHierarchy.class);
            switch (args.count()) {
                case 1 -> { return Vector3API.wrap(new Vector3d(part.getTransform().getEulerRad()), s); }
                case 2 -> {
                    Vector3d vec = args.arg(2).checkUserdata(s, Vector3d.class);
                    part.getTransform().setEulerRad((float) vec.x, (float) vec.y, (float) vec.z);
                }
                case 4 -> part.getTransform().setEulerRad(
                        (float) args.arg(2).checkDouble(s),
                        (float) args.arg(3).checkDouble(s),
                        (float) args.arg(4).checkDouble(s));
                default -> throw new LuaError("Invalid number of args to Transformable:rad(): expected 0, 1, or 3", s.allocationTracker);
            }
            return args.first();
        })); // Angles in radians
        // quat() accepts 0 args, 1 quaternion arg, or 4 numeric args
//        metatable.rawset("quat", LibFunction.createV((s, args) -> {
//            Transformable part = args.first().checkUserdata(s, Transformable.class);
//        }));
        metatable.rawset("scale", LibFunction.createV((s, args) -> {
            RiggedHierarchy<?> part = args.first().checkUserdata(s, RiggedHierarchy.class);
            switch (args.count()) {
                case 1 -> { return Vector3API.wrap(new Vector3d(part.getTransform().getScale()), s); }
                case 2 -> {
                    Vector3d vec = args.arg(2).checkUserdata(s, Vector3d.class);
                    part.getTransform().setScale((float) vec.x, (float) vec.y, (float) vec.z);
                }
                case 4 -> part.getTransform().setScale(
                        (float) args.arg(2).checkDouble(s),
                        (float) args.arg(3).checkDouble(s),
                        (float) args.arg(4).checkDouble(s));
                default -> throw new LuaError("Invalid number of args to Transformable:scale(): expected 0, 1, or 3", s.allocationTracker);
            }
            return args.first();
        }));
        // color() accepts 0 args, 1 vector4 arg, or 4 numeric args.
        metatable.rawset("color", LibFunction.createV((s, args) -> {
            RiggedHierarchy<?> part = args.first().checkUserdata(s, RiggedHierarchy.class);
            switch (args.count()) {
                case 1 -> { return Vector4API.wrap(new Vector4d(part.getTransform().getColor()), s); }
                case 2 -> {
                    Vector4d vec = args.arg(2).checkUserdata(s, Vector4d.class);
                    part.getTransform().setColor((float) vec.x, (float) vec.y, (float) vec.z, (float) vec.w);
                }
                case 5 -> part.getTransform().setColor(
                        (float) args.arg(2).checkDouble(s),
                        (float) args.arg(3).checkDouble(s),
                        (float) args.arg(4).checkDouble(s),
                        (float) args.arg(5).checkDouble(s)
                );
                default -> throw new LuaError("Invalid number of args to Transformable:color(): expected 0, 1, or 4", s.allocationTracker);
            }
            return args.first();
        }));
        metatable.rawset("vis", LibFunction.createV((s, args) -> {
            RiggedHierarchy<?> part = args.first().checkUserdata(s, RiggedHierarchy.class);
            switch (args.count()) {
                case 1 -> { return LuaBoolean.valueOf(part.getTransform().getVisible()); }
                case 2 -> part.getTransform().setVisible(args.arg(2).checkBoolean(s));
                default -> throw new LuaError("Invalid number of args to Transformable:vis(): expected 0 or 1", s.allocationTracker);
            }
            return args.first();
        }));

        FiguraMetatables.setupIndexing(state, metatable);

        return metatable;
    }


}
