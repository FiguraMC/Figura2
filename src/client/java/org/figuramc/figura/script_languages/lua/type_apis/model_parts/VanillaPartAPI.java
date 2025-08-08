package org.figuramc.figura.script_languages.lua.type_apis.model_parts;

import net.minecraft.client.model.geom.ModelPart;
import org.figuramc.figura.avatars.AvatarError;
import org.figuramc.figura.avatars.components.VanillaRendering;
import org.figuramc.figura.comptime.lua.annotations.LuaExpose;
import org.figuramc.figura.comptime.lua.annotations.LuaPassState;
import org.figuramc.figura.comptime.lua.annotations.LuaReturnSelf;
import org.figuramc.figura.comptime.lua.annotations.LuaTypeAPI;
import org.figuramc.figura.script_languages.lua.cobalt.org.squiddev.cobalt.*;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;

@LuaTypeAPI(typeName = "VanillaPart", wrappedClass = VanillaRendering.VanillaPart.class, hasSuperclass = true)
public class VanillaPartAPI {

    public static LuaUserdata wrap(VanillaRendering.VanillaPart part, LuaState state) {
        return new LuaUserdata(part, state.figuraMetatables.vanillaPart);
    }

    // Getter/setter for canceling vanilla operations
    @LuaExpose public static boolean cancelOrigin(VanillaRendering.VanillaPart self) { return self.cancelVanillaOrigin; }
    @LuaExpose @LuaReturnSelf public static void cancelOrigin(VanillaRendering.VanillaPart self, boolean cancel) { self.cancelVanillaOrigin = cancel; }
    @LuaExpose public static boolean cancelRot(VanillaRendering.VanillaPart self) { return self.cancelVanillaRotation; }
    @LuaExpose @LuaReturnSelf public static void cancelRot(VanillaRendering.VanillaPart self, boolean cancel) { self.cancelVanillaRotation = cancel; }
    @LuaExpose public static boolean cancelScale(VanillaRendering.VanillaPart self) { return self.cancelVanillaScale; }
    @LuaExpose @LuaReturnSelf public static void cancelScale(VanillaRendering.VanillaPart self, boolean cancel) { self.cancelVanillaScale = cancel; }

    // Fetching stored values
    @LuaExpose public static Vector3d storedOrigin(VanillaRendering.VanillaPart self) { return self.storedVanillaOrigin.get(new Vector3d()); }
    @LuaExpose public static Vector3d storedRot(VanillaRendering.VanillaPart self) { return storedRad(self).mul(180 / Math.PI); }
    @LuaExpose public static Vector3d storedRad(VanillaRendering.VanillaPart self) { return self.storedVanillaRotation.get(new Vector3d()); }
    @LuaExpose public static Vector3d storedScale(VanillaRendering.VanillaPart self) { return self.storedVanillaScale.get(new Vector3d()); }
    @LuaExpose public static Vector3d storedPos(VanillaRendering.VanillaPart self) { return self.storedVanillaPosition.get(new Vector3d()); }

    // Children
    @LuaExpose @LuaPassState public static LuaTable children(LuaState s, VanillaRendering.VanillaPart self) throws LuaError, AvatarError {
        LuaTable result = new LuaTable(s.allocationTracker);
        for (var entry : self.part.children.entrySet()) {
            VanillaRendering.VanillaPart scriptChild = self.getComponent().partMap.get(entry.getValue());
            if (scriptChild == null) continue;
            result.rawset(entry.getKey(), wrap(scriptChild, s));
        }
        return result;
    }

    // Custom __index fetches a child
    @LuaExpose public static @Nullable VanillaRendering.VanillaPart __index(VanillaRendering.VanillaPart self, LuaString key) {
        ModelPart child = self.part.children.get(key.toJavaStringNoAlloc());
        if (child == null) return null;
        return self.getComponent().partMap.get(child);
    }

}
