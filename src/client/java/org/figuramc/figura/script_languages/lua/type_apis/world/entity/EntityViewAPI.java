package org.figuramc.figura.script_languages.lua.type_apis.world.entity;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.figuramc.figura.avatars.AvatarError;
import org.figuramc.figura.comptime.lua.annotations.LuaExpose;
import org.figuramc.figura.comptime.lua.annotations.LuaPassState;
import org.figuramc.figura.comptime.lua.annotations.LuaTypeAPI;
import org.figuramc.figura.script_hooks.callback.items.EntityView;
import org.figuramc.figura.script_languages.lua.cobalt.org.squiddev.cobalt.LuaError;
import org.figuramc.figura.script_languages.lua.cobalt.org.squiddev.cobalt.LuaState;
import org.figuramc.figura.script_languages.lua.cobalt.org.squiddev.cobalt.LuaUserdata;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;

@LuaTypeAPI(typeName = "Entity", wrappedClass = EntityView.class)
public class EntityViewAPI {

    public static LuaUserdata wrap(EntityView<?> entityView, LuaState state) {
        return new LuaUserdata(entityView, state.figuraMetatables.entityView);
    }

    @LuaExpose @LuaPassState
    public static Vector3d pos(LuaState s, EntityView<?> self) throws LuaError, AvatarError {
        Vec3 mcVec = fetchEntity(s, self).position();
        return new Vector3d(mcVec.x, mcVec.y, mcVec.z);
    }
    @LuaExpose @LuaPassState
    public static Vector3d pos(LuaState s, EntityView<?> self, float delta) throws LuaError, AvatarError {
        Vec3 mcVec = fetchEntity(s, self).getPosition(delta);
        return new Vector3d(mcVec.x, mcVec.y, mcVec.z);
    }

    // Helper to fetch entity, or error if revoked
    private static @NotNull Entity fetchEntity(LuaState state, EntityView<?> entityView) throws LuaError, AvatarError {
        // Get entity
        @Nullable Entity entity = entityView.getEntity();
        // If null (aka revoked), error
        if (entity == null) throw new LuaError("Attempt to use entity view after it was revoked!", state.allocationTracker);
        // Return the non-null entity
        return entity;
    }

}
