package org.figuramc.figura;

import net.fabricmc.api.ClientModInitializer;
import org.figuramc.figura.avatars.Avatar;
import org.figuramc.figura.avatars.AvatarComponent;
import org.figuramc.figura.avatars.components.*;
import org.figuramc.figura.config.ConfigManager;
import org.figuramc.figura.script_hooks.Event;
import org.figuramc.figura.util.NullEmptyStack;
import org.figuramc.figura.util.ReflectionUtils;
import org.figuramc.figura.util.enumlike.EnumLike;

public class FiguraModClient implements ClientModInitializer {

    // Stacks for Avatars rendering. This is peeked by the vanilla rendering mixins.
    public static final NullEmptyStack<Avatar<?>> AVATAR_RENDERING_STACK = new NullEmptyStack<>();
    public static NullEmptyStack<VanillaRendering> VANILLA_RENDER_COMPONENT_STACK = new NullEmptyStack<>();

    // Temp testing variable for loading the testing avatar
    public static boolean LOADED_TEST_AVATAR = false;

    @Override
    public void onInitializeClient() {
        FiguraMod.LOGGER.info("Hello from Figura, client side!");

        // Ensure classes with integer ID systems are set up correctly.
        // TODO any entrypoints for addons to add more ID items should run before the freeze()!

        // Events are all stored in the Event class
        ReflectionUtils.ensureInitialized(Event.class);
        EnumLike.freeze(Event.class);

        // Avatar component types are currently stored in their classes respectively, though this isn't actually forced and could change
        ReflectionUtils.ensureInitialized(CemSelfDeleter.class, CustomItems.class, EntityRoot.class, EntityUser.class, MolangStateComponent.class, Scripts.class, Textures.class, VanillaRendering.class);
        EnumLike.freeze(AvatarComponent.Type.class);

        // Init configs
        ConfigManager.init();
    }



}
