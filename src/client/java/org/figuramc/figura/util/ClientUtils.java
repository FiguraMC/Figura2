package org.figuramc.figura.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class ClientUtils {

    // Get the UUID of the local player
    public static UUID getLocalUUID() {
        return Minecraft.getInstance().getUser().getProfileId();
    }

    // Fetch an entity in the current world by UUID
    // TODO make it more resilient to null level
    public static @Nullable Entity getEntityByUUID(UUID uuid) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) return null;
        return level.getEntities().get(uuid);
    }

}
