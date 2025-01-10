package org.figuramc.figura;

import com.mojang.blaze3d.systems.RenderSystem;
import org.figuramc.figura.avatars.Avatar;
import org.figuramc.figura.config.ConfigManager;
import org.figuramc.figura.model.optimized.OptimizedRendering;
import net.fabricmc.api.ClientModInitializer;
import org.figuramc.figura.util.NullEmptyStack;
import org.jetbrains.annotations.Nullable;

import java.util.Stack;

public class FiguraModClient implements ClientModInitializer {

    // The stack of Avatars rendering. This is peeked by the vanilla ModelPart rendering mixins.
    public static final NullEmptyStack<Avatar<?>> AVATAR_RENDERING_STACK = new NullEmptyStack<>();

    public static boolean LOADED_TEST_AVATAR = false;

    @Override
    public void onInitializeClient() {
        FiguraMod.LOGGER.info("Hello from Figura, client side!");
        ConfigManager.init();

        // Initialize OptimizedRendering on render thread later
        // Initializing it right now will segfault the game with an EXCEPTION_ACCESS_VIOLATION so be careful
        RenderSystem.recordRenderCall(OptimizedRendering::init);
    }



}
