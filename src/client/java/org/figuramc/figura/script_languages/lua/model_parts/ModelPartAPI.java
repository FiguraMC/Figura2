package org.figuramc.figura.script_languages.lua.model_parts;

import net.minecraft.resources.ResourceLocation;
import org.figuramc.figura.avatars.AvatarError;
import org.figuramc.figura.model.part.CustomItemModelPart;
import org.figuramc.figura.model.part.FigmodelModelPart;
import org.figuramc.figura.model.part.FiguraModelPart;
import org.figuramc.figura.model.shader.FiguraRenderType;
import org.figuramc.figura.model.texture.AvatarTexture;
import org.figuramc.figura.script_hooks.mem_count.AllocationTracker;
import org.figuramc.figura.script_languages.lua.FiguraMetatables;
import org.figuramc.figura.script_languages.lua.cobalt.org.squiddev.cobalt.*;
import org.figuramc.figura.script_languages.lua.cobalt.org.squiddev.cobalt.function.LibFunction;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

import static org.figuramc.figura.script_languages.lua.cobalt.org.squiddev.cobalt.Constants.NAME;
import static org.figuramc.figura.script_languages.lua.cobalt.org.squiddev.cobalt.Constants.NIL;
import static org.figuramc.figura.script_languages.lua.cobalt.org.squiddev.cobalt.ValueFactory.tableOf;

public class ModelPartAPI {

    // Wrap an object into a userdata given the metatables
    public static LuaUserdata wrap(FiguraModelPart obj, LuaState state) {
        return switch (obj) {
            case FigmodelModelPart figmodel -> new LuaUserdata(figmodel, state.figuraMetatables.figmodelModelPart);
            case CustomItemModelPart item -> new LuaUserdata(item, state.figuraMetatables.customItemModelPart);
            default -> new LuaUserdata(obj, state.figuraMetatables.modelPart);
        };
    }

    // Return a LuaTable which acts as the metatable for this type
    public static LuaTable createMetatable(LuaState state, @NotNull LuaTable riggedHierarchy) throws LuaError, AvatarError {
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
                case FiguraRenderType.EndPortal e -> LuaString.valueOfNoAlloc("END_PORTAL");
                case FiguraRenderType.EndGateway e -> LuaString.valueOfNoAlloc("END_GATEWAY");
                case FiguraRenderType.Basic basic -> {
                    LuaTable result = tableOf(s.allocationTracker);
                    if (basic.mainTex != null) result.rawset("mainTex", LuaString.valueOf(s.allocationTracker, basic.mainTex.toString()));
                    if (basic.emissiveTex != null) result.rawset("emissiveTex", LuaString.valueOf(s.allocationTracker, basic.emissiveTex.toString()));
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

        // Get child with the given string name
        metatable.rawset("child", LibFunction.create((s, p, n) -> {
            FiguraModelPart part = p.checkUserdata(s, FiguraModelPart.class);
            String name = n.checkString(s);
            FiguraModelPart child = part.children.get(name);
            if (child == null) return NIL;
            return wrap(child, s);
        }));
        // Get table of children by name
        metatable.rawset("children", LibFunction.create((s, p) -> {
            FiguraModelPart part = p.checkUserdata(s, FiguraModelPart.class);
            LuaTable result = new LuaTable(0, part.children.size(), s.allocationTracker);
            for (var childEntry : part.children.entrySet())
                result.rawset(childEntry.getKey(), wrap(childEntry.getValue(), s));
            return result;
        }));

        metatable.rawset(NAME, LuaString.valueOfNoAlloc("ModelPart"));

        FiguraMetatables.setupIndexingWithSuperclassAndCustomIndexer(state, metatable, riggedHierarchy, LibFunction.create((s, p, k) -> {
            // Fetch the child
            FiguraModelPart part = p.checkUserdata(s, FiguraModelPart.class);
            String name = k.checkString(s);
            FiguraModelPart child = part.children.get(name);
            if (child == null) return NIL;
            return wrap(child, s);
        }));
        
        return metatable;
    }


}
