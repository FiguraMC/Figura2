package org.figuramc.figura;

import net.fabricmc.api.ClientModInitializer;
import org.figuramc.figura.avatars.Avatar;
import org.figuramc.figura.avatars.components.VanillaRendering;
import org.figuramc.figura.config.ConfigManager;
import org.figuramc.figura.util.NullEmptyStack;

public class FiguraModClient implements ClientModInitializer {

    // Stacks for Avatars rendering. This is peeked by the vanilla rendering mixins.
    public static final NullEmptyStack<Avatar<?>> AVATAR_RENDERING_STACK = new NullEmptyStack<>();
    public static NullEmptyStack<VanillaRendering> VANILLA_RENDER_COMPONENT_STACK = new NullEmptyStack<>();

    // Temp testing variable for loading the testing avatar
    public static boolean LOADED_TEST_AVATAR = false;

    @Override
    public void onInitializeClient() {
        FiguraMod.LOGGER.info("Hello from Figura, client side!");
        ConfigManager.init();
    }



}
