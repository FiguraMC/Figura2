package org.figuramc.figura.script_languages.lua.model_parts;

import net.minecraft.resources.ResourceLocation;
import org.figuramc.figura.model.part.CustomItemModelPart;
import org.figuramc.figura.model.part.FiguraModelPart;
import org.figuramc.figura.model.part.WorldRootedModelPart;
import org.figuramc.figura.model.shader.FiguraRenderType;
import org.figuramc.figura.model.texture.AvatarTexture;
import org.figuramc.figura.script_hooks.mem_count.AllocationTracker;
import org.figuramc.figura.script_languages.lua.FiguraMetatables;
import org.figuramc.figura.script_languages.lua.cobalt.org.squiddev.cobalt.*;
import org.figuramc.figura.script_languages.lua.cobalt.org.squiddev.cobalt.function.LibFunction;

import static org.figuramc.figura.script_languages.lua.cobalt.org.squiddev.cobalt.Constants.NAME;
import static org.figuramc.figura.script_languages.lua.cobalt.org.squiddev.cobalt.Constants.NIL;
import static org.figuramc.figura.script_languages.lua.cobalt.org.squiddev.cobalt.ValueFactory.*;

public class ModelPartAPI {

    // Wrap an object into a userdata given the metatables
    public static LuaUserdata wrap(FiguraModelPart obj, FiguraMetatables metatables) {
        return switch (obj) {
            case WorldRootedModelPart world -> userdataOf(world, metatables.worldRootModelPart);
            case CustomItemModelPart item -> userdataOf(item, metatables.customItemModelPart);
            default -> userdataOf(obj, metatables.modelPart);
        };
    }

    // Return a LuaTable which acts as the metatable for this type
    public static LuaTable createMetatable(LuaState state, FiguraMetatables metatables) throws LuaError {
        AllocationTracker t = state.allocationTracker;
        LuaTable metatable = tableOf(t);

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
                    if (basic.mainTex != null) result.rawset("mainTex", valueOf(basic.mainTex.toString(), s.allocationTracker));
                    if (basic.emissiveTex != null) result.rawset("emissiveTex", valueOf(basic.emissiveTex.toString(), s.allocationTracker));
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

        FiguraMetatables.setupInheritance(state, metatable, metatables.transformable, LibFunction.create((s, p, k) -> {
            // Fetch the child
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
