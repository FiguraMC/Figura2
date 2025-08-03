package org.figuramc.figura.script_languages.lua.vanilla;

import net.minecraft.client.model.geom.ModelPart;
import org.figuramc.figura.avatars.AvatarError;
import org.figuramc.figura.avatars.components.VanillaRendering;
import org.figuramc.figura.model.part.RiggedHierarchy;
import org.figuramc.figura.script_languages.lua.FiguraMetatables;
import org.figuramc.figura.script_languages.lua.cobalt.org.squiddev.cobalt.*;
import org.figuramc.figura.script_languages.lua.cobalt.org.squiddev.cobalt.function.LibFunction;
import org.figuramc.figura.script_languages.lua.math.vector.Vector3API;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3d;

import java.util.Objects;

public class VanillaPartAPI {

    public static LuaValue wrap(VanillaRendering.VanillaPart part, LuaState state) {
        return new LuaUserdata(part, state.figuraMetatables.vanillaPart);
    }

    public static LuaTable createMetatable(LuaState state, @NotNull LuaTable riggedHierarchy) throws LuaError, AvatarError {

        LuaTable metatable = ValueFactory.tableOf(state.allocationTracker);

        // Cancelling vanilla transforms
        metatable.rawset("cancelOrigin", LibFunction.createV((s, args) -> {
            VanillaRendering.VanillaPart part = args.first().checkUserdata(s, VanillaRendering.VanillaPart.class);
            switch (args.count()) {
                case 1 -> { return LuaBoolean.valueOf(part.cancelVanillaOrigin); }
                case 2 -> part.cancelVanillaOrigin = args.arg(2).checkBoolean(s);
                default -> throw new LuaError("Invalid number of args to VanillaModelPart:cancelOrigin(): expected 0 or 1", s.allocationTracker);
            }
            return args.first();
        }));
        metatable.rawset("cancelRot", LibFunction.createV((s, args) -> {
            VanillaRendering.VanillaPart part = args.first().checkUserdata(s, VanillaRendering.VanillaPart.class);
            switch (args.count()) {
                case 1 -> { return LuaBoolean.valueOf(part.cancelVanillaRotation); }
                case 2 -> part.cancelVanillaRotation = args.arg(2).checkBoolean(s);
                default -> throw new LuaError("Invalid number of args to VanillaModelPart:cancelRot(): expected 0 or 1", s.allocationTracker);
            }
            return args.first();
        }));
        metatable.rawset("cancelScale", LibFunction.createV((s, args) -> {
            VanillaRendering.VanillaPart part = args.first().checkUserdata(s, VanillaRendering.VanillaPart.class);
            switch (args.count()) {
                case 1 -> { return LuaBoolean.valueOf(part.cancelVanillaScale); }
                case 2 -> part.cancelVanillaScale = args.arg(2).checkBoolean(s);
                default -> throw new LuaError("Invalid number of args to VanillaModelPart:cancelScale(): expected 0 or 1", s.allocationTracker);
            }
            return args.first();
        }));

        // Fetching stored transforms
        metatable.rawset("storedOrigin", LibFunction.create((s, part) -> Vector3API.wrap(new Vector3d(part.checkUserdata(s, VanillaRendering.VanillaPart.class).storedVanillaOrigin), s)));
        metatable.rawset("storedRot", LibFunction.create((s, part) -> Vector3API.wrap(new Vector3d(part.checkUserdata(s, VanillaRendering.VanillaPart.class).storedVanillaRotation), s)));
        metatable.rawset("storedScale", LibFunction.create((s, part) -> Vector3API.wrap(new Vector3d(part.checkUserdata(s, VanillaRendering.VanillaPart.class).storedVanillaScale), s)));

        // :children() - get a table of the children of this part by name
        metatable.rawset("children", LibFunction.create((s, p) -> {
            VanillaRendering.VanillaPart part = p.checkUserdata(s, VanillaRendering.VanillaPart.class);
            LuaTable t = ValueFactory.tableOf(s.allocationTracker);
            for (var childEntry : part.part.children.entrySet()) {
                VanillaRendering.VanillaPart scriptChild = part.getComponent().partMap.get(childEntry.getValue());
                if (scriptChild == null) continue;
                t.rawset(childEntry.getKey(), wrap(scriptChild, s));
            }
            return t;
        }));

        // Metamethod __name for error messages
        metatable.rawset(Constants.NAME, LuaString.valueOfNoAlloc("VanillaPart"));

        FiguraMetatables.setupIndexingWithSuperclassAndCustomIndexer(state, metatable, riggedHierarchy, LibFunction.create((s, p, k) -> {
            // Fetch the child
            VanillaRendering.VanillaPart scriptPart = p.checkUserdata(s, VanillaRendering.VanillaPart.class);
            String name = k.checkString(s);
            ModelPart child = scriptPart.part.children.get(name);
            if (child == null) return Constants.NIL;
            VanillaRendering.VanillaPart scriptChild = scriptPart.getComponent().partMap.get(child);
            if (scriptChild == null) return Constants.NIL;
            return VanillaPartAPI.wrap(scriptChild, s);
        }));

        return metatable;
    }

}
