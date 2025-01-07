package org.figuramc.figura.script_languages.lua.model_parts;

import org.figuramc.figura.model.part.*;
import org.figuramc.figura.script_hooks.mem_count.AllocationTracker;
import org.figuramc.figura.script_languages.lua.FiguraMetatables;
import org.figuramc.figura.script_languages.lua.cobalt.org.squiddev.cobalt.*;
import org.figuramc.figura.script_languages.lua.cobalt.org.squiddev.cobalt.function.LibFunction;
import org.figuramc.figura.script_languages.lua.math.Vector3API;
import org.joml.Vector3d;

import static org.figuramc.figura.script_languages.lua.cobalt.org.squiddev.cobalt.Constants.INDEX;
import static org.figuramc.figura.script_languages.lua.cobalt.org.squiddev.cobalt.Constants.NIL;
import static org.figuramc.figura.script_languages.lua.cobalt.org.squiddev.cobalt.ValueFactory.*;

public class ModelPartAPI {

    // Wrap an object into a userdata given the metatables
    public static LuaUserdata wrap(FiguraModelPart obj, FiguraMetatables metatables) {
        return switch (obj) {
            case RootModelPart root -> switch (root) {
                case VanillaRootModelPart vanilla -> userdataOf(vanilla, metatables.vanillaRootModelPart);
                case WorldRootModelPart world -> userdataOf(world, metatables.worldRootModelPart);
                case CustomItemModelPart item -> userdataOf(item, metatables.customItemModelPart);
                default -> userdataOf(root, metatables.rootModelPart);
            };
            default -> userdataOf(obj, metatables.modelPart);
        };
    }

