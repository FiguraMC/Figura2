package org.figuramc.figura.script_languages.lua.model_parts;

import net.minecraft.client.model.geom.ModelPart;
import org.figuramc.figura.avatars.components.VanillaRendering;
import org.figuramc.figura.ducks.client.ModelPartTrackingAccess;
import org.figuramc.figura.script_languages.lua.FiguraMetatables;
import org.figuramc.figura.script_languages.lua.cobalt.org.squiddev.cobalt.*;
import org.figuramc.figura.script_languages.lua.cobalt.org.squiddev.cobalt.function.LibFunction;
import org.figuramc.figura.script_languages.lua.math.vector.Vector3API;
import org.joml.Vector3d;

public class VanillaPartAPI {

    public static LuaValue wrap(VanillaRendering.VanillaPart part, FiguraMetatables metatables) {
        return new LuaUserdata(part, metatables.vanillaModelPart);
    }

    public static LuaTable createMetatable(LuaState state, FiguraMetatables metatables, VanillaRendering component) throws LuaError {

        LuaTable metatable = ValueFactory.tableOf(state.allocationTracker);

        // Cancelling vanilla transforms
        metatable.rawset("cancelOrigin", LibFunction.createV((s, args) -> {
            VanillaRendering.VanillaPart part = args.first().checkUserdata(s, VanillaRendering.VanillaPart.class);
            switch (args.count()) {
                case 1 -> { return ValueFactory.valueOf(part.cancelVanillaOrigin); }
                case 2 -> part.cancelVanillaOrigin = args.arg(2).checkBoolean(s);
                default -> throw new LuaError("Invalid number of args to VanillaModelPart:cancelOrigin(): expected 0 or 1", s.allocationTracker);
            }
            return args.first();
        }));
        metatable.rawset("cancelRot", LibFunction.createV((s, args) -> {
            VanillaRendering.VanillaPart part = args.first().checkUserdata(s, VanillaRendering.VanillaPart.class);
            switch (args.count()) {
                case 1 -> { return ValueFactory.valueOf(part.cancelVanillaRotation); }
                case 2 -> part.cancelVanillaRotation = args.arg(2).checkBoolean(s);
                default -> throw new LuaError("Invalid number of args to VanillaModelPart:cancelRot(): expected 0 or 1", s.allocationTracker);
            }
            return args.first();
        }));
        metatable.rawset("cancelScale", LibFunction.createV((s, args) -> {
            VanillaRendering.VanillaPart part = args.first().checkUserdata(s, VanillaRendering.VanillaPart.class);
            switch (args.count()) {
                case 1 -> { return ValueFactory.valueOf(part.cancelVanillaScale); }
                case 2 -> part.cancelVanillaScale = args.arg(2).checkBoolean(s);
                default -> throw new LuaError("Invalid number of args to VanillaModelPart:cancelScale(): expected 0 or 1", s.allocationTracker);
            }
            return args.first();
        }));

        // Fetching stored transforms
        metatable.rawset("storedOrigin", LibFunction.create((s, part) -> Vector3API.wrap(new Vector3d(part.checkUserdata(s, VanillaRendering.VanillaPart.class).storedVanillaOrigin), metatables)));
        metatable.rawset("storedRot", LibFunction.create((s, part) -> Vector3API.wrap(new Vector3d(part.checkUserdata(s, VanillaRendering.VanillaPart.class).storedVanillaRotation), metatables)));
        metatable.rawset("storedScale", LibFunction.create((s, part) -> Vector3API.wrap(new Vector3d(part.checkUserdata(s, VanillaRendering.VanillaPart.class).storedVanillaScale), metatables)));

        // :name() - give the name of this part
        metatable.rawset("name", LibFunction.create((s, p) -> {
            ModelPart part = p.checkUserdata(s, VanillaRendering.VanillaPart.class).part;
            String name = ((ModelPartTrackingAccess) (Object) part).figura$getName();
            return ValueFactory.valueOf(name, s.allocationTracker);
        }));

        // :children() - get a list of the children of this part
        metatable.rawset("children", LibFunction.create((s, p) -> {
            VanillaRendering.VanillaPart part = p.checkUserdata(s, VanillaRendering.VanillaPart.class);
            LuaTable t = ValueFactory.tableOf(s.allocationTracker);
            int i = 1;
            for (ModelPart child : part.part.children.values()) {
                VanillaRendering.VanillaPart scriptChild = component.partMap.get(child);
                if (scriptChild == null) continue;
                t.rawset(i++, VanillaPartAPI.wrap(scriptChild, metatables));
            }
            return t;
        }));

        // Metamethod __name for error messages
        metatable.rawset(Constants.NAME, ValueFactory.valueOf("VanillaModelPart", state.allocationTracker));

        FiguraMetatables.setupInheritance(state, metatable, metatables.transformable, LibFunction.create((s, p, k) -> {
            // Fetch the child
            VanillaRendering.VanillaPart scriptPart = p.checkUserdata(s, VanillaRendering.VanillaPart.class);
            String name = k.checkString(s);
            ModelPart child = scriptPart.part.children.get(name);
            if (child == null) return Constants.NIL;
            VanillaRendering.VanillaPart scriptChild = component.partMap.get(child);
            if (scriptChild == null) return Constants.NIL;
            return VanillaPartAPI.wrap(scriptChild, metatables);
        }));

        return metatable;
    }

}
