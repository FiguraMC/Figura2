package org.figuramc.figura;

import net.fabricmc.api.ClientModInitializer;
import org.figuramc.figura.avatars.Avatar;
import org.figuramc.figura.config.ConfigManager;
import org.figuramc.figura.util.NullEmptyStack;

public class FiguraModClient implements ClientModInitializer {

    // The stack of Avatars rendering. This is peeked by the vanilla ModelPart rendering mixins.
    public static final NullEmptyStack<Avatar<?>> AVATAR_RENDERING_STACK = new NullEmptyStack<>();

    public static boolean LOADED_TEST_AVATAR = false;

    @Override
    public void onInitializeClient() {
        FiguraMod.LOGGER.info("Hello from Figura, client side!");
        ConfigManager.init();
    }



}
