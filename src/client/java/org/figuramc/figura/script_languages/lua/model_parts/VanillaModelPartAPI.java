package org.figuramc.figura.script_languages.lua.model_parts;

import net.minecraft.util.Mth;
import org.figuramc.figura.model.part.VanillaRootModelPart;
import org.figuramc.figura.script_hooks.mem_count.AllocationTracker;
import org.figuramc.figura.script_languages.lua.FiguraMetatables;
import org.figuramc.figura.script_languages.lua.cobalt.org.squiddev.cobalt.LuaError;
import org.figuramc.figura.script_languages.lua.cobalt.org.squiddev.cobalt.LuaState;
import org.figuramc.figura.script_languages.lua.cobalt.org.squiddev.cobalt.LuaTable;
import org.figuramc.figura.script_languages.lua.cobalt.org.squiddev.cobalt.function.LibFunction;
import org.figuramc.figura.script_languages.lua.math.vector.Vector3API;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;

import static org.figuramc.figura.script_languages.lua.cobalt.org.squiddev.cobalt.Constants.NAME;
import static org.figuramc.figura.script_languages.lua.cobalt.org.squiddev.cobalt.ValueFactory.tableOf;
import static org.figuramc.figura.script_languages.lua.cobalt.org.squiddev.cobalt.ValueFactory.valueOf;

public class VanillaModelPartAPI {

