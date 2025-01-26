package org.figuramc.figura.script_languages.lua.model_parts;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.figuramc.figura.model.part.*;
import org.figuramc.figura.model.shader.FiguraRenderType;
import org.figuramc.figura.model.texture.AvatarTexture;
import org.figuramc.figura.script_hooks.mem_count.AllocationTracker;
import org.figuramc.figura.script_languages.lua.FiguraMetatables;
import org.figuramc.figura.script_languages.lua.cobalt.org.squiddev.cobalt.*;
import org.figuramc.figura.script_languages.lua.cobalt.org.squiddev.cobalt.function.LibFunction;
import org.figuramc.figura.script_languages.lua.math.vector.Vector3API;
import org.joml.Vector3d;

import static org.figuramc.figura.script_languages.lua.cobalt.org.squiddev.cobalt.Constants.*;
import static org.figuramc.figura.script_languages.lua.cobalt.org.squiddev.cobalt.ValueFactory.*;

public class ModelPartAPI {

    // Wrap an object into a userdata given the metatables
    public static LuaUserdata wrap(FiguraModelPart obj, FiguraMetatables metatables) {
        //noinspection SwitchStatementWithTooFewBranches
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
        metatable.rawset("rot", LibFunction.createV((s, args) -> {
            FiguraModelPart part = args.first().checkUserdata(s, FiguraModelPart.class);
            switch (args.count()) {
                case 1 -> { return Vector3API.wrap(new Vector3d(part.transform.getEulerRad()).mul(Mth.RAD_TO_DEG), metatables); }
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
        // quat() accepts 0 args, 1 quaternion arg, or 4 numeric args
//        metatable.rawset("quat", LibFunction.createV((s, args) -> {
//            FiguraModelPart part = args.first().checkUserdata(s, FiguraModelPart.class);
//        }));
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
        // color() accepts 0 args, 1 vector3 arg, or 3 numeric args.
        metatable.rawset("color", LibFunction.createV((s, args) -> {
            FiguraModelPart part = args.first().checkUserdata(s, FiguraModelPart.class);
            switch (args.count()) {
                case 1 -> { return Vector3API.wrap(part.transform.getColor().xyz(new Vector3d()), metatables); }
                case 2 -> {
                    Vector3d vec = args.arg(2).checkUserdata(s, Vector3d.class);
                    part.transform.setColor((float) vec.x, (float) vec.y, (float) vec.z, part.transform.getColor().w);
                }
                case 4 -> part.transform.setColor(
                        (float) args.arg(2).checkDouble(s),
                        (float) args.arg(3).checkDouble(s),
                        (float) args.arg(4).checkDouble(s),
                        part.transform.getColor().w
                );
                default -> throw new LuaError("Invalid number of args to ModelPart:color(): expected 0, 1, or 3", s.allocationTracker);
            }
            return args.first();
        }));
        metatable.rawset("vis", LibFunction.createV((s, args) -> {
            FiguraModelPart part = args.first().checkUserdata(s, FiguraModelPart.class);
            switch (args.count()) {
                case 1 -> { return valueOf(part.transform.getVisible()); }
                case 2 -> part.transform.setVisible(args.arg(2).checkBoolean(s));
                default -> throw new LuaError("Invalid number of args to ModelPart:vis(): expected 0 or 1", s.allocationTracker);
            }
            return args.first();
        }));

        // Render type has separate getter/setter.
        // This is because setting to nil needs to be a valid operation,
        // and we want calling with 0 args to be the same as setting to nil.

        // TODO improve these functions! They aren't final and may need to change as rendering evolves.

        metatable.rawset("getRenderType", LibFunction.create((s, p) -> {
            FiguraModelPart part = p.checkUserdata(s, FiguraModelPart.class);
            return switch (part.getRenderType()) {
                case null -> NIL;
                case FiguraRenderType.EndPortal e -> valueOf("END_PORTAL", s.allocationTracker);
                case FiguraRenderType.EndGateway e -> valueOf("END_GATEWAY", s.allocationTracker);
                case FiguraRenderType.Basic basic -> {
                    LuaTable result = tableOf(s.allocationTracker);
                    if (basic.mainTex() != null) result.rawset("mainTex", valueOf(basic.mainTex().toString(), s.allocationTracker));
                    if (basic.emissiveTex() != null) result.rawset("emissiveTex", valueOf(basic.emissiveTex().toString(), s.allocationTracker));
                    yield result;
                }
            };
        }));
        metatable.rawset("setRenderType", LibFunction.create((s, p, ty, pri) -> {
            FiguraModelPart part = p.checkUserdata(s, FiguraModelPart.class);
            part.renderTypePriority = pri.optInteger(s, 1); // Priority is 1 by default, to override 0
            part.setRenderType(switch (ty) {
                case LuaNil nil -> null;
                case LuaString string -> switch (string.toString()) {
                    case "END_PORTAL" -> FiguraRenderType.EndPortal.INSTANCE;
                    case "END_GATEWAY" -> FiguraRenderType.EndGateway.INSTANCE;
                    default -> throw new LuaError("Invalid string arg to ModelPart:setRenderType(): \"" + string + "\"", s.allocationTracker);
                };
                case LuaTable table -> new FiguraRenderType.Basic(
                        switch (table.rawget("mainTex")) {
                            case LuaNil nil -> null;
                            case LuaString string -> ResourceLocation.parse(string.toString());
                            case LuaUserdata texture -> texture.checkUserdata(s, AvatarTexture.class).getLocation();
                            default -> throw new LuaError("Invalid value for key \"mainTex\" in ModelPart:setRenderType() - expected string or texture", s.allocationTracker);
                        },
                        switch (table.rawget("emissiveTex")) {
                            case LuaNil nil -> null;
                            case LuaString string -> ResourceLocation.parse(string.toString());
                            case LuaUserdata texture -> texture.checkUserdata(s, AvatarTexture.class).getLocation();
                            default -> throw new LuaError("Invalid value for key \"emissiveTex\" in ModelPart:setRenderType() - expected string or texture", s.allocationTracker);
                        }
                );
                default -> throw new LuaError("Invalid arg to ModelPart:setRenderType(). Expected nil, string, or table", s.allocationTracker);
            });
            return p;
        }));
        // Other operations

        // Get name of the part
        metatable.rawset("name", LibFunction.create((s, p) -> {
            FiguraModelPart part = p.checkUserdata(s, FiguraModelPart.class);
            return valueOf(part.name, s.allocationTracker);
        }));

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

        metatable.rawset(NAME, valueOf("ModelPart", t));

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
