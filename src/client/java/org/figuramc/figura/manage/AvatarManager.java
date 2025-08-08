package org.figuramc.figura.manage;

import net.minecraft.world.entity.Entity;
import org.figuramc.figura.avatars.Avatar;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;
import java.util.function.Consumer;

/**
 * The central location that manages all Avatars in existence.
 * It does this through several SubManager instances, which store Avatars according to generically typed keys.
 */
public class AvatarManager {

    // Avatars controlled by entities
    public static final AvatarSubManager<UUID> ENTITY_AVATARS = new AvatarSubManager<>();
    // Avatars that are summoned in the world (todo)
    public static final AvatarSubManager<String> WORLD_AVATARS = new AvatarSubManager<>();
    // Avatars which act as GUIs for the mod (todo)
    public static final AvatarSubManager<GuiType> GUI_AVATARS = new AvatarSubManager<>();

    public static void tick() {
        ENTITY_AVATARS.tick();
        WORLD_AVATARS.tick();
        GUI_AVATARS.tick();
    }

    // Utility: run the consumer on every sub-manager.
    public static void forEachSubManager(Consumer<AvatarSubManager<?>> consumer) {
        consumer.accept(ENTITY_AVATARS);
        consumer.accept(WORLD_AVATARS);
        consumer.accept(GUI_AVATARS);
    }

    // Utility: Run the consumer on every currently loaded Avatar.
    public static void forEachAvatar(Consumer<Avatar<?>> consumer) {
        ENTITY_AVATARS.forEach(consumer);
        WORLD_AVATARS.forEach(consumer);
        GUI_AVATARS.forEach(consumer);
    }

    // Utility: Fetch avatar for entity, or start looking for CEM
    public static @Nullable Avatar<UUID> tryGetEntityAvatar(Entity entity) {
        Avatar<UUID> avatar = AvatarManager.ENTITY_AVATARS.get(entity.getUUID());
        if (avatar == null)  { CemManager.tryGetCem(entity); return null; }
        return avatar;
    }

    // Only 1 type of GUI planned for now in the mod, might change later?
    public enum GuiType {
        MAIN_GUI
    }
}