    public static LuaTable createMetatable(LuaState state, FiguraMetatables metatables) throws LuaError {
        @Nullable AllocationTracker t = state.allocationTracker;
        LuaTable metatable = tableOf(t);
        FiguraMetatables.setupInheritance(metatable, metatables.rootModelPart); // Extends rootModelPart

        // Modifying vanilla transform
        metatable.rawset("vanillaOrigin", LibFunction.createV((s, args) -> {
            VanillaRootModelPart part = args.first().checkUserdata(s, VanillaRootModelPart.class);
            switch (args.count()) {
                case 1 -> { return Vector3API.wrap(new Vector3d(part.vanillaTransform.getOrigin()), metatables); }
                case 2 -> {
                    Vector3d vec = args.arg(2).checkUserdata(s, Vector3d.class);
                    part.vanillaTransform.setOrigin((float) vec.x, (float) vec.y, (float) vec.z);
                }
                case 4 -> part.vanillaTransform.setOrigin(
                        (float) args.arg(2).checkDouble(s),
                        (float) args.arg(3).checkDouble(s),
                        (float) args.arg(4).checkDouble(s));
                default -> throw new LuaError("Invalid number of args to VanillaModelPart:vanillaOrigin(): expected 0, 1, or 3", s.allocationTracker);
            }
            return args.first();
        }));
        metatable.rawset("vanillaPos", LibFunction.createV((s, args) -> {
            VanillaRootModelPart part = args.first().checkUserdata(s, VanillaRootModelPart.class);
            switch (args.count()) {
                case 1 -> { return Vector3API.wrap(new Vector3d(part.vanillaTransform.getPosition()), metatables); }
                case 2 -> {
                    Vector3d vec = args.arg(2).checkUserdata(s, Vector3d.class);
                    part.vanillaTransform.setPosition((float) vec.x, (float) vec.y, (float) vec.z);
                }
                case 4 -> part.vanillaTransform.setPosition(
                        (float) args.arg(2).checkDouble(s),
                        (float) args.arg(3).checkDouble(s),
                        (float) args.arg(4).checkDouble(s));
                default -> throw new LuaError("Invalid number of args to VanillaModelPart:vanillaPos(): expected 0, 1, or 3", s.allocationTracker);
            }
            return args.first();
        }));
        metatable.rawset("vanillaRot", LibFunction.createV((s, args) -> {
            VanillaRootModelPart part = args.first().checkUserdata(s, VanillaRootModelPart.class);
            switch (args.count()) {
                case 1 -> { return Vector3API.wrap(new Vector3d(part.vanillaTransform.getEulerRad()).mul(Mth.RAD_TO_DEG), metatables); }
                case 2 -> {
                    Vector3d vec = args.arg(2).checkUserdata(s, Vector3d.class);
                    part.vanillaTransform.setEulerDeg((float) vec.x, (float) vec.y, (float) vec.z);
                }
                case 4 -> part.vanillaTransform.setEulerDeg(
                        (float) args.arg(2).checkDouble(s),
                        (float) args.arg(3).checkDouble(s),
                        (float) args.arg(4).checkDouble(s));
                default -> throw new LuaError("Invalid number of args to VanillaModelPart:vanillaRot(): expected 0, 1, or 3", s.allocationTracker);
            }
            return args.first();
        }));
        metatable.rawset("vanillaRad", LibFunction.createV((s, args) -> {
            VanillaRootModelPart part = args.first().checkUserdata(s, VanillaRootModelPart.class);
            switch (args.count()) {
                case 1 -> { return Vector3API.wrap(new Vector3d(part.vanillaTransform.getEulerRad()), metatables); }
                case 2 -> {
                    Vector3d vec = args.arg(2).checkUserdata(s, Vector3d.class);
                    part.vanillaTransform.setEulerRad((float) vec.x, (float) vec.y, (float) vec.z);
                }
                case 4 -> part.vanillaTransform.setEulerRad(
                        (float) args.arg(2).checkDouble(s),
                        (float) args.arg(3).checkDouble(s),
                        (float) args.arg(4).checkDouble(s));
                default -> throw new LuaError("Invalid number of args to VanillaModelPart:vanillaRad(): expected 0, 1, or 3", s.allocationTracker);
            }
            return args.first();
        }));
        metatable.rawset("vanillaScale", LibFunction.createV((s, args) -> {
            VanillaRootModelPart part = args.first().checkUserdata(s, VanillaRootModelPart.class);
            switch (args.count()) {
                case 1 -> { return Vector3API.wrap(new Vector3d(part.vanillaTransform.getScale()), metatables); }
                case 2 -> {
                    Vector3d vec = args.arg(2).checkUserdata(s, Vector3d.class);
                    part.vanillaTransform.setScale((float) vec.x, (float) vec.y, (float) vec.z);
                }
                case 4 -> part.vanillaTransform.setScale(
                        (float) args.arg(2).checkDouble(s),
                        (float) args.arg(3).checkDouble(s),
                        (float) args.arg(4).checkDouble(s));
                default -> throw new LuaError("Invalid number of args to VanillaModelPart:vanillaScale(): expected 0, 1, or 3", s.allocationTracker);
            }
            return args.first();
        }));
        metatable.rawset("vanillaVis", LibFunction.createV((s, args) -> {
            VanillaRootModelPart part = args.first().checkUserdata(s, VanillaRootModelPart.class);
            switch (args.count()) {
                case 1 -> { return valueOf(part.vanillaTransform.getVisible()); }
                case 2 -> part.vanillaTransform.setVisible(args.arg(2).checkBoolean(s));
                default -> throw new LuaError("Invalid number of args to VanillaModelPart:vanillaVis(): expected 0 or 1", s.allocationTracker);
            }
            return args.first();
        }));

        // Cancelling vanilla transforms
        metatable.rawset("cancelOrigin", LibFunction.createV((s, args) -> {
            VanillaRootModelPart part = args.first().checkUserdata(s, VanillaRootModelPart.class);
            switch (args.count()) {
                case 1 -> { return valueOf(part.cancelVanillaOrigin); }
                case 2 -> part.cancelVanillaOrigin = args.arg(2).checkBoolean(s);
                default -> throw new LuaError("Invalid number of args to VanillaModelPart:cancelOrigin(): expected 0 or 1", s.allocationTracker);
            }
            return args.first();
        }));
        metatable.rawset("cancelRot", LibFunction.createV((s, args) -> {
            VanillaRootModelPart part = args.first().checkUserdata(s, VanillaRootModelPart.class);
            switch (args.count()) {
                case 1 -> { return valueOf(part.cancelVanillaRotation); }
                case 2 -> part.cancelVanillaRotation = args.arg(2).checkBoolean(s);
                default -> throw new LuaError("Invalid number of args to VanillaModelPart:cancelRot(): expected 0 or 1", s.allocationTracker);
            }
            return args.first();
        }));
        metatable.rawset("cancelScale", LibFunction.createV((s, args) -> {
            VanillaRootModelPart part = args.first().checkUserdata(s, VanillaRootModelPart.class);
            switch (args.count()) {
                case 1 -> { return valueOf(part.cancelVanillaScale); }
                case 2 -> part.cancelVanillaScale = args.arg(2).checkBoolean(s);
                default -> throw new LuaError("Invalid number of args to VanillaModelPart:cancelScale(): expected 0 or 1", s.allocationTracker);
            }
            return args.first();
        }));

        // Fetching stored transforms
        metatable.rawset("storedOrigin", LibFunction.create((s, part) ->
                Vector3API.wrap(new Vector3d(part.checkUserdata(s, VanillaRootModelPart.class).storedVanillaOrigin), metatables)));
        metatable.rawset("storedRot", LibFunction.create((s, part) ->
                Vector3API.wrap(new Vector3d(part.checkUserdata(s, VanillaRootModelPart.class).storedVanillaRotation), metatables)));
        metatable.rawset("storedScale", LibFunction.create((s, part) ->
                Vector3API.wrap(new Vector3d(part.checkUserdata(s, VanillaRootModelPart.class).storedVanillaScale), metatables)));

        // Name
        metatable.rawset(NAME, valueOf("VanillaModelPart", t));

        return metatable;
    }

}