    // Return a LuaTable which acts as the metatable for this type
    public static LuaTable createMetatable(LuaState state, FiguraMetatables metatables) throws LuaError {
        AllocationTracker t = state.allocationTracker;
        LuaTable metatable = tableOf(t);

        // 0 args: get value
        // 1 vector3 arg: set value to vector3
        // 3 number args: set value to args
        metatable.rawset("origin", LibFunction.createV((s, args) -> {
            FiguraModelPart part = args.first().checkUserdata(s, FiguraModelPart.class);
            switch (args.count()) {
                case 1 -> { return Vector3API.wrap(new Vector3d(part.transform.getOrigin()), metatables); }
                case 2 -> {
                    Vector3d vec = args.arg(2).checkUserdata(s, Vector3d.class);
                    part.transform.setOrigin((float) vec.x, (float) vec.y, (float) vec.z);
                }
                case 4 -> part.transform.setOrigin(
                        (float) args.arg(2).checkDouble(s),
                        (float) args.arg(3).checkDouble(s),
                        (float) args.arg(4).checkDouble(s));
                default -> throw new LuaError("Invalid number of args to ModelPart:origin(): expected 0, 1, or 3", s.allocationTracker);
            }
            return args.first();
        }));
        metatable.rawset("pos", LibFunction.createV((s, args) -> {
            FiguraModelPart part = args.first().checkUserdata(s, FiguraModelPart.class);
            switch (args.count()) {
                case 1 -> { return Vector3API.wrap(new Vector3d(part.transform.getPosition()), metatables); }
                case 2 -> {
                    Vector3d vec = args.arg(2).checkUserdata(s, Vector3d.class);
                    part.transform.setPosition((float) vec.x, (float) vec.y, (float) vec.z);
                }
                case 4 -> part.transform.setPosition(
                        (float) args.arg(2).checkDouble(s),
                        (float) args.arg(3).checkDouble(s),
                        (float) args.arg(4).checkDouble(s));
                default -> throw new LuaError("Invalid number of args to ModelPart:pos(): expected 0, 1, or 3", s.allocationTracker);
            }
            return args.first();
        }));
        metatable.rawset("scale", LibFunction.createV((s, args) -> {
            FiguraModelPart part = args.first().checkUserdata(s, FiguraModelPart.class);
            switch (args.count()) {
                case 1 -> { return Vector3API.wrap(new Vector3d(part.transform.getScale()), metatables); }
                case 2 -> {
                    Vector3d vec = args.arg(2).checkUserdata(s, Vector3d.class);
                    part.transform.setScale((float) vec.x, (float) vec.y, (float) vec.z);
                }
                case 4 -> part.transform.setScale(
                        (float) args.arg(2).checkDouble(s),
                        (float) args.arg(3).checkDouble(s),
                        (float) args.arg(4).checkDouble(s));
                default -> throw new LuaError("Invalid number of args to ModelPart:scale(): expected 0, 1, or 3", s.allocationTracker);
            }
            return args.first();
        }));
        metatable.rawset("rot", LibFunction.createV((s, args) -> {
            FiguraModelPart part = args.first().checkUserdata(s, FiguraModelPart.class);
            switch (args.count()) {
                case 1 -> { return Vector3API.wrap(new Vector3d(part.transform.getEulerRad()).mul(180 / Math.PI), metatables); }
                case 2 -> {
                    Vector3d vec = args.arg(2).checkUserdata(s, Vector3d.class);
                    part.transform.setEulerDeg((float) vec.x, (float) vec.y, (float) vec.z);
                }
                case 4 -> part.transform.setEulerDeg(
                        (float) args.arg(2).checkDouble(s),
                        (float) args.arg(3).checkDouble(s),
                        (float) args.arg(4).checkDouble(s));
                default -> throw new LuaError("Invalid number of args to ModelPart:rot(): expected 0, 1, or 3", s.allocationTracker);
            }
            return args.first();
        }));
        metatable.rawset("rad", LibFunction.createV((s, args) -> {
            FiguraModelPart part = args.first().checkUserdata(s, FiguraModelPart.class);
            switch (args.count()) {
                case 1 -> { return Vector3API.wrap(new Vector3d(part.transform.getEulerRad()), metatables); }
                case 2 -> {
                    Vector3d vec = args.arg(2).checkUserdata(s, Vector3d.class);
                    part.transform.setEulerRad((float) vec.x, (float) vec.y, (float) vec.z);
                }
                case 4 -> part.transform.setEulerRad(
                        (float) args.arg(2).checkDouble(s),
                        (float) args.arg(3).checkDouble(s),
                        (float) args.arg(4).checkDouble(s));
                default -> throw new LuaError("Invalid number of args to ModelPart:rad(): expected 0, 1, or 3", s.allocationTracker);
            }
            return args.first();
        })); // Angles in radians
        // quat() accepts 0 args, 1 vector4 arg, or 4 numeric args
//        metatable.rawset("quat", LibFunction.createV((s, args) -> {
//            FiguraModelPart part = args.first().checkUserdata(s, FiguraModelPart.class);
//        }));

        // Other operations

        // Get child with the given string name
        metatable.rawset("child", LibFunction.create((s, p, n) -> {
            FiguraModelPart part = p.checkUserdata(s, FiguraModelPart.class);
            LuaString name = n.checkLuaString(s);
            for (FiguraModelPart child : part.children) {
                if (name.equals(child.name))
                    return wrap(child, metatables);
            }
            return NIL;
        }));
        // Get table of all children
        metatable.rawset("children", LibFunction.create((s, p) -> {
            FiguraModelPart part = p.checkUserdata(s, FiguraModelPart.class);
            int c = part.children.size();
            LuaTable result = new LuaTable(c, 1, s.allocationTracker);
            for (int i = 0; i < c; i++) {
                result.rawset(i+1, wrap(part.children.get(i), metatables));
            }
            return result;
        }));

        // Special __index: gets child by name if it's not a method
        metatable.rawset(INDEX, LibFunction.create((s, p, k) -> {
            // Try to rawget from metatable for method, if it exists then return it
            LuaValue rawResult = metatable.rawget(k);
            if (!rawResult.isNil()) return rawResult;
            // Otherwise, custom __index time, gets child
            FiguraModelPart part = p.checkUserdata(s, FiguraModelPart.class);
            LuaString name = k.checkLuaString(s);
            for (FiguraModelPart child : part.children) {
                if (name.equals(child.name))
                    return wrap(child, metatables);
            }
            return NIL;
        }));

        
        return metatable;
    }


}
