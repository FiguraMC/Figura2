package org.figuramc.figura.script_languages.lua.other_apis;

import net.minecraft.client.model.Model;
import org.figuramc.figura.avatars.AvatarError;
import org.figuramc.figura.avatars.components.VanillaRendering;
import org.figuramc.figura.script_languages.lua.cobalt.org.squiddev.cobalt.*;
import org.figuramc.figura.script_languages.lua.cobalt.org.squiddev.cobalt.function.LibFunction;
import org.figuramc.figura.script_languages.lua.type_apis.model_parts.VanillaPartAPI;
import org.figuramc.figura.vanillamodel.ModelNames;

/**
 * The global table "vanilla", storing APIs related to rendering of the vanilla entity.
 */
public class VanillaTable {

    public static LuaTable create(LuaState state, VanillaRendering component) throws LuaError, AvatarError {
        LuaTable vanilla = new LuaTable(state.allocationTracker);

        // Simple getter/setter pairs:

        // vanilla.hideAllParts(). 0 arg getter, 1 arg setter.
        vanilla.rawset("hideAllParts", LibFunction.createV((s, args) -> {
            switch (args.count()) {
                case 0 -> { return LuaBoolean.valueOf(component.hideAllModelParts); }
                case 1 -> component.hideAllModelParts = args.first().checkBoolean(s);
                default -> throw ErrorFactory.argCountError(s, "vanilla.cancelAllParts()", args.count(), 0, 1);
            }
            return Constants.NONE;
        }));

        // models:
        LuaTable models = ValueFactory.tableOf(state.allocationTracker);
        LuaTable modelsMetatable = ValueFactory.tableOf(state.allocationTracker);
        modelsMetatable.rawset("__index", LibFunction.create((s, self, key) -> {
            // Fetch the model for this entity renderer
            if (component.entityRenderer == null) throw new LuaError("Figura bug - entity renderer is null when trying to get entity model? Please report this to the devs!", s.allocationTracker);
            String name = key.checkString(s);
            Model model = ModelNames.getModelsByName(component.entityRenderer).get(name);
            // If we have a valid model, fetch its root.
            if (model == null) return Constants.NIL;
            VanillaRendering.VanillaPart scriptRoot = component.partMap.get(model.root());
            if (scriptRoot == null) return Constants.NIL;
            // Save it for later so we don't need to query this again
            LuaValue wrapped = VanillaPartAPI.wrap(scriptRoot, s);
            models.rawset(key, wrapped);
            return wrapped;
        }));
        models.setMetatable(state, modelsMetatable);
        vanilla.rawset("models", models);

        return vanilla;
    }

}
